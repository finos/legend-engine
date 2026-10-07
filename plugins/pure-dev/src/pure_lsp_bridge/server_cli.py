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
"""Launches the HTTP<->LSP bridge for the Legend Pure LSP server.

$LEGEND_PURE_ROOT / $LEGEND_ENGINE_ROOT are your local checkouts. Prefer `pure-lsp-launch
<project>`; see references/lsp-devloop-internals.md for building this command by hand. --server-jar and --classpath-file are typically resolved from a project's own
Maven classpath (via pure-lsp-classpath), not a local build - see pure-lsp-common's
resolve_lsp_server_jar for the resolution order.

Example (engine-scale, classpath computed + cached by the pure-lsp-classpath skill - the project's
own classpath already carries the server jar and its runtime deps, so no --dependency-classpath-file
is needed):
    pure-lsp-server \\
        --repo-root "$LEGEND_PURE_ROOT" --repo-root "$LEGEND_ENGINE_ROOT" \\
        --server-jar .../legend-pure-lsp-server-<version>.jar \\
        --classpath-file "$(pure-lsp-classpath legend-engine)"

Example (a project whose own classpath does NOT carry the server jar - e.g. one added via
PURE_DEV_EXTRA_PROJECTS_JSON with no pure-ide-light module of its own - borrow it, and the deps it
needs, from legend-engine's classpath instead):
    pure-lsp-server \\
        --repo-root "$SOME_EXTRA_PROJECT_ROOT" \\
        --server-jar .../legend-pure-lsp-server-<version>.jar \\
        --classpath-file "$(pure-lsp-classpath some-extra-project)" \\
        --dependency-classpath-file "$(pure-lsp-classpath legend-engine)" \\
        --prefer-server-pure-jars

Example (legend-pure only, no Maven classpath at all - --dependency-classpath-file also
accepts a plain directory of jars, e.g. the server module's own target/dependency, for this
classpath-less case):
    pure-lsp-server \\
        --repo-root "$LEGEND_PURE_ROOT" \\
        --server-jar "$LEGEND_PURE_ROOT"/legend-pure-lsp/legend-pure-lsp-server/target/legend-pure-lsp-server-*.jar \\
        --dependency-classpath-file "$LEGEND_PURE_ROOT"/legend-pure-lsp/legend-pure-lsp-server/target/dependency
"""
import argparse
import json
import os
import signal
import sys
import time

from . import bridge as bridge_module
from . import classpath as classpath_module


