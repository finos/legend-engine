---
name: pure-lsp-status
description: "Checks whether the Legend Pure LSP dev-loop bridge is running and its current state — initializing/ready/failed, live compiler progress text, symbol/repository counts. Use whenever the user asks if the Pure LSP or pure-lsp bridge is up/running/ready, what it's currently doing, why it seems stuck, or wants to confirm it's ready before using pure-lsp-check or pure-lsp-go."
---

# Check the Pure LSP bridge's status

Never `curl` the bridge — `pure-lsp` is the only reliable client (see `references/lsp-devloop-usage-rules.md`).

## Usage

```bash
pure-lsp health   # is the process even alive?
pure-lsp status   # what is it actually doing?
```

Add `--port <N>` / `PURE_LSP_PORT` to target a specific bridge; otherwise `pure-lsp` discovers
a running one automatically (sidecar lookup, falling back to 8991). `pure-lsp status --wait <N>`
polls up to N seconds and returns as soon as it reaches a terminal state
(`ready`/`degraded`/`failed`) — useful right after starting it instead of guessing how long to
sleep.

If `health` fails to connect at all, nothing is running — use `pure-lsp-launch-engine` rather than
trying to interpret a connection error as a Pure-side problem.

## Reading the status JSON

```json
{
  "state": "ready",
  "repositoryCount": 143,
  "symbolCount": 41788,
  "message": "Ready in 143973ms",
  "recoveryAttempts": 0,
  "recoveryInProgress": false,
  "compiledRepositories": 0,
  "totalRepositories": 0,
  "connectedClientCount": 1,
  "port": 9100,
  "transport": "socket",
  "requestPoolSize": 12,
  "executionConcurrency": 6,
  "repoRoots": ["/home/developer/projects/finos-legend-pure", "/home/developer/projects/finos-legend-engine"],
  "jvmArgs": ["-Dlegend.test.server.host=127.0.0.1", "-Dlegend.test.server.port=9095"],
  "recentErrors": [],
  "lockContended": false,
  "recentDisconnectCount": 0
}
```

The bridge passes the server's `legend/status` payload through verbatim.

- `state`: `created` → `initializing` → `ready`, or `failed`/`degraded` on the way. `reindexing`
  and `recovering` are transient states after a workspace file change or a crash-recovery attempt.
- `message`: live, human-readable compiler progress (e.g. `"Loading 2854 sources..."`,
  `"Finished compiling platform_dsl_mapping in 00:00:03"`) while initializing — or the real
  failure reason (e.g. a `NoClassDefFoundError`) if `state` is `failed`. It updating between polls
  means it's making progress, not hanging.
- `repositoryCount`/`symbolCount`: only count **workspace** repos (discovered via
  `*.definition.json` under `--repo-root`) and indexed symbols. A `--classpath-file` (engine-scale
  mode) adds a lot to `symbolCount` once ready but does **not** change `repositoryCount` — that
  stat is workspace-only, so seeing it "stuck" at a small number in engine-scale mode is expected,
  not a bug.
- `compiledRepositories`/`totalRepositories`: only populate during a genuine from-source compile
  pass (the `"Compiling repositories in the following order: [...]"` phase). Sitting at `0/0` the
  rest of the time — including while `ready` — is normal, not a sign anything is broken.
- **`port` is the LSP daemon's SOCKET port, not the bridge's HTTP port.** In socket mode this is
  the `--socket-port` value (9100 above), so it will not match the `--port` you pass to `pure-lsp`.
  Use `pure-lsp health` for the HTTP port.
- `executionConcurrency` is how many Pure executions run at once — the real ceiling for
  `pure-lsp execute-parallel`, and the number to raise/lower (`-Dlegend.lsp.executionConcurrency`)
  rather than hand-chunking batches. Default `0.75 x cores`: execution is CPU-bound, so more
  threads than that cost throughput instead of adding it.
- `requestPoolSize` is the pool for *every other* request (`status`, `check`, hover, …). It is
  deliberately larger and is NOT the execution ceiling — executions have their own pool precisely
  so a big batch cannot occupy every request thread and make the daemon look wedged.
- `recoveryAttempts`/`recoveryInProgress` track crash-recovery; `MAX_RECOVERY_ATTEMPTS` is 3, after
  which the runtime latches to `failed` and only a restart clears it (see `pure-lsp-restart` below).
- `recentErrors` is a list of `{"timestamp": <epoch-ms>, "message": "..."}`.
- `lockContended` reflects whether the LSP's internal read/write lock is currently contended (e.g. a
  `check`/`go()` blocked behind a long-running compile). The richer detail arrives as an async
  `legend/lockContention` notification (`{active, lockType, reason, pendingCount}`) pushed while
  contention is ongoing — it is not a `/status` field, so don't poll for one.

## How long to expect

- Plain repo-only mode: ready in roughly 10 seconds.
- Engine-scale mode (`--classpath-file`): roughly 3 minutes, and it genuinely burns CPU the whole
  time (confirmed via `ps`/`jstack` — a healthy compile shows real, sustained CPU usage, not near-
  zero). If `status` is stuck on the exact same `message` for several minutes *and* the java
  process's CPU usage is near zero, that's a real hang, not slowness — check with:
  ```bash
  ps -eo pid,cmd | grep LegendPureLspServer | grep -v grep
  ps -p <pid> -o pid,etimes,time,%cpu
  ```
  A hang like this is what motivated adding real failure reporting to the server — if it's
  actually broken (not just slow), `state` should transition to `failed` with a specific message
  rather than sitting silently in `initializing` forever.

## `recovering` or `failed`: restart the daemon, don't just poll

A bad request (e.g. checking a non-`.pure` file, see `pure-lsp-check`'s note on `.legend` files)
can knock the runtime into `recovering`. Recovery can silently fail — `status` may keep reporting
`recovering` while the runtime is actually dead, or flip to `failed`, and either way the next
`pure-lsp-go` call errors with `"Runtime not initialized"`. Don't wait it out — restart:

```bash
pure-lsp-restart [--socket-port N] [--dry-run]
```

This finds the java daemon and its bridge for `--socket-port` (default **9100**), captures the
bridge's exact original argv from `/proc/<pid>/cmdline` before touching anything, kills the daemon
(then the now-stale bridge), relaunches the bridge with that captured argv, and polls it until the
runtime reports `ready`/`failed` or a timeout. `--dry-run` prints the same discovery (pids, captured
argv, what would be killed/relaunched) without killing or relaunching anything — use it to sanity-
check before touching a shared daemon.

Exit codes: `0` restarted and ready (or, under `--dry-run`, a valid plan was found); `2` nothing is
listening on `--socket-port` — nothing to restart; `3` a bridge was found but its daemon's pid
couldn't be determined — aborts without killing anything; `4` a daemon is listening but no bridge is
attached to it, so there's no captured argv to relaunch with (use `pure-lsp-connect` instead); `5`
the daemon/bridge were killed and relaunched but never reached `ready` within the timeout (see
stderr and the bridge log). The backend Server (`EngineServerForTest`, if one is wired in) is a
separate process and normally survives this; no need to restart it too.
