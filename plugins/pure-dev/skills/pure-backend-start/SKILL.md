---
name: pure-backend-start
description: "Starts (or confirms) the standalone legend-engine backend - engine Server + H2 + local metadata server on fixed ports 9095/9092 - so a Pure LSP started with -Dlegend.test.* routes execute()/go() through real plan-generation instead of the interpreted engine. Starts only the backend, not the LSP. Use when the user wants to run Pure queries 'through the real engine', 'through plan-gen' or 'via the backend server', reproduce compiled-mode behaviour the interpreted engine cannot, or mentions legend.test.server, EngineServerForTest or LegendTest."
---

# Start the standalone engine backend (H2 + metadata server + engine Server)

## Why this exists

The Pure LSP `go()`/`pure-lsp-go` runs the **interpreted** engine, which (a) can't reproduce
compiled-mode behaviour (plan generation + generated `Specifics.prepare` Java, e.g. relational
graph-fetch temp-table nodes) and (b) has its own gaps (e.g. interpreted RFPM graph-fetch is
unimplemented). `meta::pure::router::execute(...)` wraps the query in the `mayExecuteLegendTest`
native which — if `legend.test.server.host`/`port`/`clientVersion`/`serverVersion` System
properties are set on the JVM — does **real plan-generation and POSTs to a running backend
Server** instead. This skill boots that backend on fixed ports; `pure-lsp-launch-engine` then points
the LSP JVM at it via `--jvm-arg=-D...`.

Result: `go()` through the so-configured LSP runs the **same plan-gen + execute path as
`Test_*_UsingPureClientTestSuite`**, verified end-to-end (returns real query results and shows
generated SQL in the `go()` output trace), without a 15-30 min `mvn test`.

## The launcher

`org.finos.legend.engine.ide.EngineServerForTest` (in the `legend-engine-pure-ide-light-http-server`
module, alongside `PureIDELight`). It starts H2 (fixed 9092), a local test metadata server
(dynamic), and the full `org.finos.legend.engine.server.Server` on a FIXED port (9095), then
blocks forever.

## Run the script

All of check-if-already-up / compile-once / generate-classpath-once / launch-in-background /
poll-for-ready is implemented deterministically in `bin/pure-backend-start`:

```bash
pure-backend-start [--port N] [--h2-port N] [--force-classpath]
```

Defaults: `--port 9095`, `--h2-port 9092`. If something's already listening on `--port`, it reports
that and exits 0 without relaunching (it's a full engine JVM — never stack a second one).

On success it prints one JSON line to stdout, e.g. `{"port": 9095, "h2Port": 9092, "state": "ready"}`,
and exits 0. Exit codes:
- `1` — the launcher module didn't compile, or its runtime classpath is missing/empty (pass
  `--force-classpath` to regenerate the cached classpath after a dependency change)
- `3` — launched but never reached `READY` within the timeout (~2 minutes) — check
  `/tmp/engine-server.log`

## Then: point the LSP at it

This skill only brings up the backend — it does not start the Pure LSP. `pure-lsp-launch-engine`
calls `pure-backend-start` **automatically by default** and wires the resulting
`-Dlegend.test.server.*` jvm-args into the LSP launch; pass `--no-backend` to skip this and launch
interpreted-only instead. For a custom/manual launch, use `pure-lsp-server`'s `--jvm-arg` passthrough
with the flags `pure-backend-start` prints (see `references/lsp-devloop-internals.md`).

## Things worth knowing / gotchas

- **The go() wrapper must seed the DB and use the right execute FQN** — `createTablesAndFillDb()`
  first, `meta::pure::router::execute` not `meta::pure::mapping::execute`, and one `go()` per
  session (reuse `welcome.pure`). All three are the database-setup and `welcome.pure` conventions
  in `references/lsp-devloop-usage-rules.md`.
- **Pure-only edits do NOT require rebuilding/restarting this backend Server** — because plan
  generation runs LOCALLY in the LSP's interpreted JVM and only plan *execution* is delegated here.
  That is how `legendExecute` in `core/pure/protocol/vX_X_X/invocations/execution.pure` is wired
  (see `pure-lsp-go`'s "Execution routing" section): `let planGenerated = if(true, {|local
  executionPlan(...)}, {|remote generatePlan(...)})` keeps plan-gen local, so it uses the Pure graph
  in the LSP session (reflecting files you've `pure-lsp check`ed live, on top of the jar baseline),
  and only `executePlan($host,$port)` hits this server. Plan execution is almost all Java, so it
  ignores your Pure edits. Net loop with NO `mvn install` / NO server restart: edit `.pure` →
  `pure-lsp check` it into the session → `go()` → live local plan-gen + server execute. (Rebuild/
  restart this server only for Java execution-layer changes, or if you flip plan-gen to run
  remotely by editing that `if(true,...)`.)
- **clientVersion `vX_X_X`** is the dev placeholder and is accepted (see `PureClientVersions`).
- Logs: `/tmp/engine-server.log`. Stop it with `pkill -f EngineServerForTest` (match the java
  process precisely; a bare pgrep can match your own shell command).
