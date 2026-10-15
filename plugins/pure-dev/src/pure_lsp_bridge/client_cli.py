# Copyright 2026 Goldman Sachs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""curl-equivalent CLI for talking to a running pure-lsp-server bridge.

Examples:
    pure-lsp health
    pure-lsp status --wait 300
    pure-lsp check path/to/File.pure
    pure-lsp go
"""
import argparse
import json
import os
import signal
import sys
import time
import urllib.error
import urllib.request
import uuid

from .discovery import resolve_port


class BridgeError(Exception):
    """A 4xx/5xx from the bridge, carrying the bridge's own explanation."""


def _url(args, path):
    return "http://%s:%d%s" % (args.host, args.port, path)


def _send(args, path, data=None, timeout=15):
    headers = {"Content-Type": "application/json"} if data is not None else {}
    req = urllib.request.Request(_url(args, path), data=data, headers=headers,
                                 method="POST" if data is not None else "GET")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        # The bridge answers every rejection with a JSON {"error": "..."} body saying exactly
        # what was wrong ("missing 'uri'", "every entry in 'files' needs a 'uri'"). urlopen
        # raises before the caller can read it, and HTTPError subclasses URLError, so main()'s
        # connection handler used to swallow it and print a bare "HTTP Error 400: Bad Request",
        # throwing away the only useful part. Surface the body instead.
        try:
            body = json.loads(e.read().decode("utf-8"))
        except Exception:
            body = None
        if isinstance(body, dict) and body.get("error"):
            raise BridgeError("bridge rejected %s (HTTP %s): %s" % (path, e.code, body["error"]))
        raise BridgeError("bridge returned HTTP %s for %s" % (e.code, path))


def _get(args, path, timeout=15):
    return _send(args, path, timeout=timeout)


def _post(args, path, payload, timeout=60):
    return _send(args, path, data=json.dumps(payload).encode("utf-8"), timeout=timeout)


def _exec_timeout(files):
    """Ceiling for a call that can drive plan-gen + DB execution or a whole test sweep.

    Kept strictly ABOVE the bridge's own ceiling for the same call (bridge.py uses
    max(600, 3*len(files))). They used to be equal, so on a genuine timeout the client gave up
    at the same instant the bridge did and the caller saw a socket timeout instead of the
    bridge's actual error.
    """
    return max(600, 3 * len(files or [])) + 30


def _read_files(paths):
    """Read .pure files into the [{uri, content}] shape every batch endpoint takes."""
    files = []
    for path in paths:
        _reject_non_pure_file(path)
        abspath = os.path.abspath(path)
        with open(abspath, "r") as f:
            files.append({"uri": "file://" + abspath, "content": f.read()})
    return files


def _reject_non_pure_file(path):
    """.legend files are runtime fixtures loaded via readFile(...) inside a test, not Pure
    compilation units - pushing one through check/check-many/go corrupts the LSP runtime state
    (throws "Invalid source id" and knocks it into recovering/failed)."""
    if not path.endswith(".pure"):
        print(
            "error: %r is not a .pure file - only .pure source files can be checked/compiled here. "
            "(.legend files are runtime fixtures loaded via readFile(...) inside a test; verify them "
            "indirectly by running the test/go() function that reads them.)" % path,
            file=sys.stderr,
        )
        sys.exit(2)


def cmd_health(args):
    print(json.dumps(_get(args, "/health")))


def cmd_status(args):
    deadline = time.time() + args.wait
    while True:
        result = _get(args, "/status")
        if args.wait <= 0 or result.get("state") in ("ready", "degraded", "failed"):
            print(json.dumps(result))
            return 0 if result.get("state") == "ready" else 1
        if time.time() >= deadline:
            print(json.dumps(result))
            return 1
        time.sleep(1.5)


