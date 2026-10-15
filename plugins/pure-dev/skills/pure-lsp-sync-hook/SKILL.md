---
name: pure-lsp-sync-hook
description: "Explains and manages the PostToolUse hook that auto-syncs .pure file edits/creates/deletes to the running Pure LSP session's overlay, so the Agent's on-disk changes and the LSP's in-memory model stay in a matching state without manual /check or /delete calls - mirroring the file-change events a real IDE sends. Use when the user asks how the LSP-sync hook works, wants to enable/disable/troubleshoot it, sees stale overlay / orphan go() issues, or wants the dev loop to keep disk and the warm LSP session consistent automatically."
---

# PostToolUse hook: auto-sync .pure edits to the LSP session

## What it does

The hook script `pure-lsp-sync-hook` runs after every
`Write`, `Edit`, `MultiEdit`, and `Bash` tool call and keeps the running Pure LSP session's overlay
in sync with what the Agent changed on disk — the thing a real IDE gets for free via file-watcher
events, which the Agent's file tools do NOT emit:

- `.pure` created/modified (Write/Edit/MultiEdit, or a bash content-write) → `POST /check` (pushes
  current on-disk content = a didChange).
- `.pure` deleted (`rm ... file.pure` via Bash) → `POST /delete` (unloads it from open-docs +
  runtime + overlay, so it stops participating in compilation — the fix for orphan
  `go__Any_MANY_ is defined more than once`).

## Key properties (by design)

- **The LSP is OPTIONAL.** The hook first checks the bridge's `/status`; if the bridge is unreachable
  OR its runtime is not `ready`/`degraded` (e.g. mid-boot/recovering), it does NOTHING and exits 0.
  So you can work with no LSP running and nothing happens.
- **Never blocks or annoys.** Always exits 0, prints nothing on success, swallows all errors, and
  uses short timeouts (1.5s readiness probe). A hook failure can never interfere with real work.
- **Readiness, not liveness.** It gates on `/status` state, not `/health` alive — because during the
  ~3-min engine-scale boot the java process is alive but the runtime is `initializing`, and pushing
  to a not-ready runtime would block and add latency to the tool call.

## Configuration

The hook is **declared by the pure-dev plugin** (`hooks/hooks.json`), so it activates automatically
when the plugin is enabled — no manual `settings.json` wiring needed:

```json
"PostToolUse": [
  {
    "matcher": "Write|Edit|MultiEdit|Bash",
    "hooks": [
      { "type": "command", "command": "${CLAUDE_PLUGIN_ROOT}/hooks/pure-dev-sync-hook-launcher" }
    ]
  }
]
```

The launcher resolves the bundled `pure-lsp-sync-hook` (via PATH after preflight, the recorded
plugin bin dir, or its sibling `bin/`) and passes the tool-call payload through on stdin.

- Port: the hook resolves `$PURE_LSP_PORT`, else a running bridge discovered from its
  `/tmp/pure_lsp_server_<port>.json` sidecar, else 8991 — the same resolution `pure-lsp` uses, so
  it follows whichever bridge the launchers actually started. (It used to hardcode 8992, which no
  launcher binds, so it silently no-opped against every real session.)
- To disable: disable the pure-dev plugin, or remove the PostToolUse entry from the plugin's
  `hooks/hooks.json`.
- `install-pure-dev` does **not** also copy this hook into `~/.claude/settings.json` — doing both
  registered it twice and fired it twice per tool call. It only wires settings.json under
  `--link-skills`, for agents with no native plugin system, and removes a duplicate an earlier
  install added.
- The hook is a silent exit-0 no-op whenever the LSP daemon is down or not `ready`, so it never
  blocks a tool call even when you're not using the dev loop.

## Interaction with the dev loop

- You still drive runs via `welcome.pure` (see `references/lsp-devloop-usage-rules.md` for why that one file, and
  `pure-lsp-go` / `pure-lsp-execute` for driving it). The hook
  just means you no longer have to remember to `check`/`delete` files you touch — editing
  `welcome.pure` or any `.pure` auto-syncs it. You can still pass files explicitly to `go` for an
  atomic multi-file compile+run; the hook is complementary, not a replacement.
- Bash content-writes to `.pure` (echo/cat/sed/cp/mv) are best-effort: the hook resyncs a `.pure`
  path it sees in a non-`rm` command if that path now exists. Prefer Write/Edit for reliable sync.

## Troubleshooting

- "My edit didn't reach the session": confirm the LSP is `ready` (`pure-lsp status`); the hook is a
  no-op unless ready. Confirm the file ends in `.pure`. Confirm the port matches `PURE_LSP_PORT`.
- To watch it work, temporarily add a debug line writing `payload.get('tool_name')` to a log file at
  the top of `main()`; remove it after.
- Note: the duplicate-`go()` diagnostic attaches to whichever file is checked, so after a push the
  collision may be reported on the newly-pushed file, not on `welcome.pure` — both being flagged
  means the file is in the session.
