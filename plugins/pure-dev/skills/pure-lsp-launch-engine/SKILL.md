---
name: pure-lsp-launch-engine
description: "Launches the Pure LSP bridge scoped to legend-engine with defaults baked in - the go-to entrypoint for 'just start the LSP for legend-engine'. Brings the backend engine Server up by default so execute()/go() use real plan-generation; --no-backend opts out. Use when the user wants to start/launch/boot the Pure LSP for legend-engine with nothing unusual - unlike pure-lsp-connect, which attaches to a daemon that is already running."
---

# Launch the Pure LSP for legend-engine (defaults baked in)

The real entrypoint for the common case: legend-engine-scoped LSP, backend engine Server on by
default, no per-run decisions. A thin wrapper over `pure-lsp-launch legend-engine`; for a scope the
project registry doesn't describe, build the `pure-lsp-server` command by hand per
`references/lsp-devloop-internals.md`.

## Run the script

```bash
pure-lsp-launch-engine [--port N] [--socket-port N] [--java PATH]
                       [--backend-server-host HOST] [--backend-server-port N] [--no-backend]
                       [--source legend-pure] [--full-source]
                       [extra args passed through to pure-lsp-server, e.g. --jvm-arg=-D...]
```

Defaults: `--port 8991`, `--socket-port 9100` (this shop's standing convention for `LegendLspServer`,
shared with `pure-lsp-connect`/`pure-lsp-restart`'s default), `--java` = plain `java` on `PATH` (JDK 11).
`$LEGEND_ENGINE_ROOT` defaults to the checkout containing the current directory.
Only legend-engine is source-rooted; legend-pure comes from the classpath jar and needs no checkout
or `$LEGEND_PURE_ROOT`. Add `--source legend-pure` to edit platform `.pure` live too (needs
`$LEGEND_PURE_ROOT`), or `--full-source` for the whole chain.

**Backend is on by default.** If nothing's listening on 127.0.0.1:9095 the script runs
`pure-backend-start` itself and wires the resulting `-Dlegend.test.server.*` jvm-args in, so
`execute()`/`go()` take the real plan-gen path. `--no-backend` gives a plain interpreted session. A
failed backend start aborts the launch rather than silently degrading to interpreted — so if you
want interpreted, ask for it explicitly.

Whenever the backend is wired in, `ExecPlan`/`PlanLocal`/`FullInteractiveExec` also default to
`true` — the same "3 trues" `legendLspServerConfig.json` sets for the engine-side config-driven
launcher of this same daemon. Plan-gen then runs locally against the live graph rather than the
backend's own jars, so a Pure-only edit is reflected immediately with no jar rebuild. Override any
of them with a trailing `--jvm-arg=-Dpure.options.X=...` — it lands after these defaults and wins.

Since the daemon it starts is on a fixed socket port, re-running this after a daemon on that port is
already up reconnects to the existing warm session (via `pure-lsp-server`'s own reconnect logic —
the same behavior `pure-lsp-connect` relies on) rather than double-booting a second JVM.

**If a bridge is already listening on the HTTP port, this script refuses to launch and exits 0**
after printing that bridge's `/health` (repo-roots, jvm-args, socket port) so you can see whether
it is the session you wanted. It does not try to start a second one: the bridge binds its HTTP port
before doing anything else, so a second launch dies instantly on `Address already in use` — and
this script would then poll that same port, get the EXISTING bridge's `ready`, and report a
successful launch while the configuration you just asked for was silently discarded. To run a
second independent session pass a free `--port`/`--socket-port`; to rebuild this one use
`pure-lsp-restart --socket-port 9100`.

## It backgrounds itself, waits for ready, and warms welcome.pure — then returns

Unlike a bare `pure-lsp-server` call this does **not** block your terminal. It launches the bridge
fully detached (`setsid nohup ... & disown`, so a process-group teardown can't reap it), polls
`pure-lsp status` until `ready`/`failed`, then pushes and compiles legend-engine's own `welcome.pure`
(`$LEGEND_ENGINE_ROOT/welcome.pure`) into the session overlay via `pure-lsp check` — **not** `go`, so
it's compiled and go()-ready but never executed — before printing a final summary and exiting. You
get your shell prompt (or tool call) back immediately with the bridge already running and warmed, no
separate verification call needed.

If `welcome.pure` doesn't exist yet, a minimal `go():Any[*]` wrapper is created for you — but only
when legend-engine's checkout actually `.gitignore`s it (true today), so this never leaves an
untracked file a later `git add -A` could sweep into a commit. If the checkout stopped ignoring it,
the launcher skips creation and tells you to add the ignore rule yourself.

Exit codes: `0` ready and warmed (or `welcome.pure` missing/failed to compile — that's a non-fatal
warning printed to stderr, not a failure, since the LSP itself is still usable), `1` bad args or
missing server jar/dependency dir, `3` the bridge never reached `ready` (or reported `failed`)
within the timeout — see the two log paths it prints.

Two separate logs, one writer each: `/tmp/pure_lsp_bridge_8991.log` (the Python bridge — argument
errors, classpath resolution, bind failures) and `/tmp/pure_lsp_server_8991.log` (the LSP daemon
JVM's own output). They used to be one file written by both, which meant a relaunch truncated the
log out from under the still-running daemon.

## Then

The shared dev-loop conventions (talking to the bridge, `welcome.pure`, database setup) are in
`references/lsp-devloop-usage-rules.md`.

Hand the resolved `--port` off to `pure-lsp-check`/`pure-lsp-go`/`pure-lsp status` for the actual
compile/execute work — `go()` is already warmed and ready to run against `welcome.pure` immediately.