def cmd_check(args):
    if args.file == "-":
        content = sys.stdin.read()
        uri = args.uri or "file:///stdin.pure"
    else:
        _reject_non_pure_file(args.file)
        path = os.path.abspath(args.file)
        with open(path, "r") as f:
            content = f.read()
        uri = args.uri or ("file://" + path)
    result = _post(args, "/check", {"uri": uri, "content": content})
    print(json.dumps(result, indent=2))
    if result.get("timedOut"):
        # Exit non-zero: an empty diagnostics list here means "nothing came back", not "clean", and
        # a caller treating it as a pass would go on to run against the pre-edit graph.
        print("error: %s" % result.get("error", "timed out waiting for diagnostics"), file=sys.stderr)
        print("       check `pure-lsp status` for lockContended / a long-running test run",
              file=sys.stderr)
        return 1
    diagnostics = result.get("diagnostics") or []
    errors = [d for d in diagnostics if d.get("severity") == 1]
    return 1 if errors else 0


def cmd_check_many(args):
    files = _read_files(args.files)
    response = _post(args, "/check-batch", {"files": files}, timeout=max(30, 3 * len(files)) + 30)
    print(json.dumps(response, indent=2))

    if not response.get("success"):
        # Single-compile batch: on failure the whole thing rolled back, so exactly one file is
        # actually implicated (errorUri) - the rest weren't confirmed either way, unlike the old
        # per-file diagnostics array this used to return.
        error_uri = response.get("errorUri")
        if error_uri:
            print("compile failed, attributed to: %s" % error_uri, file=sys.stderr)
        return 1
    return 0


def cmd_go(args):
    payload = {}
    if args.files:
        payload["files"] = _read_files(args.files)
    result = _post(args, "/go", payload, timeout=_exec_timeout(args.files))
    print(json.dumps(result, indent=2))
    return 0 if result.get("success") else 1


MAX_TESTS_PER_RUN = 30

# A long run holds the graph read lock for its whole duration, so every compile queues behind it and
# the session stops seeing edits. Past this many tests, stagger the run into smaller scopes or use
# the compiled (Maven) suite instead.
_OVER_LIMIT_ADVICE = (
    "run it in smaller scopes (a --source file at a time, or a narrower --package with "
    "--no-recursive), or use the compiled Maven suite for a run this size"
)


def _reject_oversized_list(functions):
    if len(functions) <= MAX_TESTS_PER_RUN:
        return None
    return ("error: %d functions requested, limit is %d per run\n       %s"
            % (len(functions), MAX_TESTS_PER_RUN, _OVER_LIMIT_ADVICE))


def _warn_if_scope_oversized(result):
    """A scope's size is only known once the daemon has walked it, so this cannot prevent an
    oversized run - it makes one visible instead of letting it pass as normal."""
    count = len([t for t in (result.get("tests") or []) if not t.get("hook")])
    if count > MAX_TESTS_PER_RUN:
        print("warning: this scope ran %d tests, over the %d limit - %s"
              % (count, MAX_TESTS_PER_RUN, _OVER_LIMIT_ADVICE), file=sys.stderr)


def _scope_payload(args, parallel):
    """Build the /execute-tests payload from the --package/--source scope flags, or None if the
    caller named functions instead. Concurrency is decided by which subcommand was invoked, not by a
    flag: `execute` is serial, `execute-parallel` is not."""
    package = getattr(args, "package", None)
    source = getattr(args, "source", None)
    if not package and not source:
        return None
    payload = {"parallel": parallel, "recursive": not getattr(args, "no_recursive", False)}
    if package:
        payload["packagePath"] = package
    else:
        # The bridge forwards this to the LSP, which accepts a file:// uri, a pure:// uri or a raw
        # sourceId. Send an absolute path as a file:// uri so it resolves the same way an editor's
        # would; leave anything else (already a uri, or a bare sourceId) untouched.
        payload["uri"] = "file://" + os.path.abspath(source) if os.path.exists(source) else source
    if getattr(args, "pct_adapter", None):
        payload["pctAdapterPath"] = args.pct_adapter
    if getattr(args, "vanilla_only", False):
        payload["includePct"] = False
    if getattr(args, "pct_only", False):
        payload["includeVanilla"] = False
    if args.files:
        payload["files"] = _read_files(args.files)
    # The client cannot size a scope before the daemon walks it, so ask the daemon to refuse an
    # oversized one up front. Ignored by servers that predate the field.
    payload["maxTests"] = MAX_TESTS_PER_RUN
    return payload


