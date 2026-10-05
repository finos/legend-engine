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
"""Owns one warm Legend Pure LSP server subprocess and exposes it over plain
HTTP, so a stateless client (curl, or anything issuing one-shot HTTP calls)
can drive incremental compilation and `go()` execution without holding a
live stdin pipe open itself.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from .protocol import LspClient

SERVER_MAIN_CLASS = "org.finos.legend.pure.lsp.LegendPureLspServer"

READY_STATES = ("ready", "degraded", "failed")

# Linux caps any single execve() argv element at MAX_ARG_STRLEN (32 pages, ~131072 bytes) -
# independent of the much larger total ARG_MAX. An engine-scale classpath (1000+ jars) can exceed
# that as one "-cp <classpath>" argument, failing with OSError: [Errno 7] Argument list too long
# even though the total command is nowhere near ARG_MAX. Stay safely under the limit and fall back
# to a java @argfile (only the short "@path" token reaches execve) for anything close to it.
_MAX_SINGLE_ARG_LEN = 100_000


# A whole-package test run is open-ended in a way a single execute is not: the caller names a scope,
# not a list, so we cannot size the wait from the request. These are deliberately generous - the cost
# of being too low is a spurious TimeoutError on a run that was progressing fine, which also leaves
# the daemon doing work nobody is waiting for. Callers who know better pass an explicit timeout.
_TESTS_TIMEOUT_SERIAL = 3600
_TESTS_TIMEOUT_PARALLEL = 1800


def _tests_timeout(parallel):
    return _TESTS_TIMEOUT_PARALLEL if parallel else _TESTS_TIMEOUT_SERIAL


def _classpath_argv(classpath):
    if len(classpath) <= _MAX_SINGLE_ARG_LEN:
        return ["-cp", classpath]
    fd, argfile_path = tempfile.mkstemp(prefix="pure-lsp-classpath-", suffix=".argfile")
    with os.fdopen(fd, "w") as f:
        f.write("-cp\n%s\n" % classpath)
    return ["@" + argfile_path]


class OpenDoc:
    def __init__(self):
        self.version = 0


class Bridge:
    def __init__(self, java, classpath, stderr_log_path, jvm_args=None, socket_port=None):
        """Two transports:

        * stdio (socket_port is None): spawn the JVM as a piped child. The JVM dies when this bridge
          exits (pipe EOF) - not durable.
        * socket (socket_port set): connect to a JVM daemon listening on 127.0.0.1:socket_port. If
          none is running, spawn one DETACHED (its own session, stdio to a log, no pipe held by us)
          and connect once it's up. Because nothing owns the JVM via a pipe, it survives this bridge
          being killed, and a later bridge reconnects to the same warm session.
        """
        if socket_port is not None:
            self.client = self._init_socket(java, classpath, stderr_log_path, jvm_args, socket_port)
        else:
            command = [java] + list(jvm_args or []) + _classpath_argv(classpath) + [SERVER_MAIN_CLASS]
            print("[bridge] launching (stdio) with %d classpath entries" % len(classpath.split(":")), file=sys.stderr)
            if jvm_args:
                print("[bridge] extra jvm args:", " ".join(jvm_args), file=sys.stderr)
            print("[bridge] command:", " ".join(command), file=sys.stderr)
            self.client = LspClient.spawn(command, stderr_log_path)

        self.open_docs = {}  # uri -> OpenDoc
        self._docs_lock = threading.Lock()

        self._diagnostics = {}  # uri -> (seq, [diagnostics])
        self._diag_seq = 0
        self._diag_cv = threading.Condition()
        self.client.on_notification("textDocument/publishDiagnostics", self._on_diagnostics)

    def _init_socket(self, java, classpath, stderr_log_path, jvm_args, socket_port):
        # 1) Reuse a daemon already listening (true persistence across bridge restarts).
        try:
            client = LspClient.connect("127.0.0.1", socket_port, timeout=3)
            print("[bridge] connected to EXISTING LSP daemon on 127.0.0.1:%d (warm session reused)"
                  % socket_port, file=sys.stderr)
            return client
        except Exception:
            pass

        # 2) None running: spawn one DETACHED + REPARENTED TO INIT, then wait for it to listen.
        java_command = ([java] + list(jvm_args or [])
                        + ["-Dlegend.lsp.socketPort=%d" % socket_port]
                        + _classpath_argv(classpath) + [SERVER_MAIN_CLASS])
        print("[bridge] no daemon on :%d; spawning detached LSP daemon" % socket_port, file=sys.stderr)
        if jvm_args:
            print("[bridge] extra jvm args:", " ".join(jvm_args), file=sys.stderr)
        logf = open(stderr_log_path, "ab")
        # DOUBLE-FORK the JVM so it is reparented to init (PID 1) and leaves this bridge's process
        # TREE entirely. `start_new_session` alone only escapes the process GROUP; it does NOT survive
        # a harness that kills the whole task subtree (which is what reaped earlier daemons - the JVM
        # was a descendant of the launcher). `setsid --fork` performs the double-fork: setsid creates a
        # new session, --fork forks so setsid (the JVM's parent) exits immediately, orphaning the JVM
        # to init - exactly how the backend Server (PPID 1) survives. Prefer setsid; fall back to a
        # Python os.fork double-fork if setsid is unavailable.
        setsid_path = shutil.which("setsid")
        if setsid_path is not None:
            command = [setsid_path, "--fork"] + java_command
            subprocess.Popen(
                command,
                stdin=subprocess.DEVNULL,
                stdout=logf,
                stderr=logf,
                start_new_session=True,
                close_fds=True,
            )
        else:
            self._double_fork_spawn(java_command, logf)
        # Both paths above have already duplicated logf's fd into the spawned process(es) by this
        # point (Popen dups before returning; fork() gives descendants their own independent copy of
        # the fd table at fork time) - our copy in this process is redundant and would otherwise stay
        # open (leaking one fd) for the life of this long-running bridge process.
        logf.close()
        # Wait for the daemon to bind and accept.
        deadline = time.time() + 300
        last_err = None
        while time.time() < deadline:
            try:
                client = LspClient.connect("127.0.0.1", socket_port, timeout=3)
                print("[bridge] connected to freshly-spawned LSP daemon on 127.0.0.1:%d" % socket_port,
                      file=sys.stderr)
                return client
            except Exception as e:
                last_err = e
                time.sleep(1.0)
        raise RuntimeError("LSP daemon did not start listening on :%d within timeout: %s"
                           % (socket_port, last_err))

    @staticmethod
    def _double_fork_spawn(command, logf):
        """Portable double-fork daemonize (fallback when `setsid` binary is absent). The classic
        Unix idiom: fork -> setsid -> fork again -> the grandchild execs the JVM and is orphaned to
        init. The intermediate child exits immediately; this method returns in the original process
        without leaving a tracked child of it, so the JVM is NOT in the bridge's process subtree."""
        pid = os.fork()
        if pid > 0:
            # original process: reap the intermediate child so it doesn't zombie, then return.
            os.waitpid(pid, 0)
            return
        # first child
        try:
            os.setsid()
            pid2 = os.fork()
            if pid2 > 0:
                os._exit(0)  # intermediate child exits -> grandchild reparents to init
            # grandchild: redirect stdio and exec the JVM
            devnull = os.open(os.devnull, os.O_RDONLY)
            os.dup2(devnull, 0)
            os.dup2(logf.fileno(), 1)
            os.dup2(logf.fileno(), 2)
            os.execvp(command[0], command)
        except Exception:
            os._exit(127)

    def _on_diagnostics(self, params):
        uri = params.get("uri")
        diags = params.get("diagnostics", [])
        with self._diag_cv:
            self._diag_seq += 1
            self._diagnostics[uri] = (self._diag_seq, diags)
            self._diag_cv.notify_all()

    def _current_diag_seq(self):
        with self._diag_cv:
            return self._diag_seq

    def _wait_for_diagnostics(self, uri, after_seq, timeout):
        deadline = time.time() + timeout
        with self._diag_cv:
            while True:
                entry = self._diagnostics.get(uri)
                if entry is not None and entry[0] > after_seq:
                    return entry[1]
                remaining = deadline - time.time()
                if remaining <= 0:
                    return None
                self._diag_cv.wait(remaining)

    # ---------- lifecycle ----------

    def initialize(self, repo_roots):
        # repo_roots: list of absolute workspace roots. Each is sent as a workspaceFolder; the LSP
        # scans every root for */src/main/resources/*.definition.json repos, and a repo found in ANY
        # workspace root is loaded from SOURCE, overriding the same-named classpath-jar repo. (A single
        # string is tolerated for backward compat.)
        if isinstance(repo_roots, str):
            repo_roots = [repo_roots]

        # Reuse-safe: if we connected to a daemon that is already initialized (its runtime is in a
        # real state, not just default), skip the handshake - re-initializing an already-running LSP
        # is unnecessary and can disturb the warm session. We detect this by probing legend/status:
        # a fresh (uninitialized) server has no meaningful state yet, an initialized one reports
        # created/initializing/ready/etc.
        try:
            existing = self.status()
            state = existing.get("state") if isinstance(existing, dict) else None
            if state in ("ready", "degraded", "initializing", "reindexing", "recovering"):
                print("[bridge] daemon already initialized (state=%s); skipping initialize handshake"
                      % state, file=sys.stderr)
                return
        except Exception:
            pass  # status not answerable yet -> proceed with a normal initialize

        folders = [{"uri": "file://" + r, "name": r.rsplit("/", 1)[-1]} for r in repo_roots]
        print("[bridge] initialize with %d workspace root(s): %s"
              % (len(folders), ", ".join(repo_roots)), file=sys.stderr)
        result = self.client.request("initialize", {
            "processId": None,
            # rootUri is deprecated in LSP but kept for servers that read it; use the first root.
            "rootUri": folders[0]["uri"],
            "workspaceFolders": folders,
            "capabilities": {},
            "initializationOptions": {},
        }, timeout=30)
        if "error" in result:
            raise RuntimeError("initialize failed: %s" % result["error"])
        self.client.notify("initialized", {})

    def wait_ready(self, timeout=300):
        deadline = time.time() + timeout
        last = None
        while time.time() < deadline:
            last = self.status()
            if last and last.get("state") in READY_STATES:
                return last
            time.sleep(1.5)
        return last

    # ---------- operations ----------

    def status(self):
        resp = self.client.request("legend/status", None, timeout=10)
        return resp.get("result") or resp.get("error")

    def _push(self, uri, content):
        """Caller must hold self._docs_lock."""
        doc = self.open_docs.get(uri)
        if doc is None:
            doc = OpenDoc()
            doc.version = 1
            self.open_docs[uri] = doc
            self.client.notify("textDocument/didOpen", {
                "textDocument": {"uri": uri, "languageId": "pure", "version": 1, "text": content}
            })
        else:
            doc.version += 1
            self.client.notify("textDocument/didChange", {
                "textDocument": {"uri": uri, "version": doc.version},
                "contentChanges": [{"text": content}],
            })

    def check(self, uri, content):
        with self._docs_lock:
            before_seq = self._current_diag_seq()
            self._push(uri, content)
        diags = self._wait_for_diagnostics(uri, before_seq, timeout=15)
        if diags is None:
            # No diagnostics arrived in time. Reporting that as [] is indistinguishable from a clean
            # compile, which is how a daemon whose compiles are all blocked reports every file as
            # green - the edit is not in the session, so everything run afterwards sees stale code.
            return {"uri": uri, "diagnostics": [], "timedOut": True,
                    "error": "timed out after 15s waiting for diagnostics - the edit may not have "
                             "been compiled into the session"}
        return {"uri": uri, "diagnostics": diags}

    def check_many(self, files):
        """files: [{"uri":..., "content":...}, ...].

        Delegates to the server's own legend/checkBatch (LegendPureLspServer#checkBatch ->
        SourceMutationService#applyBulkChangesAndCompile), which applies every file's content to
        the runtime FIRST and compiles exactly ONCE - a single atomic operation, not the
        push-twice client-side workaround this used to do to route around the per-file
        didOpen/didChange compile-immediately-and-independently behavior. Trade-off: on success,
        every file in the batch (and anything else transitively affected) is guaranteed clean; on
        failure, the whole batch is rolled back and you get exactly ONE error, attributed to its
        real file via the server's own SourceInformation-based resolution where possible - not a
        per-file diagnostics breakdown across the whole batch like the old two-pass client-side
        version gave you. See the pure-lsp-check skill for why that trade-off is worth it.

        Returns the server's CheckBatchResult dict as-is:
          success:  {"success": true, "modifiedFiles": [uri, ...]}
          failure:  {"success": false, "error": str, "errorUri": str|null,
                     "errorDiagnostics": [Diagnostic]|null}
        """
        resp = self.client.request("legend/checkBatch", {"files": files}, timeout=max(30, 3 * len(files)))
        return resp.get("result") or resp.get("error")

    def execute_go(self, files=None):
        """If files are given, the server compiles them as one atomic batch and only executes
        go() if that succeeds - all in a single legend/executeGo call (LegendPureLspServer now
        does the compile-then-execute itself; the bridge no longer orchestrates two separate
        requests for this).
        """
        params = {"files": files} if files else None
        # go() can drive real plan-gen + DB execution (and a whole test sweep), so give it a long
        # ceiling: 10 min base, scaling up for large multi-file batches. 120s was too short.
        timeout = 600 if not files else max(600, 3 * len(files))
        resp = self.client.request("legend/executeGo", params, timeout=timeout)
        return resp.get("result") or resp.get("error")

    def execute_function(self, function, files=None, pct_adapter_path=None, before_function_path=None,
                          after_function_path=None, arguments=None, timeout=600):
        """Run an arbitrary zero-arg function by Pure path via legend/execute (not just go()).
        `function` may be a signature ('a::b::t():Boolean[1]'), a mangled id ('a::b::t__Boolean_1_'),
        or a bare path ('a::b::t'). Optional files compiled as one batch first.

        `pct_adapter_path` is only meaningful for a <<PCT.test>> function (which takes exactly one
        parameter: the adapter Function itself) - the Pure path of one of the adapters returned by
        legend/getPCTAdapters. `before_function_path`/`after_function_path` are optional zero-arg
        functions run immediately before/after `function`, within the same atomic call.

        `arguments` invokes a function taking String[1] parameters, one literal per parameter in
        order. String is the only type the daemon marshals; it is mutually exclusive with
        `pct_adapter_path`. A bare path only resolves against zero-arg shapes, so pass a signature
        or mangled id alongside arguments.
        """
        params = {"function": function}
        if files:
            params["files"] = files
        if arguments:
            params["arguments"] = list(arguments)
        if pct_adapter_path:
            params["pctAdapterPath"] = pct_adapter_path
        if before_function_path:
            params["beforeFunctionPath"] = before_function_path
        if after_function_path:
            params["afterFunctionPath"] = after_function_path
        resp = self.client.request("legend/execute", params, timeout=timeout)
        return resp.get("result") or resp.get("error")

    def execute_tests(self, package_path=None, uri=None, functions=None, invocations=None, recursive=True,
                       parallel=False, include_vanilla=True, include_pct=True, pct_adapter_path=None,
                       files=None, run_id=None, timeout=None):
        """Discover and run EVERY test in a package or a file via legend/executeTests - the batch
        counterpart to execute_function(), which runs one function at a time.

        Exactly one of `package_path` / `uri` / `functions` selects the scope. For the first two,
        discovery and execution both happen server-side, walking the same TestCollection tree the
        Maven/JUnit side uses, so per-subpackage and inherited
        <<test.BeforePackage>>/<<test.AfterPackage>> bracketing is preserved: hooks run once per
        package node, not once per test.

        `functions` instead names an explicit list of zero-arg function paths, run as one flat
        group with discovery skipped - so they need not carry <<test.Test>> nor share a package,
        and correspondingly get no hook bracketing. This is what lets an arbitrary batch still come
        back as per-entry TestResults with streamed events and runId cancellation, rather than as N
        separate execute calls whose only output is console text.

        `invocations` is the same flat group where each entry is a dict
        {"path", "arguments"?, "label"?}, so one parameterised function can be fanned out over many
        String[1] argument sets - which a list of function paths cannot express, every entry naming
        the same function.

        `parallel` fans out the tests WITHIN each package node; the tree walk itself stays serial on
        the server (a node's hooks bracket its whole subtree). `pct_adapter_path` is the single
        adapter applied to every <<PCT.test>> in the run.

        A run can be much longer than a single execute, so the default timeout scales with it rather
        than reusing execute's fixed 600s - see _tests_timeout().
        """
        # Always carry a runId, generating one if the caller didn't. It is the only handle for
        # cancelling the run, and the timeout path below needs it: a run that we stop waiting for
        # keeps executing server-side (holding the graph read lock) unless it is explicitly
        # cancelled, so "gave up waiting" must also mean "told it to stop".
        run_id = run_id or ("bridge-%s" % uuid.uuid4())
        params = {
            "recursive": bool(recursive),
            "parallel": bool(parallel),
            "includeVanilla": bool(include_vanilla),
            "includePct": bool(include_pct),
            "runId": run_id,
        }
        if package_path:
            params["packagePath"] = package_path
        if uri:
            params["uri"] = uri
        if functions:
            params["functions"] = list(functions)
        if invocations:
            params["invocations"] = list(invocations)
        if pct_adapter_path:
            params["pctAdapterPath"] = pct_adapter_path
        if files:
            params["files"] = files
        try:
            resp = self.client.request("legend/executeTests", params,
                                       timeout=timeout if timeout else _tests_timeout(parallel))
        except TimeoutError:
            self._cancel_quietly(run_id)
            raise
        return resp.get("result") or resp.get("error")

    def cancel_tests(self, run_id=None, all_runs=False, timeout=30):
        """Stop an in-flight executeTests run via legend/cancelTests.

        Returns as soon as the run is signalled - the run itself then unwinds on the daemon (its
        in-flight test aborts, its teardown hooks still run) and its own executeTests call returns a
        partial result. An empty cancelledRunIds is not an error: it usually means the run had
        already finished.
        """
        params = {"all": bool(all_runs)}
        if run_id:
            params["runId"] = run_id
        resp = self.client.request("legend/cancelTests", params, timeout=timeout)
        return resp.get("result") or resp.get("error")

    def _cancel_quietly(self, run_id):
        """Best-effort cancel on a path that is already failing - never mask the original error."""
        try:
            self.cancel_tests(run_id=run_id)
            print("[bridge] timed out waiting for test run %s; cancelled it" % run_id, file=sys.stderr)
        except Exception as e:
            print("[bridge] failed to cancel timed-out test run %s: %s" % (run_id, e), file=sys.stderr)

    def execute_functions_parallel(self, functions, files=None, timeout=600):
        """Run several functions CONCURRENTLY on the (single) LSP daemon. Optionally compile `files`
        once (write-locked) FIRST, synchronously, so all executions run against the same compiled
        graph; then fire every legend/execute at once (read-locked on the server -> they run in
        parallel up to the server's request-pool size) and collect all results.

        Returns [{"function": fn, "result": <ExecuteGoResult|error>}, ...] in input order.
        This is the concurrent-custom-execution path enabled by the session's ReadWriteLock: many
        executions (readers) overlap; a compile (writer) is exclusive.
        """
        # 1) Compile the batch ONCE up front (if any), so the parallel executes don't each try to
        #    compile and contend on the write lock. Use executeGo-less compile via check_many.
        if files:
            batch = self.check_many(files)
            if isinstance(batch, dict) and batch.get("success") is False:
                return [{"function": fn, "result": batch} for fn in functions]

        # 2) Fire all executes without waiting (no files -> nothing to recompile per call).
        pending = []
        for fn in functions:
            msg_id, entry = self.client.request_async("legend/execute", {"function": fn})
            pending.append((fn, msg_id, entry))

        # 3) Collect all responses.
        results = []
        for fn, msg_id, entry in pending:
            try:
                resp = self.client.await_response(msg_id, entry, timeout=timeout)
                results.append({"function": fn, "result": resp.get("result") or resp.get("error")})
            except Exception as e:
                results.append({"function": fn, "result": {"success": False, "error": str(e)}})
        return results

    def set_option(self, name, value):
        """Set (value=True) or clear (value=False) a Pure runtime option in the LSP JVM so
        isOptionSet('<name>') reflects it live for subsequent go()/execute() runs - no restart.
        Maps to System.setProperty/clearProperty on 'pure.options.<name>' in the LSP process; the
        bridge (a separate process) cannot touch those properties itself, which is why this goes
        over the wire to the JVM that actually evaluates isOptionSet.
        """
        resp = self.client.request("legend/setOption", {"name": name, "value": bool(value)}, timeout=15)
        return resp.get("result") or resp.get("error")

    def delete_file(self, uri):
        """Unload a .pure source from the LSP session: removes it from the open-document set, deletes
        it from the Pure runtime, and clears its overlay, then recompiles. Use this to unload a
        throwaway go() wrapper (or any pushed file) so it stops participating in compilation - the fix
        for 'go__Any_MANY_ is defined more than once' when a previously-pushed overlay lingers.
        Deleting the file on the local disk does NOT clear its LSP overlay; this call does.
        Accepts a file:// uri, an absolute path, or a raw sourceId. Also forget it locally so we don't
        treat it as still-open on a later push.
        """
        with self._docs_lock:
            self.open_docs.pop(uri, None)
        resp = self.client.request("legend/deleteFile", {"uri": uri}, timeout=30)
        return resp.get("result") or resp.get("error")


