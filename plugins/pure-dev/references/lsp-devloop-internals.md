# Pure LSP dev-loop: internals

How the fast Pure dev loop works under the hood — the reference behind the pure-dev skills. The loop
has two moving parts alongside your `finos-legend-pure` / `finos-legend-engine` checkouts:

- **The bridge** (this plugin's Python: `bin/pure-lsp*`, `src/pure_lsp_bridge/`) — a warm HTTP
  front end.
- **The LSP server JVM** (`legend-pure-lsp/legend-pure-lsp-server` in `finos-legend-pure`) — the
  Pure runtime the bridge drives.

Skills: `pure-lsp-connect` / `pure-lsp-status` / `pure-lsp-check` / `pure-lsp-go` /
`pure-lsp-execute` / `pure-lsp-set-option` / `pure-lsp-classpath` /
`pure-lsp-execute-parallel` / `pure-lsp-sync-hook` / `pure-backend-start` / `pure-lsp-launch-engine` /
`pure-lsp-sql-e2e` / `pure-chain-update` / `trim-comments`.
See `lsp-devloop-usage-rules.md` for how to drive it.

`bin/pure-lsp-restart` (kill the daemon + relaunch the bridge with its captured argv, for a runtime
stuck in `recovering`/`failed`) has no skill of its own — it is documented in `pure-lsp-status`,
which is where you decide whether a restart is warranted. `bin/pure-lsp-option` likewise backs
`pure-lsp-set-option`, and `bin/pure-lsp-common` is a sourced bash library, not a command.

## Runtime options are live system-property reads

`isOptionSet('X')` in the LSP resolves to `Boolean.getBoolean("pure.options.X")`, evaluated on every
call (default `RuntimeOptions.systemPropertyOptions("pure.options.")`). So options are controllable:
- **Static:** launch with `pure-lsp-server --jvm-arg=-Dpure.options.X=true`.
- **Dynamic (mid-session, no restart):** bridge `POST /set-option {"name":"X"}` /
  `POST /unset-option {"name":"X"}` → LSP `@JsonRequest("legend/setOption")` →
  `System.setProperty`/`clearProperty`. Effective on the next `go()`/`execute()`.

The options that drive execute() routing are gated in `execution.pure` and `router_entry.pure`.
**They are documented in exactly one place — the `pure-lsp-set-option` skill** (per-option table:
what SET does, what the UNSET default is, how they interact). Do not restate them here: this
reference previously carried its own copy, it fell out of date when `FULL_INTERACTIVE` moved off
`PlanLocal` onto the separate `FullInteractiveExec` flag, and the two descriptions then
contradicted each other.

The working dev-loop combination is `ExecPlan` + `PlanLocal` + `FullInteractiveExec` set, with
`ExecDebug`/`ShowLocalPlan` unset (quiet) — see `pure-lsp-set-option` for the full per-intent matrix.
`ForceInterpreted` overrides all of it and forces the interpreted path even with a backend up.

## Arbitrary + concurrent function execution

The LSP can run any zero-arg function (not just `go()`) and run several concurrently on one daemon:
- `LegendPureSession` uses a fair `ReentrantReadWriteLock`: graph-mutating methods
  (reinitialize / setClasspathRepositoryNames / restoreFromDisk / modifyAndCompile /
  applyBulkChangesAndCompile) take the WRITE lock (exclusive); `executeFunction`/`executeGo` take the
  READ lock (concurrent). The RW lock guarantees no compile interleaves an in-flight execution.
- Each execution gets its OWN `FunctionExecutionInterpreted` (own console + cancel flag) over the
  shared read-only compiled runtime, plus its own thread-local `ModelRepositoryTransaction` (rolled
  back after) to isolate/reclaim transient instances. Safe because instance allocation is thread-safe
  (AtomicInteger ids) and transactions are thread-local.
- Requests: `@JsonRequest("legend/execute")` (function path + optional files). Bridge:
  `POST /execute {"function": "..."}`. Executions run on their own bounded pool
  (`legend.lsp.executionConcurrency`, default `0.75 x cores`), NOT the shared request pool, so a
  batch cannot occupy every request thread and starve `status`/`check`/`cancelTests`.
- `POST /execute-parallel {"functions": [...]}` still exists — Python firing N concurrent
  `legend/execute` calls — but the CLI no longer uses it: `execute-parallel` sends the function
  list to `/execute-tests` instead (next section), which gives per-entry results and cancellation.
  The legacy endpoint is safe but returns only `{results:[{function, ExecuteGoResult}]}`.
- **Shared-backend caveat:** parallel executions that WRITE the shared H2 (e.g. concurrent
  `createTablesAndFillDb()`) RACE. Do table setup once serially, then run read-only executes in
  parallel. Parallel compile-check + local plan-gen + read-only queries are safe.

## Scoped test runs (`legend/executeTests`)

Batch execution lives on the SERVER, not in the bridge. That matters because the IntelliJ plugin
speaks lsp4j directly and never goes through the bridge; a server-side method is the only way both
clients share one implementation. IntelliJ always worked this way (one scoped `executeTests` per
run); the CLI was the outlier until its explicit-function path moved here too.

- `@JsonRequest("legend/executeTests")` takes exactly one of `{packagePath | uri | functions}`, plus
  `recursive`, `parallel`, `includeVanilla`, `includePct`, `pctAdapterPath`, `files`, `runId`.
  Bridge: `POST /execute-tests`; CLI: `--package`/`--source` **or a bare function list** on
  `execute` (serial) and `execute-parallel` (concurrent). Concurrency is chosen by the subcommand,
  not a flag.
- `functions` skips discovery and runs the named functions as one flat node: they need not carry
  `<<test.Test>>` nor share a package, and get no `<<test.BeforePackage>>` bracketing. An
  unresolvable path fails the run up front rather than being reported as a failed test.
- **Discovery** (`TestDiscovery`) delegates to `TestCollection` in legend-pure-m3-core — the same
  walker `PureTestBuilder` uses. File scope is the identical walk with a source-id predicate added
  to the filter, rooted at the deepest package covering the file's tests (walking from `::` would
  touch the whole platform graph). `TestCollection` gathers Before/After hooks *outside* its test
  filter and every node's constructor runs `pruneBeforeAfters`, so narrowing to one file keeps the
  hooks that file needs and drops the rest automatically.