def _depth(package_path):
    return 0 if not package_path else package_path.count("::")


RETURN_VALUE_PREVIEW_CHARS = 160


def _render_return_value(entry):
    """One-line preview of an entry's return value for the tree view.

    `complex` and `empty` carry no value by design, so they render as the type alone rather than
    a bare 'None' that reads like a failure. The full value is always in --json.
    """
    kind = entry.get("returnKind")
    if not kind or kind == "empty":
        return None
    if kind == "complex":
        return "%s (use --json / print() for complex values)" % (entry.get("returnType") or "?")

    value = entry.get("returnValue")
    # A <<test.Test>> conventionally ends in `true`, so a passing test would otherwise print
    # "= True" on every line. A `false` return still shows: status tracks whether the function
    # threw, not what it returned, so the two genuinely differ.
    if value is True and (entry.get("status") or "").lower() == "passed":
        return None
    if isinstance(value, list):
        text = "[%s]" % ", ".join(repr(v) for v in value)
    else:
        text = repr(value)

    text = " ".join(text.split())
    if len(text) > RETURN_VALUE_PREVIEW_CHARS:
        text = text[:RETURN_VALUE_PREVIEW_CHARS] + "..."
    # Appended after the length cap, not before: a truncated collection is exactly the case whose
    # preview overflows, so a suffix added first is the part that gets cut off.
    if entry.get("returnTruncated"):
        size = entry.get("returnSize")
        text = "%s  (%s total, truncated)" % (text, size if size is not None else "?")
    return text


def _render_tests(result):
    """Print the run as an indented tree. Results arrive in execution order with the package node
    each belongs to, so a suite header on every change of suitePath reproduces the nesting - which
    is the whole point of the per-subpackage setup/teardown this runs."""
    tests = result.get("tests") or []
    if not tests:
        print("no tests found in %s" % (result.get("scope") or "the requested scope"))
        return

    base = min(_depth(t.get("suitePath")) for t in tests)
    current = None
    for entry in tests:
        suite = entry.get("suitePath")
        indent = "  " * max(0, _depth(suite) - base)
        if suite != current:
            print("%s%s" % (indent, suite))
            current = suite
        status = (entry.get("status") or "?").upper()[:4]
        label = entry.get("name") or entry.get("functionPath")
        if entry.get("hook"):
            label = "%s (%s)" % (label, entry.get("hookKind"))
        print("%s  %-4s %s  %dms" % (indent, status, label, entry.get("durationMs") or 0))
        rendered = _render_return_value(entry)
        if rendered:
            print("%s       = %s" % (indent, rendered))
        message = entry.get("message")
        if message and status != "PASS":
            for line in str(message).splitlines():
                print("%s       %s" % (indent, line))

    print()
    print("%d passed, %d failed, %d skipped in %.1fs"
          % (result.get("passed") or 0, result.get("failed") or 0, result.get("skipped") or 0,
             (result.get("durationMs") or 0) / 1000.0))


def _cancel_run(args, run_id):
    """Best-effort cancel, used from the Ctrl-C handler. Never raises: we are already on the way out,
    and a failure to cancel must not replace the user's interrupt with a stack trace."""
    try:
        _post(args, "/cancel-tests", {"runId": run_id}, timeout=30)
        print("cancelled test run %s" % run_id, file=sys.stderr)
    except Exception as e:
        print("warning: could not cancel test run %s: %s" % (run_id, e), file=sys.stderr)
        print("the daemon may still be running it - `pure-lsp cancel-tests --all` to stop it",
              file=sys.stderr)


