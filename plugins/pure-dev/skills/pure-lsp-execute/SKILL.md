---
name: pure-lsp-execute
description: "Executes an arbitrary zero-arg Pure function BY PATH through the running Legend Pure LSP bridge - not just the one designated go() entry point. Also runs EVERY test in a package or a .pure file serially with `--package`/`--source`, discovering them server-side (vanilla <<test.Test>> and <<PCT.test>> alike) with correct per-subpackage setup/teardown. Use when the user wants to run/execute a specific existing function (e.g. an already-written <<test.Test>> or <<PCT.test>>) without rewriting it as a go() wrapper, wants to run a function by its Pure path/signature, wants to run 'all the tests in' a package or file, or asks to execute something 'directly' / 'by name' rather than through go(). Returns the function's own return value, typed, in returnValue (plus returnKind/returnType/returnSize) — a function returning a primitive, enum, or collection of those does NOT need a print() wrapper to make its result visible; only complex returns (a Class instance, Relation, lambda) report type-and-size with a null value and still need print(). Use it too when the user wants the RESULT of a Pure function rather than its console output. Complements pure-lsp-go (single fixed go():Any[*] entry point) and pure-lsp-execute-parallel (same scopes, run concurrently, and the way to batch many value-returning functions)."
---

# Execute an arbitrary zero-arg function via the LSP bridge

Requires a running bridge (`pure-lsp health`) — and never `curl` it, `pure-lsp` is the only
reliable client. See `references/lsp-devloop-usage-rules.md` for those and the shared conventions.

## The mental model

`pure-lsp-go` only ever runs one designated function literally named `go`. This skill runs **any**
existing zero-argument function directly, by its Pure path — no `go()` wrapper needed. Use it when
the function you want to run already exists (an existing `<<test.Test>>`/`<<PCT.test>>`, or any
other zero-arg function) and rewriting it as `go()`'s body would just be indirection.

```bash
pure-lsp execute 'my::pkg::testFoo():Boolean[1]'
```

`function` accepts:
- a full signature — `my::pkg::testFoo():Boolean[1]`
- a mangled id — `my::pkg::testFoo__Boolean_1_`
- a bare path — `my::pkg::testFoo` (common zero-arg return shapes are tried)

Returns the same shape as `go`: `{"success": bool, "error": str|null, "output": str}` plus the
return-value fields below. `output` is still only what the function printed via `print(...)`.

## You do not need `print()` for a primitive result

The function's own return value comes back in `returnValue`, so a function that returns a value no
longer needs a `println` wrapper just to let you see it:

```bash
pure-lsp execute 'my::pkg::buildSql():String[1]'
```
```json
{
  "success": true,
  "output": "(my::pkg::buildSql():String[1] returned successfully with no console output. See returnValue.)",
  "returnKind": "primitive",
  "returnType": "String",
  "returnValue": "select \"root\".ID from myTable as \"root\"",
  "returnSize": 1,
  "returnTruncated": false
}
```

| Field | Meaning |
|---|---|
| `returnKind` | `primitive` \| `enum` \| `collection` \| `empty` \| `complex` |
| `returnType` | `String`, `Integer`, …; full path for enums and classes |
| `returnValue` | scalar for `primitive`/`enum`, array for `collection`, `null` for `empty`/`complex` |
| `returnSize` | true element count, before truncation |
| `returnTruncated` | `true` when more than 1000 elements were produced |

**`complex` is the one case that still needs `print()`.** A Class instance, Relation or lambda
reports its type and size with a `null` value — the server deliberately does not walk an arbitrary
instance graph. Print what you need from it instead.

`String[1]` comes back as a scalar; `String[*]` comes back as an array even when it holds one
element, because the multiplicity is part of the answer.

## Functions taking `String[1]` parameters

`--arg` binds one String literal per parameter, in order; repeat it per parameter. String is the
only supported parameter type — a function wanting anything else takes its name/text as a String
and resolves it in Pure (e.g. an enum via `extractEnumValue`). A bare path only resolves zero-arg
shapes, so name a signature or mangled id here:

```bash
pure-lsp execute 'my::pkg::greet_String_1__String_1__Any_MANY_' --arg alpha --arg beta
```

