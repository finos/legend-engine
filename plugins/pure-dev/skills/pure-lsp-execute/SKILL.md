---
name: pure-lsp-execute
description: "Runs exactly ONE existing Pure function by path (signature, mangled id or bare path) through the running Legend Pure LSP bridge, without a go() wrapper, and returns its typed value in returnValue. Also runs every test in a package or .pure file serially with --package/--source. For 2 or more functions or tests, use pure-lsp-execute-parallel instead, never a shell loop of this command. Use when the user wants to run a specific function, a single <<test.Test>> or <<PCT.test>>, or the result of a Pure function."
---

# Execute one function via the LSP bridge

Needs a running bridge (`pure-lsp health`). Use only the `pure-lsp` CLI, never `curl`; see `references/lsp-devloop-usage-rules.md`.

**More than one function or test? Use `pure-lsp-execute-parallel`.** Never loop this command in a shell `for`. Keep serial runs for tests that write to the shared H2 backend. For a quick snippet you are editing in `welcome.pure`, use `pure-lsp-go`.

```bash
pure-lsp execute 'my::pkg::testFoo():Boolean[1]'
```

`function` is a signature, a mangled id (`my::pkg::testFoo__Boolean_1_`) or a bare path (zero-arg shapes only).

## Result

`output` is only what the function printed. `returnValue` holds the function's value, so primitives, enums and collections of them need no `print()`. Also returned: `returnKind` (`primitive|enum|collection|empty|complex`), `returnType`, `returnSize`, `returnTruncated` (over 1000 elements). A `complex` return (Class instance, Relation, lambda) has a null value; `print()` what you need. `String[1]` returns a scalar, `String[*]` an array even with one element.

## Options

- Edited files compile first as one atomic batch, and the function runs only if it succeeds: `pure-lsp execute 'my::pkg::t():Boolean[1]' Main.pure Helper.pure`
- `String[1]` parameters: `--arg a --arg b`, in order; name a signature or mangled id. String is the only supported parameter type; take other things as a String and resolve in Pure (an enum via `extractEnumValue`). One parameterised function over many argument sets: the `invocations` payload in `pure-lsp-execute-parallel`, as `pure-sql-e2e` does.
- Setup/teardown around the function: `--before my::pkg::setUp --after my::pkg::tearDown`. `<<test.BeforePackage>>` hooks do not run on a direct execute, so grep the test's file for the hook and pass it as `--before`.

## PCT tests

A `<<PCT.test>>` takes the adapter function, so it needs its mangled id and `--pct-adapter`. The id is always `<fqn>_Function_1__Boolean_1_`:

```bash
pure-lsp execute 'meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_' --pct-adapter DuckDB
```

`--pct-adapter` accepts the adapter's path, simple name or display name; paths are in `references/pct-adapters.md`. Never run `--source`/`--package` just to find one test's id; that executes every test in the scope. For a whole package's PCT tests use `--package <pkg> --pct-only --pct-adapter <path>`; no ids needed.

## Whole package or file

```bash
pure-lsp execute --package meta::pure::functions::collection::tests
pure-lsp execute --source path/to/MyTests.pure
```

Flags, hook bracketing and the 30-test cap: `references/test-scopes.md`.

## Notes

- Compiles and runs against the whole loaded session, so an unrelated compile error fails this call too.
- Runs interpreted unless the backend server is wired in; see `pure-lsp-go` "Execution routing".
- On failure read `output` and `error` in full; the useful part is not always at the top.