def cmd_execute_tests(args, parallel, payload=None):
    payload = _scope_payload(args, parallel) if payload is None else payload
    # Generate the runId client-side so Ctrl-C has something to cancel with. Without it, interrupting
    # the CLI would just abandon the run: the daemon keeps executing to completion, holding the graph
    # read lock and blocking every compile, with nobody waiting on the result.
    run_id = "cli-%s" % uuid.uuid4()
    payload["runId"] = run_id

    def on_interrupt(_signum, _frame):
        print("\ninterrupted - cancelling the run on the daemon...", file=sys.stderr)
        _cancel_run(args, run_id)
        # Re-raise as the usual KeyboardInterrupt so the normal exit path runs.
        raise KeyboardInterrupt()

    previous_handler = signal.signal(signal.SIGINT, on_interrupt)
    try:
        return _execute_tests_request(args, payload, run_id)
    except KeyboardInterrupt:
        return 130
    finally:
        signal.signal(signal.SIGINT, previous_handler)


def _execute_tests_request(args, payload, run_id):
    # No per-call timeout here: a scope-sized run is open-ended, and the bridge already picks a
    # generous one (bridge._tests_timeout). Keep the HTTP read above it so the client does not give
    # up on a run the daemon is still doing.
    result = _post(args, "/execute-tests", payload, timeout=3900)
    if getattr(args, "json", False):
        print(json.dumps(result, indent=2))
    elif result.get("cancelled"):
        # A cancelled run carries BOTH an error line and the entries that did complete - show the
        # partial tree, since "what had passed before I stopped it" is the useful part.
        _render_tests(result)
        print("run cancelled: %s" % result.get("error", "stopped early"), file=sys.stderr)
    elif result.get("error"):
        # A scope-level rejection (unknown package, PCT-only scope with no adapter, uncompilable
        # files) produces no entries at all. Printing the empty-tree line underneath it would just
        # bury the actual reason.
        print("error: %s" % result["error"], file=sys.stderr)
    else:
        _render_tests(result)
        _warn_if_scope_oversized(result)
    return 0 if result.get("success") else 1


def cmd_cancel_tests(args):
    payload = {"all": True} if args.all else {"runId": args.run_id}
    result = _post(args, "/cancel-tests", payload, timeout=30)
    print(json.dumps(result, indent=2))
    # Nothing to cancel is not a failure - the run had most likely already finished, which is the
    # common case when racing a run you have lost the client for.
    return 0


def cmd_execute(args):
    scope = _scope_payload(args, parallel=False)
    if scope is not None:
        if args.function:
            print("error: give either a function or a --package/--source scope, not both",
                  file=sys.stderr)
            return 2
        return cmd_execute_tests(args, parallel=False)
    if not args.function:
        print("error: a function is required (or use --package/--source to run a whole scope)",
              file=sys.stderr)
        return 2

    payload = {"function": args.function}
    if args.files:
        payload["files"] = _read_files(args.files)
    if args.arg:
        payload["arguments"] = args.arg
    if args.pct_adapter:
        payload["pctAdapterPath"] = args.pct_adapter
    if args.before:
        payload["beforeFunctionPath"] = args.before
    if args.after:
        payload["afterFunctionPath"] = args.after
    # (matches bridge.py's Bridge.execute_function / the LSP's ExecuteFunctionParams field names)
    result = _post(args, "/execute", payload, timeout=_exec_timeout(args.files))
    print(json.dumps(result, indent=2))
    return 0 if result.get("success") else 1