- **Nested hooks are the crux.** A wide package carries hooks scoped to different subpackages plus
  inherited ones from ancestors, so the run reproduces `PureTestBuilder#buildSuite` exactly: per
  node, before hooks → subpackage nodes (sorted by package name) → the node's own tests (sorted by
  name, from `getPureAndAlloyOnlyFunctions()`) → after hooks. Hooks execute as reported entries, and
  a failing hook does NOT abort the run — both match the JUnit suite, which keeps a scoped run
  diffable against surefire. Do NOT use `TestTools.findNearestBeforePackageFunction` here; it stops
  at the first hook (right for `legend/getSetupTeardown`'s single test, wrong for a batch).
- **Parallelism respects the tree.** The whole batch holds ONE read lock; the walk is serial because
  a node's hooks bracket its subtree and siblings share state. `parallel` fans out only the tests
  within a single node, which already share that node's completed before-state. Worker threads don't
  take the read lock themselves — the calling thread holds it, which already excludes writers.
- **A scoped run holds the read lock for its whole duration**, which is what guarantees the graph
  cannot change under a suite mid-run (the same guarantee a JUnit suite over a fixed graph has). The
  cost: compiles are writers, so for as long as a big run lasts, `check`/`go`/the auto-sync hook
  will block waiting for it. That is the right trade — interleaving compiles would mean tests in one
  run executing against different graphs — but it makes a wide `--package` run a "step away from the
  keyboard" operation, not something to fire off mid-edit. `legend/lockContention` fires as usual, so
  a blocked caller is visible rather than mysterious.
- **Cancellation (`legend/cancelTests`)** — `{runId | all:true}`; bridge `POST /cancel-tests`; CLI
  `pure-lsp cancel-tests --run-id X|--all`, and Ctrl-C on a scoped run fires it automatically. Every
  run has an id (generated server-side if the caller omits one) and is registered in `activeRuns`
  for its duration. Three things make it work:
  - **Two levels, because neither suffices.** `FunctionExecutionInterpreted`'s own cancel flag is
    *consume-once* (`compareAndSet(true, false)`), so signalling an executor aborts exactly one
    execution and re-arms — it can stop a test but never a batch. So the run also carries its own
    `cancelled` flag, checked by the walk, which is what actually ends the run; `cancelExecution()`
    on each in-flight executor is what stops work already running, so a slow test needn't be waited
    out. `shutdownNow()` contributes nothing here — the interpreter never polls
    `Thread.interrupted()`, so it only drains the queue.
  - **The handler takes no lock at all.** The run it cancels holds the read lock for its whole
    duration and `graphLock` is fair, so a cancel that waited on any lock could queue behind the very
    run it exists to stop.
  - **Cancellation is cooperative, and only lands at interpreter checkpoints.** The flag is polled by
    `executeFunction` and `findValueSpecificationExecutor` — i.e. between interpreted steps. Time
    spent inside a *single native call* is not interruptible: a test blocked in a backend HTTP round
    trip, in JDBC, or in one big native collection op keeps going until that call returns. Measured:
    a test folding over an interpreted 3M-element sequence aborts in under a second, while
    `range(0, 40000000)` — which spends its time allocating inside Java — ignores the cancel until
    the allocation completes. So cancel is near-instant for ordinary Pure tests and best-effort for
    native-bound ones; it is never a hard kill.
  - **Teardown still runs.** After-hooks are forced through on a cancelled run; skipping them would
    strand what the matching `<<test.BeforePackage>>` created (shared H2 tables) and fail the *next*
    run with "Table ... already exists". A second cancel interrupts teardown too.
  Cancelled runs return normally with `cancelled: true`, `success: false`, and the entries that
  completed. Cancelling an unknown//finished run is not an error.
- **Auto-cancel on last client disconnect.** `LegendPureLspServer.disconnect` cancels every in-flight
  run once `connectedClientCount() == 0` — same gate, and same reasoning, as the existing
  SHARED-debug-session cleanup: lsp4j shares one server instance across connections, so a handler
  cannot cheaply attribute itself to a client, and "nobody is listening at all" is the only test that
  can never kill a run another live client is waiting on. With several clients attached, an abandoned
  run survives until the last leaves — use `cancel-tests --all`.
  **Note which client this is:** the LSP client is the *bridge* (or the IDE), not the `pure-lsp` CLI,
  which only speaks HTTP to the bridge. So killing the CLI does not trigger this path — the CLI is
  covered instead by its own SIGINT handler and by the bridge cancelling on request timeout. This
  gate is what covers the bridge itself dying, and an IDE disconnecting.
- **Streaming:** `legend/testEvent` (on `LegendLanguageClient`, broadcast via `ClientBroadcaster`
  like every other push) carries RUN/SUITE/TEST start+finish events tagged with `runId`, so an IDE
  test tree fills in as the run proceeds. The SUITE pair nests one-to-one with IntelliJ's
  `testSuiteStarted`/`testSuiteFinished` service messages.
- `<<test.ToFix>>` is reported as `skipped` and excluded from the pass/fail tallies.

## Unloading a pushed file (orphan `go()` overlays)

The warm session keeps every pushed file as an overlay; deleting the file on disk does NOT clear it,
so a stale `go()` collides with a new one (`go__Any_MANY_ is defined more than once`). Bridge
`POST /delete {"uri": ...}` → LSP `@JsonRequest("legend/deleteFile")` removes it from open-docs +
runtime + overlay and recompiles (works for currently-open docs, unlike a `didChangeWatchedFiles`
Deleted event). Prefer reusing a single canonical scratch file (e.g. the repo-root `welcome.pure`)
so you rarely need delete.

## Auto-sync hook

The agent's Write/Edit/rm don't emit IDE-style file-change events, so the plugin's PostToolUse hook
(`pure-lsp-sync-hook`, matcher `Write|Edit|MultiEdit|Bash`) syncs them: `.pure` create/modify →
`POST /check`; `rm *.pure` → `POST /delete`. It is OPTIONAL — gated on `/status` state in
(ready, degraded), a silent exit-0 no-op when the daemon is down or not ready (readiness, not just
liveness, so a mid-boot push can't block the tool call). Port via `PURE_LSP_PORT`, else sidecar discovery, else 8991 (`pure_lsp_bridge.discovery`).

## Launching by hand (when the launchers don't fit)

`pure-lsp-launch <project>` (and its `pure-lsp-launch-engine` wrapper) is the normal way in. Build
the `pure-lsp-server` command yourself only for a scope the registry doesn't describe — most often a
**plain** legend-pure-only session, which has no Maven classpath at all.

**Never pipe the launch command.** `pure-lsp-server ... | tail -20` waits for the whole pipeline —
i.e. for the life of the server — so the tool call just hangs. It looks identical to forgetting the
`&`. Background first, read the log in a separate call:

```bash
nohup pure-lsp-server --repo-root ... --port 8991 > /tmp/pure-lsp-bridge.log 2>&1 &
disown
```

**Plain mode** (legend-pure only, ~9s boot, ~2200 symbols, 8 workspace repos). No classpath to
extract a server jar from, so both come straight off disk. No backend/plan-gen path exists here:

```bash
pure-lsp-server \
  --repo-root "$LEGEND_PURE_ROOT" \
  --server-jar "$LEGEND_PURE_ROOT"/legend-pure-lsp/legend-pure-lsp-server/target/legend-pure-lsp-server-*.jar \
  --dependency-classpath-file "$LEGEND_PURE_ROOT"/legend-pure-lsp/legend-pure-lsp-server/target/dependency \
  --port 8991
```

**Engine-scale** (~3 min boot, ~38000 symbols) resolves the server jar out of the project's own
classpath — the same `resolve_lsp_server_jar` in `pure-lsp-common` the launchers use:

```bash
SERVER_JAR="$(tr ':' '\n' < "$(pure-lsp-classpath <project>)" \
  | grep -E '/legend-pure-lsp-server-[^/]*\.jar$' \
  | grep -Ev -- '-sources\.jar$|-javadoc\.jar$|-shaded\.jar$' | sort -V | tail -1)"
pure-lsp-server $(pure-lsp-roots <project>) \
  --server-jar "$SERVER_JAR" --classpath-file "$(pure-lsp-classpath <project>)" \
  --port 8991 --socket-port 9100
```

Add the backend wiring (`--jvm-arg=-Dlegend.test.server.host=... port=... clientVersion=vX_X_X
serverVersion=v1 serializationKind=json h2.port=9092`) to route `execute()`/`go()` through real
plan-gen; that choice is baked in at launch and cannot be added to a running daemon. If `<project>`'s
own classpath doesn't carry the server jar (e.g. a project added via `PURE_DEV_EXTRA_PROJECTS_JSON`
with no `pure-ide-light` module of its own), take `SERVER_JAR` from `pure-lsp-classpath
legend-engine` and add `--dependency-classpath-file "$(pure-lsp-classpath legend-engine)"
--prefer-server-pure-jars`.

**Server-jar resolution order** (`resolve_lsp_server_jar`): `$LEGEND_PURE_LSP_SERVER_JAR` if set
(fails fast if the file is missing, so a broken pin is visible) → the project's own classpath →
borrowed from legend-engine's. Set `$LEGEND_PURE_LSP_SERVER_JAR` to pin an exact jar — a
pre-downloaded build, or a freshly `package`d `target/legend-pure-lsp-server-*.jar` while
editing the LSP server's own Java, which is picked up without reinstalling to `.m2`.

If `.m2` has no `legend-pure-lsp-server` at all, build it once — **the only `-am` build in this
dev loop; it is slow and it is not a default, so confirm with the user before running it**:

```bash
mvn -pl legend-pure-lsp/legend-pure-lsp-server -am install -DskipTests   # from $LEGEND_PURE_ROOT
```

`install`, not `package`: classpath resolution reads `.m2`. Needed once per checkout, or after
legend-pure's LSP source itself changes. Nothing else in this plugin runs `-am`.

## Durability: socket-daemon mode + double-fork

Two transports:
- **stdio (default):** the JVM is a piped child; it dies when the launcher exits (stdin pipe EOF) or
  the task is stopped. Not durable.
- **socket (durable, recommended):** `pure-lsp-server --socket-port <N>` runs the JVM as a detached
  daemon on a loopback TCP socket that the bridge connects to. Nothing owns it via a pipe, so it
  survives the bridge being killed; a new bridge on the same `--socket-port` RECONNECTS to the warm
  session instantly (skips the ~3-min boot). `/health` shows `transport:socket, pid:null`.

Durability requires a **double-fork** so the JVM reparents to init (PPID=1) and leaves the launcher's
process subtree — `start_new_session` alone doesn't survive a task-subtree kill. `_init_socket`
spawns via `setsid --fork` (Python `os.fork` double-fork fallback). Files: server
`LegendPureLspServer.resolveSocketPort/runSocketMode`; bridge `protocol.py` `LspClient.spawn/connect`,
`bridge.py` `Bridge._init_socket`, `server_cli.py --socket-port`.

## Code-storage model + multi-root (source vs jar)

The LSP builds a `CompositeCodeStorage`: workspace repos (RepositoryScanner walks each `--repo-root`
for `*/src/main/resources/*.definition.json`) + `ClassLoaderCodeStorage` (reads `.pure` from the
classpath jars) + the in-memory overlay. **Precedence:** a repo present in a workspace root loads
from SOURCE and overrides the same-named jar repo. `--repo-root` is REPEATABLE; `pure-lsp-server`
itself has no built-in default (`pure-lsp-roots <project>` supplies one `--repo-root` per layer in
the full dependency chain by default - see `pure-lsp-classpath`). Passing only `--repo-root
$LEGEND_PURE_ROOT` by hand means legend-engine `.pure` comes from jars (edits seen only via overlay
push/sync-hook; jar-baked core files need a jar rebuild + restart). Add `--repo-root <legend-engine>`
to make ~134 legend-engine repos SOURCE-backed (on-disk edits compiled directly, no overlay
gymnastics; repositoryCount ~8→142, symbolCount ~38k→40k, boot ~+10s). Recommended for
legend-engine Pure→SQL work.

## recovery vs restart (why a bad edit can need a restart)

Every mutation is transactional: snapshot → edit → compile; on failure, roll back to the snapshot.
If the rollback succeeds you get a normal **diagnostic** and the runtime stays `ready`. If the
rollback FAILS (`isInternalError`), the server triggers **recovery** (a full from-scratch recompile).
Recovery re-reads the offending file from disk — so if that file is still broken, recovery fails too,
and after `MAX_RECOVERY_ATTEMPTS` (3) the runtime latches to `FAILED` ("Manual restart required").
`FAILED` is terminal: a pushed fix is ignored; only a fresh `initialize` (restart) clears it.

Practical consequence: editing EXISTING compiled `.pure` files is reliable (rollback works, stays
`ready`). But adding NEW stores/Databases (new repository-scope elements) is a structural change the
overlay `check` validates unreliably and whose failure can latch `FAILED` — validate those with a
clean daemon boot, not just `check`.

## Gotchas

- Changing the LSP Java (e.g. the setOption/deleteFile handlers) needs a rebuild of
  `legend-pure-lsp-server` and an LSP restart. Bridge Python changes take effect on the next bridge
  start. Pure-only edits need neither (the point of local plan-gen). The launch scripts resolve the
  server jar from `.m2` via the target project's classpath (see `resolve_lsp_server_jar` in
  `pure-lsp-common`), so after a rebuild either `mvn install` (not just `package`) to get the fresh
  jar into `.m2`, or set `$LEGEND_PURE_LSP_SERVER_JAR` to the freshly built
  `target/legend-pure-lsp-server-*.jar` directly to skip the reinstall.
- Don't `curl` the bridge if the environment blocks it; `pure-lsp` (Python urllib) is the reliable
  client.