class Handler(BaseHTTPRequestHandler):
    bridge: Bridge = None
    port: int = None
    socket_port: int = None

    def _send_json(self, obj, code=200):
        body = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        pass  # server activity already goes to the stderr log file

    @staticmethod
    def _validate_files(files):
        """Returns an error message string if `files` is malformed, else None.
        `files` itself being absent/empty is valid (means "no batch"), it's an explicit
        non-empty-but-broken list that's rejected.
        """
        if files is None:
            return None
        if not isinstance(files, list):
            return "'files' must be a list"
        if any(not isinstance(f, dict) or not f.get("uri") for f in files):
            return "every entry in 'files' needs a 'uri'"
        return None

    def _read_metadata_sidecar(self):
        """The metadata sidecar (/tmp/pure_lsp_server_<port>.json) written by server_cli.py at
        launch: pid, port, socket_port, repo_roots, jvm_args, java, started_at. Bridge-local (no
        JVM round-trip); absent/unreadable/malformed is normal (e.g. an old bridge launched before
        this existed, or the file was removed on shutdown of a since-replaced process) - just skip.
        """
        if self.port is None:
            return None
        path = "/tmp/pure_lsp_server_%d.json" % self.port
        try:
            with open(path) as f:
                data = json.load(f)
            return data if isinstance(data, dict) else None
        except (OSError, ValueError):
            return None

    def do_GET(self):
        if self.path == "/health":
            proc = self.bridge.client.proc
            result = {
                "alive": self.bridge.client.alive(),
                "pid": proc.pid if proc is not None else None,
                "transport": "stdio" if proc is not None else "socket",
                "port": self.port,
                "socket_port": self.socket_port,
            }
            metadata = self._read_metadata_sidecar()
            if metadata:
                # Non-clobbering: the live-computed fields above are authoritative; the sidecar only
                # adds extra launch-config context (repo_roots, jvm_args, java, started_at, ...).
                for k, v in metadata.items():
                    result.setdefault(k, v)
            self._send_json(result)
        elif self.path == "/status":
            try:
                self._send_json(self.bridge.status())
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        else:
            self._send_json({"error": "not found"}, 404)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            payload = json.loads(raw.decode("utf-8")) if raw else {}
        except Exception:
            payload = {}

        if self.path == "/check":
            uri = payload.get("uri")
            content = payload.get("content", "")
            if not uri:
                self._send_json({"error": "missing 'uri'"}, 400)
                return
            try:
                self._send_json(self.bridge.check(uri, content))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/check-batch":
            files = payload.get("files")
            if not files:
                self._send_json({"error": "missing/empty 'files' list"}, 400)
                return
            error = self._validate_files(files)
            if error:
                self._send_json({"error": error}, 400)
                return
            try:
                self._send_json(self.bridge.check_many(files))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/go":
            files = payload.get("files")
            error = self._validate_files(files)
            if error:
                self._send_json({"error": error}, 400)
                return
            try:
                self._send_json(self.bridge.execute_go(files=files))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/execute":
            # /execute {"function": "a::b::t():Boolean[1]", "files": [...]?, "arguments": [...]?,
            #           "pctAdapterPath": ...?, "beforeFunctionPath": ...?, "afterFunctionPath": ...?}
            # -> run one arbitrary function (not just go()); "arguments" are String[1] literals.
            fn = payload.get("function")
            files = payload.get("files")
            if not fn:
                self._send_json({"error": "missing 'function'"}, 400)
                return
            error = self._validate_files(files)
            if error:
                self._send_json({"error": error}, 400)
                return
            try:
                self._send_json(self.bridge.execute_function(
                    fn, files=files,
                    arguments=payload.get("arguments"),
                    pct_adapter_path=payload.get("pctAdapterPath"),
                    before_function_path=payload.get("beforeFunctionPath"),
                    after_function_path=payload.get("afterFunctionPath"),
                ))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/execute-parallel":
            # /execute-parallel {"functions": ["a::b::t1", "a::b::t2", ...], "files": [...]?} -> compile
            # files once, then run every function CONCURRENTLY on the daemon; returns per-function
            # results. This is the parallel custom-execution path.
            functions = payload.get("functions")
            files = payload.get("files")
            if not functions or not isinstance(functions, list):
                self._send_json({"error": "missing/invalid 'functions' list"}, 400)
                return
            error = self._validate_files(files)
            if error:
                self._send_json({"error": error}, 400)
                return
            try:
                self._send_json({"results": self.bridge.execute_functions_parallel(functions, files=files)})
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/execute-tests":
            # /execute-tests {"packagePath": "a::b"} | {"uri": "..."} | {"functions": [...]}
            #                | {"invocations": [{"path", "arguments"?, "label"?}, ...]}
            # (+ recursive/parallel/includeVanilla/includePct/pctAdapterPath/files/runId) -> run that
            # scope on the daemon. For packagePath/uri the daemon discovers the tests and preserves
            # per-subpackage <<test.BeforePackage>>/<<test.AfterPackage>> bracketing; for an explicit
            # `functions`/`invocations` list it skips discovery and runs them as one flat group (no
            # hooks). Either way the fan-out is server-side and bounded there.
            package_path = payload.get("packagePath")
            uri = payload.get("uri")
            functions = payload.get("functions")
            invocations = payload.get("invocations")
            if sum(1 for s in (package_path, uri, functions, invocations) if s) != 1:
                self._send_json(
                    {"error": "exactly one of 'packagePath', 'uri', 'functions' or 'invocations' "
                              "is required"}, 400)
                return
            files = payload.get("files")
            error = self._validate_files(files)
            if error:
                self._send_json({"error": error}, 400)
                return
            try:
                self._send_json(self.bridge.execute_tests(
                    package_path=package_path,
                    uri=uri,
                    functions=functions,
                    invocations=invocations,
                    recursive=payload.get("recursive", True),
                    parallel=payload.get("parallel", False),
                    include_vanilla=payload.get("includeVanilla", True),
                    include_pct=payload.get("includePct", True),
                    pct_adapter_path=payload.get("pctAdapterPath"),
                    files=files,
                    run_id=payload.get("runId"),
                    timeout=payload.get("timeout")))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/cancel-tests":
            # /cancel-tests {"runId": "..."} | {"all": true} -> stop an in-flight /execute-tests run.
            # Returns immediately; the run unwinds on the daemon and its own /execute-tests response
            # comes back with cancelled=true.
            run_id = payload.get("runId")
            all_runs = bool(payload.get("all"))
            if not run_id and not all_runs:
                self._send_json({"error": "missing 'runId' (or set 'all': true)"}, 400)
                return
            try:
                self._send_json(self.bridge.cancel_tests(run_id=run_id, all_runs=all_runs))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path in ("/set-option", "/unset-option"):
            # /set-option  {"name": "..."}  -> isOptionSet('name') becomes true in the LSP JVM
            # /unset-option {"name": "..."} -> isOptionSet('name') becomes false
            # Either endpoint also honours an explicit {"value": bool} for callers that prefer one
            # route; the path just supplies the default.
            name = payload.get("name")
            if not name:
                self._send_json({"error": "missing 'name'"}, 400)
                return
            value = payload.get("value", self.path == "/set-option")
            try:
                self._send_json(self.bridge.set_option(name, value))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/delete":
            # /delete {"uri": "..."} -> unload a .pure source from the session (open-docs + runtime +
            # overlay). uri may be a file:// uri, an absolute path, or a raw sourceId.
            uri = payload.get("uri")
            if not uri:
                self._send_json({"error": "missing 'uri'"}, 400)
                return
            try:
                self._send_json(self.bridge.delete_file(uri))
            except Exception as e:
                self._send_json({"error": str(e)}, 500)
        else:
            self._send_json({"error": "not found"}, 404)