def cmd_execute_parallel(args):
    """Run several zero-arg functions concurrently on the daemon in one call.

    Both forms - an explicit function list and a --package/--source scope - go to
    legend/executeTests. The server owns the fan-out either way, which is what bounds it: the
    older path fired one legend/execute per function and relied on the shared request pool to
    throttle, so a big batch could occupy every request thread and starve status/check/cancel.
    Routing the explicit list through the same endpoint also buys per-entry results (status,
    duration, message) instead of console text to re-parse, streamed progress, and a runId that
    Ctrl-C can actually cancel.
    """
    scope = _scope_payload(args, parallel=True)
    if scope is not None:
        if args.functions:
            print("error: give either functions or a --package/--source scope, not both",
                  file=sys.stderr)
            return 2
        return cmd_execute_tests(args, parallel=True)
    if not args.functions:
        print("error: at least one function is required (or use --package/--source to run a whole scope)",
              file=sys.stderr)
        return 2
    oversized = _reject_oversized_list(args.functions)
    if oversized:
        print(oversized, file=sys.stderr)
        return 2

    payload = {"functions": args.functions, "parallel": True}
    if args.files:
        payload["files"] = _read_files(args.files)
    if getattr(args, "pct_adapter", None):
        payload["pctAdapterPath"] = args.pct_adapter
    return cmd_execute_tests(args, parallel=True, payload=payload)


