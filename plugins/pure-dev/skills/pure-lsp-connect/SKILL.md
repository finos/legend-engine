---
name: pure-lsp-connect
description: "Attaches the pure-lsp HTTP bridge to an LSP daemon that is ALREADY running (e.g. started from IntelliJ, or left over from a prior session) - never spawns a new daemon JVM. Defaults to socket port 9100. Use for 'connect to the existing/already-running LSP', 'attach the bridge to the LSP on port X', or 'reconnect to the LSP' - as opposed to pure-lsp-launch-engine, which boots a new session. Also recovers a bridge whose connection has gone stale (health OK, requests fail with a broken pipe)."
---

# Connect the bridge to an already-running LSP daemon

Booting a fresh session and attaching to one already up are different intents, and conflating them
is risky when the user only wants the latter: the wrong branch can end up launching a second
multi-GB JVM alongside one already running. This skill is scoped to exactly one job: get the lightweight Python bridge talking to a daemon that already exists,
and never touch the daemon/JVM itself unless explicitly told to.

## Run the script

All the decision logic (is a daemon listening, is a bridge already attached, is it actually healthy,
recover-vs-relaunch-vs-abort) is implemented deterministically in `bin/pure-lsp-connect` — run it
rather than re-deriving these steps by hand:

```bash
pure-lsp-connect [--socket-port N] [--project NAME] [--bridge-port N] [--java PATH]
```

- `--socket-port` defaults to **9100** (this user's standing convention for `LegendLspServer`,
  configured via `legendLspServerConfig.json`'s `"port"` field).
- `--project` selects which project's repo-roots/classpath to launch with if no bridge is attached
  yet (passed through to `pure-lsp-roots`/`pure-lsp-classpath`); omit it to let those infer from
  `$PURE_DEV_PROJECT` or the current directory.
- `--bridge-port` picks the bridge's own HTTP port; omit it to auto-pick the first free port starting
  at 8991.

On success it prints one JSON line to stdout and exits 0:
```json
{"bridgeHttpPort": 8991, "socketPort": 9100, "state": "ready", "repositoryCount": 143, "symbolCount": 40756}
```
Hand `bridgeHttpPort` off to `pure-lsp-check`/`pure-lsp-go` — both take the bridge's HTTP port, not
the daemon's socket port.

Exit codes:
- `2` — nothing listening on `--socket-port`. **Stop and tell the user** to start the daemon (e.g.
  their IntelliJ "Legend LSP Server" run/debug config), or ask them to run `pure-lsp-launch-engine`
  if they want a brand-new session instead. Do not scan a wide port range speculatively,
  and do not fall back to a boot flow on your own initiative — that's the one judgment call this skill
  still leaves to you, not the script.
- `3` — a bridge is up but didn't reach a healthy/ready state; the script already tried the one known
  automatic recovery (see below) and it didn't work. Read the diagnostic on stderr rather than
  guessing further — it includes the raw `/status` error body, since `pure-lsp status`'s own error
  message hides the response body.
- `4` — safety abort: a launch attempt didn't show a reconnect to the existing daemon in its log
  within the timeout, which would otherwise risk a second multi-GB JVM coming up. The script already
  killed the errant bridge process before returning this code. If you see this, stop and investigate
  (wrong socket port is the most likely cause) rather than retrying blindly.

## What the script automates (context if you need to debug it)

1. Verifies something is actually listening on the socket port (`/dev/tcp` probe) before doing
   anything else — this is the hard boundary that keeps the skill from ever booting a daemon itself.
2. Checks for an existing bridge by confirming every `pgrep` candidate against `/proc`: it must be
   a python process actually running `-m pure_lsp_bridge.server_cli` with the requested
   `--socket-port`. A bare `pgrep -f` is not enough — it matches the invoking shell's own command
   line too (observed doing exactly that), and `pure-lsp-restart` SIGKILLs what this returns. A
   looser pattern like `pure.lsp` would also regex-match the `-` in `legend-pure-lsp-server` inside
   the daemon JVM's classpath and dump 100+KB of noise. Shared helper: `bin/pure-lsp-common`.
3. If found, verifies it's actually healthy (`health` + `status`). If `status` fails, fetches the raw
   error body directly (bypassing the CLI's generic "HTTP Error 500") and checks for a broken-pipe
   shape — the bridge's persistent socket to the daemon died underneath it, commonly because the
   daemon JVM had a debugger attached and sat paused at a breakpoint long enough to time out. Recovery
   is to kill **only the bridge process** (captured exact argv, never the daemon PID) and relaunch it
   with that same command line.
4. If no bridge is attached, resolves repo-roots/classpath/server-jar (via `resolve_lsp_server_jar` in
   `pure-lsp-common` — extracted from the target project's own classpath, or borrowed from
   legend-engine's if the project's own classpath doesn't carry it) and launches
   one against the confirmed-listening socket port, then verifies the launch log actually shows a reconnect
   (`connected to EXISTING LSP daemon`) before declaring success — aborting and killing the process
   otherwise, rather than letting an unexpected fresh boot slide by.