def _write_metadata_sidecar(path, pid, port, socket_port, repo_roots, jvm_args, java):
    """Bridge-local process metadata (pid, ports, launch config) for /health to merge in without a
    JVM round-trip. Best-effort: a write failure must never prevent the daemon from starting."""
    try:
        with open(path, "w") as f:
            json.dump({
                "pid": pid,
                "port": port,
                "socket_port": socket_port,
                "repo_roots": repo_roots,
                "jvm_args": jvm_args,
                "java": java,
                "started_at": time.time(),
            }, f)
    except OSError as e:
        print("[server-cli] could not write metadata sidecar %s: %s" % (path, e), file=sys.stderr)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo-root", required=True, action="append", dest="repo_roots", metavar="DIR",
                     help="Workspace root handed to the LSP as a workspaceFolder. Repeatable: pass "
                          "--repo-root once per repo (e.g. --repo-root <legend-pure> --repo-root "
                          "<legend-engine>). Each root is scanned for */src/main/resources/*.definition.json "
                          "repos, and a repo found in a workspace root is loaded from SOURCE and OVERRIDES "
                          "the same-named repo from the classpath jars.")
    ap.add_argument("--server-jar", required=True, help="Path to legend-pure-lsp-server-<version>.jar")
    ap.add_argument("--dependency-classpath-file", default=None,
                     help="Classpath file (same format as --classpath-file), OR a plain directory of "
                          "jars (e.g. legend-pure-lsp-server's own target/dependency, for a classpath-"
                          "less plain launch) - supplying the LSP server's own runtime deps (LSP4J, "
                          "the Pure interpreter, etc.) when --classpath-file doesn't already carry "
                          "them, e.g. borrowed from another project's pure-lsp-classpath output. Omit "
                          "when --classpath-file's project already carries everything the server jar "
                          "needs (true for legend-engine, whose anchor module - "
                          "pure-ide-light-http-server - pulls legend-pure-lsp-server along with it).")
    ap.add_argument("--classpath-file", default=None,
                     help="Maven dependency:build-classpath file supplying host jars (e.g. legend-engine). "
                          "When set, any --dependency-classpath-file jar with a filename the host classpath "
                          "already provides is dropped (avoiding duplicate Pure repositories); jars the host "
                          "doesn't provide are kept even if named legend-pure-*.")
    ap.add_argument("--prefer-server-pure-jars", action="store_true",
                     help="Resolve legend-pure-* duplicates by ARTIFACT name rather than exact filename: "
                          "drop every host-classpath legend-pure-* jar whose artifact "
                          "--dependency-classpath-file also supplies, so the platform repos load from "
                          "exactly one version - the one --dependency-classpath-file was resolved against. "
                          "Needed when the host classpath pins a different legend-pure than "
                          "--dependency-classpath-file's source (otherwise startup aborts with \"Invalid "
                          "URLs for '/platform/...' - different content\"). Pair with the NON-shaded "
                          "server jar, which does not embed platform.")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=8991)
    ap.add_argument("--java", default="java")
    ap.add_argument("--jvm-arg", action="append", default=[], dest="jvm_args", metavar="ARG",
                     help="Extra JVM argument to pass to the LSP server process (repeatable). "
                          "E.g. --jvm-arg -Dlegend.test.server.host=127.0.0.1 --jvm-arg -Dlegend.test.server.port=9095 "
                          "to route Pure execute()/go() through a running backend engine Server (plan-gen + execute) "
                          "instead of the interpreted engine.")
    ap.add_argument("--stderr-log", default=None,
                     help="Where the Java process's stderr (all LSP logging) is written "
                          "(default: /tmp/pure_lsp_server_<port>.log, so concurrent bridges don't clobber each other)")
    ap.add_argument("--socket-port", type=int, default=None,
                     help="Run the LSP JVM as a DETACHED daemon listening on this TCP port (loopback) "
                          "instead of as a piped stdio child. The bridge connects over the socket; the "
                          "JVM then survives the bridge being killed, and a later bridge reconnects to "
                          "the same warm session. Strongly recommended for a durable dev loop. If a "
                          "daemon is already listening on this port, it is reused (not respawned).")
    args = ap.parse_args(argv)
    stderr_log = args.stderr_log or ("/tmp/pure_lsp_server_%d.log" % args.port)
    metadata_path = "/tmp/pure_lsp_server_%d.json" % args.port

    # Detach into our own session/process-group so that when the shell (or tool-call) that
    # launched us has its process group torn down, the resulting SIGTERM/SIGHUP is NOT delivered
    # to this launcher - which would otherwise fire the shutdown handler below and kill the
    # expensive LSP JVM. `nohup`/backgrounding alone does not change the process group, so this is
    # required for the bridge to outlive the call that started it. setsid() fails with EPERM if we
    # are already a session leader (e.g. launched under `setsid`), which is fine - already detached.
    try:
        os.setsid()
    except OSError:
        pass

    repo_roots = [os.path.abspath(r) for r in args.repo_roots]
    server_jar = os.path.abspath(args.server_jar)

    dep_entries = []
    if args.dependency_classpath_file:
        dep_entries = classpath_module.read_dependency_entries(args.dependency_classpath_file)
        print("[server-cli] loaded %d dependency classpath entries from %s"
              % (len(dep_entries), args.dependency_classpath_file), file=sys.stderr)

    host_entries = []
    if args.classpath_file:
        host_entries = classpath_module.read_classpath_file(args.classpath_file)
        print("[server-cli] loaded %d host classpath entries from %s" % (len(host_entries), args.classpath_file),
              file=sys.stderr)

    classpath_entries = classpath_module.resolve_server_classpath(
        server_jar, dep_entries, host_entries,
        prefer_server_pure_jars=args.prefer_server_pure_jars)
    classpath = ":".join(classpath_entries)

    state = {}

    def shutdown(signum, frame):
        # stdio transport: launcher and JVM are lifecycle-bound (JVM reads JSON-RPC from the
        # launcher's stdin pipe), so terminate it explicitly and accept it dies with us.
        # socket transport: state["proc"] is None (the daemon is detached and owned by no one), so
        # we exit WITHOUT touching it - the JVM keeps serving and a new bridge reconnects. This is
        # the whole point of --socket-port: the launcher/bridge is disposable, the JVM persists.
        # Best-effort: removal must never raise or block the rest of shutdown (e.g. terminating the
        # java child in stdio transport), regardless of transport or whether the file even exists.
        try:
            os.remove(metadata_path)
        except OSError:
            pass

        proc = state.get("proc")
        if proc is not None and proc.poll() is None:
            print("[server-cli] terminating java pid %d (stdio transport)" % proc.pid, file=sys.stderr)
            proc.terminate()
        else:
            print("[server-cli] exiting; LSP daemon (socket transport) left running", file=sys.stderr)
        sys.exit(0)

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    def _on_start(b):
        state["proc"] = b.client.proc
        pid = b.client.proc.pid if b.client.proc is not None else None
        _write_metadata_sidecar(metadata_path, pid, args.port, args.socket_port, repo_roots,
                                 args.jvm_args, args.java)

    try:
        bridge_module.run(
            repo_roots=repo_roots,
            classpath=classpath,
            host=args.host,
            port=args.port,
            java=args.java,
            stderr_log_path=stderr_log,
            on_start=_on_start,
            jvm_args=args.jvm_args,
            socket_port=args.socket_port,
        )
    except KeyboardInterrupt:
        shutdown(None, None)


if __name__ == "__main__":
    main()
