# SQL e2e interpreted-mode dev loop

Runs slices of the SQL e2e parity corpus (`TestPostgresParity`, ~2,300 cases) through the
**interpreted** Pure LSP so `.pure` edits are live, instead of a 15-25 min rebuild per iteration.
Not a CI signal — the full suite stays on compiled `TestPostgresParity`.

> For how to run it and the day-to-day traps, see
> [`legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/KNOWN-GAPS.md`](../../../legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/KNOWN-GAPS.md)
> and the `sql-e2e-devloop` skill. This page covers the harness architecture and the technical
> detail behind two of those traps.

## Architecture

- `legend-engine-xt-sql-e2e-tests` — `SqlE2ERunner`: lazily starts a Testcontainers Postgres, seeds
  the schema, loads the YAML corpus, and resolves each `(case id, path)` pair (FROM-rewrite plus
  the cached reference result) into a typed `CaseRef`. No JSON serialisation anywhere in this path.
- `legend-engine-xt-sql-e2e-pure` — Pure repo `core_external_query_sql_e2e`: `model.pure` (the test
  schema and mapping) and `framework.pure` (the driver), backed by three natives —
  `resolveCaseRefs`, `executeSQLE2ETest`, `executeAdhocSQL` — with compiled and interpreted
  implementations in `SqlE2ENativeHelper`/`SqlE2E{Compiled,Interpreted}Extension`.
- Result comparison reuses the same typed `ResultComparator`/`ResultMatrix` the compiled suite
  uses (via `TdsJsonResultMatrix` on the interpreted side) — one comparator implementation, not two.

See [ADR-004](../decisions/ADR-004-sql-e2e-test-execution-natives.md) for the full native-surface
design and its rationale.

## Interpreter defects behind most false divergences

Two confirmed Pure-interpreter-only bugs explain most interpreted-vs-compiled disagreement:

- **`pair` type-variable capture.** `pair<U,V>` over a polymorphic function reference that also
  names a parameter `U` drops that function's own quantifier. `quantifiedComparisonFunction`/
  `scalarComparisonFunction` (`fromPure.pure`) build exactly this shape via
  `pair(op, equalAny_U_...)`, breaking every quantified/scalar subquery (`= ANY`, `> ALL`,
  `IN (subquery)`) with `Type Error: 'ConcreteFunctionDefinition<<U,Z> {...}>' not a subtype of
  'SqlTransformContext'` at `pair.pure:41`. Compiled is immune — the generated Java uses wildcards
  and does no runtime generic resolution. `->cast(@meta::pure::metamodel::function::Function<Any>)`
  on the function reference pins the type variable and fixes it (a no-op for compiled).
- **`reactivate` does not constant-fold interpreted.** `LIMIT n OFFSET m` emits
  `slice(rel, m, m + n)` and relies on pre-evaluation (`preeval.pure:859`,
  `$newSfe->reactivate($state.inScopeVars)`) to fold it before `pureToSQLQuery.pure:3854`'s
  `instanceOf(Literal)` assert. `reactivate` has two independent native implementations: compiled
  folds `plus(2,1)` → `3`; interpreted passes the raw `InstanceValue` straight to `plus`, surfacing
  as `"Not a number: ... instanceOf InstanceValue"` (TDS) or `"Invalid type for second parameter
  inside the slice function"` (Relation). `ORDER BY ... NULLS FIRST/LAST` fails in the same
  subsystem (`preeval.pure:238`). (The reported error location, `compileUtils.pure:102-110`, is not
  the failing code — it's the `SourceInformation` stamped on the `plus` node when it was built.)

Neither of these is a Relation-only bug, even though it can look that way: `structural/subqueries`'
quantified/scalar comparisons and `LIMIT`/`OFFSET` both hit `assertRelation(...)` on the TDS path by
design (`processQuantifiedComparisonExpression`/`processScalarSubqueryComparison`,
`fromPure.pure:3500`/`:3562`), so TDS bails intentionally and is baselined `ERROR` — "TDS passes"
there means "TDS errors as intended", not "the bug is Relation-specific".

A wrong-answer (`FAIL`) result is no more trustworthy than an `ERROR` one: the interpreter can
silently compute a different *value* rather than only failing loudly — `NULL = NULL` evaluates to
`true` interpreted and to `NULL` compiled, which is why `null_equals_null`, `null_not_equals`,
`null_is_distinct_from` and `case_simple_null_compare` all look like three-valued-logic bugs
interpreted while passing both paths compiled. Confirm every divergence against a compiled
`-Dtest.filter` run before treating it as real, in either direction.

## Corpus dispatch: table-free queries bypass Legend entirely

`StatementDispatcherVisitor.visitDefault` routes any statement whose `TableNameExtractor` finds no
qualified table name to `ExecutionType.Metadata_Generic`, served by a real Postgres metadata
connection (`SQLManager`'s `postgres-metadata-server`), not Legend. This is correct production
behaviour — a BI client's `SELECT 1` shouldn't invoke the SQL planner — but it means a corpus case
with no `FROM` tests nothing: Postgres answers it and it's compared against Postgres.

Every corpus case must therefore reference a real table or table function (a derived table does not
count) or it silently passes while exercising nothing. Six cases are deliberately left table-free:
`functions/sequence_functions` ×3 (`NEXTVAL`/`SETVAL` are side-effecting and both sides share one
database, so they can never legitimately match), `smoke_tests` ×2 (dedicated coverage of this
dispatch path itself), and `structural/values_clause` ×1 (a standalone `VALUES` has nothing to
attach a `FROM` clause to).

## Format-token and extract-field support (`fromPure.pure`)

`to_char` implements exactly `YYYY YYY YY Y MM MONTH/MON(+case) DD DDD D DAY/DY(+case) HH24 MI SS
WW Q` (`toCharFormats()`); every other token is explicitly `toCharUnsupported`. `EXTRACT` implements
exactly `YEAR QUARTER MONTH WEEK DOW DOY DAY HOUR MINUTE SECOND EPOCH` (`extractFieldToValue()`).
Corpus cases outside these sets are correctly baselined `ERROR` — that's Legend's real supported
surface, not a harness gap.