For running one parameterised function over many argument sets concurrently, see
`pure-lsp-execute-parallel`'s `invocations` payload — or, for the SQL e2e corpus specifically, the
`pure-lsp-sql-e2e` skill's `pure-sql-e2e`.

## Compiling edits first (atomic, like `go`)

Pass any edited `.pure` files before the function name — they're compiled as one atomic batch, and
the function only runs if that batch succeeds (same all-or-nothing semantics as `pure-lsp go`):

```bash
pure-lsp execute 'my::pkg::testFoo():Boolean[1]' Main.pure Helper.pure
```

## PCT tests and before/after hooks

A `<<PCT.test>>` function takes exactly one parameter — the adapter `Function` itself — so it can't
be run as a plain zero-arg call, and (being generic) it also can't be resolved by bare path or a
hand-written signature the way an ordinary zero-arg function can — **you must give the target
function's mangled id** (the `function` param's "mangled id" form from the section above). Get that
mangled id from `pure-lsp execute --package <pkg> --pct-only --json` (each discovered test's raw
path) or from `legend/testFunctions`/`TestFunctionInfo#getFunctionPath` if you're driving the
protocol directly — don't hand-guess Pure's name-mangling scheme.

The **adapter** argument is more forgiving — `--pct-adapter` takes the mangled id, the bare path,
the simple name or the display name, all tried server-side. Paths are pre-dumped in
`references/pct-adapters.md`; see `references/test-scopes.md` for the rest.

```bash
pure-lsp execute 'my::pkg::somePctTest_Function_1__Boolean_1_' --pct-adapter 'DuckDB'
```

For a whole package's PCT tests, prefer `--package ... --pct-only --pct-adapter <path>` (next
section) over resolving one function's mangled id by hand — the daemon discovers the tests and
needs no mangled id from you at all.

To run setup/teardown around the target function in the same atomic call (e.g. a `createTablesAndFillDb()`-style
setup that `<<test.BeforePackage>>` doesn't trigger under a direct execute, same as under `go()`),
use `--before`/`--after` with the Pure path of a zero-arg function each:

```bash
pure-lsp execute 'my::pkg::testFoo():Boolean[1]' --before 'my::pkg::setUp' --after 'my::pkg::tearDown'
```

## Running every test in a package or a file

Instead of a function path, name a scope and the daemon discovers the tests itself:

```bash
pure-lsp execute --package meta::pure::functions::collection::tests
pure-lsp execute --source path/to/MyTests.pure
```

**`references/test-scopes.md` is the reference for this** — the flags, the nested
`<<test.BeforePackage>>` bracketing, PCT adapters, output shape, the 30-test cap and when to switch
to Maven. It is shared with `pure-lsp-execute-parallel`, which takes the identical flags.

The only thing specific to this subcommand: it runs the scope **serially**. Reach for it over
`execute-parallel` when the tests themselves write to the shared H2 backend.

## When to use this vs. `pure-lsp-go` vs. `pure-lsp-execute-parallel`

- **`pure-lsp-go`** — the one true `go():Any[*]` entry point; best for quick ad-hoc snippets you're
  actively editing in `welcome.pure`.
- **`pure-lsp-execute`** (this skill) — run one already-named function directly, no wrapper; or a
  whole `--package`/`--source` scope, serially.
- **`pure-lsp-execute-parallel`** — run several named functions at once, or the same
  `--package`/`--source` scopes concurrently. Both go to `legend/executeTests`, so you get
  per-entry status/duration and a cancellable run, with the daemon bounding the fan-out. Reach for
  it when you have a batch to fire together and the tests don't write to the shared H2 backend.

## Things worth knowing

- Same session-wide recompile caveat as `go`: it recompiles/executes against the **entire loaded
  session**, so an unrelated file with a compile error will fail this call too, even if the target
  function is fine.
- Runs on the **interpreted** engine unless the backend Server + `legend.test.server.*` jvm-args are
  wired in — see `pure-lsp-go`'s "Execution routing" section, which applies identically here (it
  governs `meta::pure::router::execute`, not anything `go()`-specific).
- On failure, read `output`/`error` in full — the useful part (Pure stack trace or raw Java
  exception) isn't always at the top.