def run(repo_roots, classpath, host, port, java, stderr_log_path, on_start=None, jvm_args=None,
        socket_port=None):
    # Bind first: fail fast on a port conflict instead of spawning an expensive
    # java subprocess (which can take minutes to boot at engine scale) only to
    # discover the port is taken and leak it as an orphan.
    httpd = ThreadingHTTPServer((host, port), Handler)

    bridge = None
    try:
        bridge = Bridge(java, classpath, stderr_log_path, jvm_args=jvm_args, socket_port=socket_port)
        if on_start is not None:
            on_start(bridge)
        bridge.initialize(repo_roots)
    except Exception:
        httpd.server_close()
        # Only tear down a JVM we OWN (stdio transport). In socket mode the daemon is intentionally
        # independent and must survive this bridge - never terminate it here.
        if bridge is not None and bridge.client.proc is not None and bridge.client.alive():
            bridge.client.proc.terminate()
        raise

    Handler.bridge = bridge
    Handler.port = port
    Handler.socket_port = socket_port
    jvm_pid = bridge.client.proc.pid if bridge.client.proc is not None else "detached-daemon"
    print("[bridge] HTTP listening on %s:%d (java: %s)" % (host, port, jvm_pid), file=sys.stderr)
    print("[bridge] waiting for Pure runtime to become ready...", file=sys.stderr)

    def wait_and_report():
        status = bridge.wait_ready()
        print("[bridge] runtime status:", json.dumps(status), file=sys.stderr)

    threading.Thread(target=wait_and_report, daemon=True).start()
    httpd.serve_forever()
