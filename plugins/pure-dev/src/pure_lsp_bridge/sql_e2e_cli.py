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
"""Run legend-engine SQL e2e parity cases against a running pure-lsp bridge.

Each corpus case is executed by the same natives the compiled suite's dev loop uses, invoked
directly with its id and mode - no wrapper function is generated into framework.pure and no file is
edited.

    pure-sql-e2e ids   structural/null_semantics
    pure-sql-e2e run   null_equals_null null_not_equals --mode tds
    pure-sql-e2e case  null_equals_null --mode tds
    pure-sql-e2e adhoc "SELECT 1 FROM dates WHERE id = 1" --mode both
"""
import argparse
import os
import sys
import urllib.error

from .client_cli import MAX_TESTS_PER_RUN, BridgeError, _post, cmd_execute_tests
from .discovery import resolve_port

E2E = "meta::external::query::sql::e2e::"
CASE_INDEX = E2E + "caseIndex_String_1__String_1_"
CHECK_CASE = E2E + "checkCase_String_1__String_1__Boolean_1_"
DIAG_CASE = E2E + "diagCase_String_1__String_1__String_1_"
DIAG_ADHOC = E2E + "diagAdhoc_String_1__String_1__String_1_"

# One corpus entry is one test entry, so the ceiling is client_cli's, for its reason: a run holds
# the graph read lock throughout, so every compile queues behind it and the session serves stale
# code until it finishes. At tens of seconds of interpreted plan-gen per case, a whole category
# outlives the edit-test loop this exists to serve.
MAX_ENTRIES = MAX_TESTS_PER_RUN

MODES = {"tds": ["TDS"], "relation": ["Relation"], "both": ["TDS", "Relation"]}

COMPILED_MODULE = "legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests"

# Resolving a case warms its reference-Postgres result, so enumerating a wide filter is slow the
# first time a daemon sees it.
INDEX_TIMEOUT = 1800
DIAG_TIMEOUT = 900


def _execute(args, function, arguments, timeout):
    payload = {"function": function, "arguments": list(arguments)}
    result = _post(args, "/execute", payload, timeout=timeout)
    if not result.get("success"):
        raise BridgeError(_failure_text(result))
    return result.get("output") or ""


def _failure_text(result):
    """A Pure-level failure arrives as {"success": false, "error"}, but a JVM-level one (a missing
    class on the daemon's classpath, say) arrives as a raw JSON-RPC {"code", "message", "data"}
    with no 'error' at all - which read as "failed with no error reported" and buried the cause."""
    if result.get("error"):
        return result["error"]
    data = (result.get("data") or "").strip()
    if data:
        return "%s: %s" % (result.get("message", "execution failed"), data.splitlines()[0])
    return result.get("message") or "execution failed with no error reported"


def _case_index(args, filters):
    """[(id, path)] for the filters, in corpus order, as the daemon resolves them.

    Pure's println renders a String with surrounding quotes, so the multi-line payload arrives as
    one quoted block - the first and last lines carry a stray apostrophe. No corpus id or path
    contains one, so stripping them per line is unambiguous.
    """
    output = _execute(args, CASE_INDEX, [",".join(filters)], INDEX_TIMEOUT)
    entries = []
    seen = set()
    for line in output.splitlines():
        line = line.strip().strip("'").strip()
        if line.count("|") != 1:
            continue
        case_id, _, path = line.partition("|")
        if path in ("TDS", "Relation") and line not in seen:
            seen.add(line)
            entries.append((case_id, path))
    return entries


def _for_mode(entries, mode):
    wanted = MODES[mode]
    return [e for e in entries if e[1] in wanted]


def _too_many(filters, mode, count):
    print(
        "error: %d entries resolved for %s (--mode %s); a run is capped at %d, past which it holds\n"
        "       the graph read lock long enough that the session serves stale code.\n"
        "       Narrow it (--mode tds, or name individual case ids - `pure-sql-e2e ids %s` lists\n"
        "       them), or run it compiled:\n"
        "  mvn test -pl %s \\\n"
        "      -Dtest=TestPostgresParity -Dtest.filter=%s"
        % (count, " ".join(repr(f) for f in filters), mode, MAX_ENTRIES, filters[0],
           COMPILED_MODULE, filters[0]),
        file=sys.stderr,
    )


def cmd_ids(args):
    entries = _for_mode(_case_index(args, args.filters), args.mode)
    for case_id, path in entries:
        print("%s|%s" % (case_id, path))
    print("%d entries" % len(entries), file=sys.stderr)
    return 0


def cmd_run(args):
    entries = _for_mode(_case_index(args, args.filters), args.mode)
    if not entries:
        print("error: no corpus case matched %s (--mode %s)"
              % (" ".join(repr(f) for f in args.filters), args.mode), file=sys.stderr)
        return 2
    if len(entries) > MAX_ENTRIES:
        _too_many(args.filters, args.mode, len(entries))
        return 2

    payload = {
        "parallel": not args.serial,
        "invocations": [
            {"path": CHECK_CASE, "label": "%s|%s" % (case_id, path), "arguments": [case_id, path]}
            for case_id, path in entries
        ],
    }
    return cmd_execute_tests(args, parallel=not args.serial, payload=payload)


def cmd_case(args):
    for path in MODES[args.mode]:
        print(_execute(args, DIAG_CASE, [args.case_id, path], DIAG_TIMEOUT))
    return 0


def cmd_adhoc(args):
    # 'Both' is a SqlE2EPath value of its own - the native runs each side independently and reports
    # them together, which is more useful here than two separate executions.
    mode = "Both" if args.mode == "both" else MODES[args.mode][0]
    print(_execute(args, DIAG_ADHOC, [args.sql, mode], DIAG_TIMEOUT))
    return 0


def _add_mode(parser):
    parser.add_argument("--mode", choices=sorted(MODES), default="both",
                        help="execution path, applied to every case in the run (default: both). "
                             "'both' doubles the entry count against the %d-entry cap." % MAX_ENTRIES)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default=os.environ.get("PURE_LSP_HOST", "127.0.0.1"))
    ap.add_argument("--port", type=int, default=None,
                    help="bridge HTTP port (default: $PURE_LSP_PORT, else a discovered live bridge)")
    sub = ap.add_subparsers(dest="command", required=True)

    filters_help = ("an exact case id, a category ('structural/joins'), a 'prefix*', or '' for the "
                    "whole corpus; several are unioned")

    p_ids = sub.add_parser("ids", help="list the (case id, path) entries a filter resolves to")
    p_ids.add_argument("filters", nargs="+", help=filters_help)
    _add_mode(p_ids)

    p_run = sub.add_parser("run", help="run the resolved cases as a test tree - PASS is green, every "
                                        "other status red, independent of the corpus baseline")
    p_run.add_argument("filters", nargs="+", help=filters_help)
    _add_mode(p_run)
    p_run.add_argument("--serial", action="store_true",
                       help="run the cases one at a time instead of concurrently")
    p_run.add_argument("--json", action="store_true",
                       help="print the raw result payload instead of the rendered tree")

    p_case = sub.add_parser("case", help="full diagnostic detail for one case")
    p_case.add_argument("case_id")
    _add_mode(p_case)

    p_adhoc = sub.add_parser("adhoc", help="run SQL that is not in the corpus against both Legend "
                                            "and the reference Postgres")
    p_adhoc.add_argument("sql")
    _add_mode(p_adhoc)

    args = ap.parse_args(argv)
    if args.port is None:
        args.port = resolve_port()
    handlers = {"ids": cmd_ids, "run": cmd_run, "case": cmd_case, "adhoc": cmd_adhoc}

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