def _add_scope_args(parser):
    """Scope flags shared by `execute` and `execute-parallel`.

    Both subcommands keep their existing meaning when given function paths; a scope flag switches
    them to /execute-tests instead, where the daemon discovers the tests itself. Which subcommand
    was used decides serial vs concurrent, so there is no --parallel flag here.

    Note --source (the file whose tests to run) is distinct from execute-parallel's --file (a file to
    compile before running).
    """
    scope = parser.add_mutually_exclusive_group()
    scope.add_argument("--package", default=None, metavar="PURE_PACKAGE",
                       help="run every test in this Pure package ('meta::pure::functions::x::tests'), "
                            "with each subpackage keeping its own <<test.BeforePackage>>/"
                            "<<test.AfterPackage>> bracketing")
    scope.add_argument("--source", default=None, metavar="PURE_FILE",
                       help="run every test defined in this .pure file (path, file:// uri, or "
                            "sourceId); inherited package hooks still apply")
    parser.add_argument("--no-recursive", action="store_true",
                        help="with --package, run only that package's own tests, not its subpackages")
    parser.add_argument("--vanilla-only", action="store_true",
                        help="with --package/--source, run only plain <<test.Test>> functions")
    parser.add_argument("--pct-only", action="store_true",
                        help="with --package/--source, run only <<PCT.test>> functions")
    parser.add_argument("--json", action="store_true",
                        help="with --package/--source, print the raw result payload instead of the "
                             "rendered tree")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default=os.environ.get("PURE_LSP_HOST", "127.0.0.1"))
    # Resolved lazily below (only when not given) so discovery never runs for an explicit --port.
    ap.add_argument("--port", type=int, default=None,
                    help="bridge HTTP port (default: $PURE_LSP_PORT, else a live bridge "
                         "discovered from its /tmp/pure_lsp_server_<port>.json sidecar, else 8991)")
    sub = ap.add_subparsers(dest="command", required=True)

    sub.add_parser("health", help="check whether the bridge and its java child are alive")

    p_status = sub.add_parser("status", help="fetch (or wait for) Pure runtime status")
    p_status.add_argument("--wait", type=float, default=0, help="seconds to poll until state is ready/degraded/failed")

    p_check = sub.add_parser("check", help="open/edit a .pure file in the session and print diagnostics")
    p_check.add_argument("file", help="path to a .pure file, or '-' for stdin")
    p_check.add_argument("--uri", default=None, help="override the LSP document URI (default: file://<abspath>)")

    p_check_many = sub.add_parser("check-many", help="open/edit several .pure files as one batch, "
                                                       "then print diagnostics for all of them")
    p_check_many.add_argument("files", nargs="+", help="paths to .pure files")

    p_go = sub.add_parser("go", help="compile everything loaded and execute function go():Any[*]")
    p_go.add_argument("files", nargs="*", help="optional .pure files to compile first (as one "
                                                "cross-file-safe batch, like check-many), then "
                                                "execute go() against the resulting session")

    p_execute = sub.add_parser("execute", help="compile everything loaded and execute an arbitrary "
                                                "zero-arg function by Pure path (not just go()), or "
                                                "every test in a --package/--source scope, serially")
    p_execute.add_argument("function", nargs="?", default=None,
                            help="signature ('a::b::t():Boolean[1]'), mangled id "
                                 "('a::b::t__Boolean_1_'), or bare path ('a::b::t'). Omit when using "
                                 "--package/--source.")
    p_execute.add_argument("files", nargs="*", help="optional .pure files to compile first (as one "
                                                     "cross-file-safe batch, like check-many), then "
                                                     "execute the function against the resulting session")
    p_execute.add_argument("--arg", action="append", default=[], metavar="STRING",
                            help="a String[1] literal to pass as the next parameter; repeatable, in "
                                 "order. String is the only supported parameter type. A bare path "
                                 "only resolves zero-arg shapes, so name a signature or mangled id "
                                 "when passing arguments.")
    p_execute.add_argument("--pct-adapter", default=None,
                            help="Pure path of a PCT adapter Function, only for a <<PCT.test>> "
                                 "function (which takes the adapter as its one parameter)")
    p_execute.add_argument("--before", default=None,
                            help="Pure path of a zero-arg function to run immediately before, in the same atomic call")
    p_execute.add_argument("--after", default=None,
                            help="Pure path of a zero-arg function to run immediately after, in the same atomic call")
    _add_scope_args(p_execute)

    p_par = sub.add_parser("execute-parallel",
                            help="compile files once, then run several zero-arg functions - or every "
                                 "test in a --package/--source scope - CONCURRENTLY on the daemon, "
                                 "which owns and bounds the fan-out (see executionConcurrency in "
                                 "`pure-lsp status`); reports per-entry status/duration")
    p_par.add_argument("functions", nargs="*",
                        help="zero or more functions (max %d), each a signature, mangled id, or "
                             "bare path. Omit when using --package/--source."
                             % MAX_TESTS_PER_RUN)
    p_par.add_argument("--file", action="append", dest="files", default=[], metavar="PURE_FILE",
                        help="a .pure file to compile (once, up front) before running the "
                             "functions; repeatable. Separate flag because the positional "
                             "arguments are the function list.")
    p_par.add_argument("--pct-adapter", default=None,
                        help="Pure path of a PCT adapter Function, applied to every <<PCT.test>> in "
                             "the run - whether scoped or an explicit function list")
    _add_scope_args(p_par)

    p_cancel = sub.add_parser("cancel-tests",
                               help="stop an in-flight --package/--source test run on the daemon")
    cancel_target = p_cancel.add_mutually_exclusive_group(required=True)
    cancel_target.add_argument("--run-id", default=None,
                                help="the runId reported by the run (see its JSON output)")
    cancel_target.add_argument("--all", action="store_true",
                                help="cancel every run in flight - the one to reach for when a run "
                                     "was abandoned (client timed out or was killed) and its runId "
                                     "is no longer to hand")

    args = ap.parse_args(argv)
    if args.port is None:
        args.port = resolve_port()
    handlers = {
        "health": cmd_health,
        "status": cmd_status,
        "check": cmd_check,
        "check-many": cmd_check_many,
        "go": cmd_go,
        "execute": cmd_execute,
        "execute-parallel": cmd_execute_parallel,
        "cancel-tests": cmd_cancel_tests,
    }

    try:
        return handlers[args.command](args) or 0
    except BridgeError as e:
        print("error: %s" % e, file=sys.stderr)
        return 2
    except urllib.error.URLError as e:
        print("error contacting bridge at %s:%d: %s" % (args.host, args.port, e), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
