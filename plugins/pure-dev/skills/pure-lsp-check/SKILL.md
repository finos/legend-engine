---
name: pure-lsp-check
description: "Compiles/checks a single .pure file against the already-running Legend Pure LSP bridge and reports diagnostics (errors/warnings) in under a second, as a fast alternative to a full `mvn test`/`mvn compile` cycle while iterating on Pure source in legend-pure. Use whenever the user is editing .pure files and wants to know if they compile, asks 'does this Pure file compile', 'check this .pure file', 'validate this Pure code', or wants fast type/syntax feedback without running the full Maven build."
---

# Compile-check a .pure file via the LSP bridge

Requires a running bridge (`pure-lsp health`) — and never `curl` it, `pure-lsp` is the only
reliable client. Both rules, and the port-discovery behaviour, are in `references/lsp-devloop-usage-rules.md`.

## Usage

```bash
pure-lsp check <path/to/file.pure>
```

- Pass `-` instead of a path to check content from stdin.
- Add `--port <N>` (or set `PURE_LSP_PORT`) to target a specific bridge. Without it, `pure-lsp`
  finds a running bridge automatically (via the `/tmp/pure_lsp_server_<port>.json` sidecar each
  bridge writes), falling back to 8991.
- Add `--uri <uri>` to override the LSP document URI if you need a specific one (rarely needed —
  the default `file://<absolute path>` is fine even for scratch files that don't map to a real
  workspace repo).

The command exits 1 if any diagnostic has `severity: 1` (error), 0 if clean — usable directly in
a shell conditional. Output looks like:

```json
{
  "uri": "file:///tmp/Example.pure",
  "diagnostics": [
    {
      "range": {"start": {"line": 2, "character": 5}, "end": {"line": 2, "character": 20}},
      "severity": 1,
      "message": "TotallyNotAType has not been defined!"
    }
  ]
}
```

An empty `diagnostics` array means it compiled cleanly.

## Checking several files that reference each other: use check-many, not a loop of check

If you're validating a batch of changed files and any of them reference types/functions defined
in another file in the same batch, don't just call `check` on each one in a loop — plain `check`
goes through `textDocument/didOpen`/`didChange`, which the LSP compiles immediately and
independently as each one arrives, with no awareness of files pushed moments later. A file
checked before its dependency is pushed will falsely report "X has not been defined."

```bash
pure-lsp check-many FileA.pure FileB.pure FileC.pure
```

`check-many` instead calls the server's `legend/checkBatch`, which applies every file's content
to the runtime and compiles **once**, atomically — not per file, and not client-side pushed twice
to route around ordering. Argument order genuinely doesn't matter: the compiler sees the whole
batch at once. Response shape:

```json
{"success": true, "modifiedFiles": ["file:///.../FileA.pure", "..."]}
```
or, on failure:
```json
{"success": false, "error": "...", "errorUri": "file:///.../FileC.pure", "errorDiagnostics": [...]}
```

The trade-off for the efficiency of a single compile: on success, every file in the batch (and
anything else transitively affected) is guaranteed clean — `modifiedFiles` lists all of them. On
failure, the **whole batch is rolled back** (nothing was actually applied) and you get exactly
**one** error, attributed to its real file via `errorUri` (with full structured `errorDiagnostics`
— range/severity/code — for that one file), not a per-file breakdown across everything you
passed. If you need to know about every broken file in a batch rather than just the first one the
compiler hits, fix the reported one and re-run `check-many` to find the next. Exits 1 on failure.

## Things worth knowing

- The **first** `check` call on a given URI is a `didOpen`; every call after that on the same URI
  is a `didChange` (the LSP protocol requires this distinction) — `pure-lsp` handles the
  transition automatically, you don't need to track which call number you're on.
- This only compiles the file(s) you've pushed via `check` (plus whatever else is already loaded
  in the session) — it does not run anything. To actually execute a function, follow up with the
  `pure-lsp-go` skill.
- **A `check`/`check-many` target must be a real file under one of the bridge's configured repo
  roots** (or fed as literal content via `check -`). A `.pure` file that exists on disk somewhere
  else — e.g. a scratch file under `/tmp` — is silently ignored by the LSP (it isn't part of any
  registered module), so you get back `{"diagnostics": []}` (looks clean) with exit 0 **even for
  code that doesn't compile at all** — nothing was actually checked. For ad hoc scratch content
  that isn't part of a real module, pipe it through stdin (`check -`) instead of writing it to a
  path outside the workspace — stdin has no on-disk identity, so the LSP compiles it for real as a
  scratch source.
- If diagnostics mention a type/class from elsewhere in the codebase that "has not been defined",
  double check it isn't just missing from what's been pushed into this particular session yet —
  the bridge only knows about files under its configured workspace root(s)/classpath plus whatever
  you've explicitly `check`ed.
- **Never pass a `.legend` file to `check`/`check-many`.** `.legend` files are plain-text fixture
  resources loaded at runtime via the Pure `readFile(...)` function inside a test — they are not
  Pure compilation units the LSP indexes. Passing one throws `"Invalid source id"` and knocks the
  whole runtime into a `recovering` state (see `pure-lsp-status`'s troubleshooting note). Only pass
  `.pure` source files here; verify a `.legend` fixture indirectly by running the test/`go()`
  function that reads it via `pure-lsp-go`.
