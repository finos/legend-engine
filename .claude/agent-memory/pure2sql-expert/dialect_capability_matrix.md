---
name: Dialect capability matrix from expectedFailures
description: Recurring "dialect cannot do X" shapes observed in PCT Test_Relational_<Dialect>_*_PCT expected-failure lists. Use as first-order dialect fingerprint for triage
type: reference
---

Patterns lifted from `Test_Relational_<Dialect>_{Essential,Standard,Grammar,Relation,Variant,Unclassified,ScenarioQuant}Functions_PCT.java` expectedFailures as of 2026-04-20. These are observational shapes, not a contract — verify with `git grep` before quoting.

## Cross-dialect recurring patterns

- **Variant/Postgres model gap:** `add`, `concatenate`, `drop`, `get`, `keys`, `put`/`putAll`, `values`, `head`/`init`, `indexOf` often fail on H2 with `"Couldn't find DynaFunction to Postgres model translation for toVariantList()."` or `toVariantObject()`. Origin: `core_relational/relational/sqlDialectTranslation/toPostgresModel.pure:264`. Indicates the new legend-SQL pipeline Postgres-model converter lacks a binding for the variant-building dyna-functions.
- **forAll:** `No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'` is universal — forAll is not wired for SQL at all. Origin: `pureToSQLQuery.pure:2349`.
- **Fold:** Frequently fails with `Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda` or platform-copy errors; fold-in-SQL is fragile.
- **Array aggregates (array_max/array_min/array_append/array_concatenate/array_drop/array_first/array_position):** absent on Postgres (`[unsupported-api] … is not supported yet`) and Oracle. DuckDB and ClickHouse implement them. Origin of the error wording: `dbExtension.pure:1041`.
- **stdDev/variance/covar/corr:** Postgres reports `Unused format args. [N] arguments provided to expression "…(%s, %s)"` — the default formatter mismatches arity. Symptom of a mis-authored `ToSql(format=…)` in extensionDefaults or in the dialect.
- **contains with non-literal:** `"Parameter to IN operation isn't a literal!"` across many dialects — flagged with `AdapterQualifier.unsupportedFeature`.
- **Date max/min on arrays:** Postgres produces `Unused format args. [2] arguments provided to expression "max(%s)"` — same arity mismatch family.
- **DateTime precision divergence:** Postgres returns `2025-02-10T20:10:20.000000000+0000` where expectation is `2025-02-10T20:10:20+0000` — nine-digit fractional seconds vs expected millis. Classify as `assertErrorMismatch`.
- **Integer-vs-Double comparisons:** Postgres `expected: 1.0D actual: 1.0` and `2D` vs `2.0` — literal formatter emits `1.0`/`2.0` where Pure Double formats to `1.0D`.

## Dialect-specific shapes

- **H2:** `JdbcSQLSyntaxErrorException … not comparable` on mixed-type IN; no variant model (Postgres-model-based); semistructured tested in separate `Test_Relational_H2_Semistructured.java` outside PCT framework.
- **Postgres:** `ERROR: syntax error at or near "["` at plan generation when array aggregates slip through (leaks Pure array literal syntax); `ERROR: operator does not exist: integer = text` on primitive IN mixing types.
- **SqlServer:** reports floating-point-domain errors as `An invalid floating point operation occurred.` when ANSI math functions overflow domain (log on negative, sqrt of negative).
- **Oracle:** most array-manipulation functions flagged as `[unsupported-api]`; uses double-quote identifiers and date-only semantics for `Date`.
- **DuckDB:** often sees `Unexpected error executing function with params [Anonymous_Lambda]` for functions that push platform-eval lambdas to SQL; `time_bucket` origin differs from Snowflake (handled via `constructTimeBucketOffset`).
- **ClickHouse:** uses `array*` prefixed functions (`arrayMax`, `arrayMin`, `arrayFirst`) — dialect extension rewrites via `getDynaFunctionToSqlForClickHouse()`; reserved-word list is empty; bool literal rendered as `'t'::Bool`.
- **Snowflake:** `time_slice` instead of `time_bucket`; additional semistructured test sibling `Test_Relational_Snowflake_Semistructured.java`. Cloud-only (pct-cloud-test profile).
- **Databricks:** Cloud-only; all seven PCT categories registered but unverified locally.
- **Spanner/DeepHaven:** require Docker via Testcontainers.
- **Trino/Presto:** share lineage but distinct extension files; Presto has a noticeably larger test sidebar (`testPrestoSliceTakeLimitDrop.pure`, `testPrestoWithFunction.pure`, etc.).

## Adapter qualifiers vocabulary (repeat as needed)

- `needsImplementation` — function not yet wired in this dialect (most common for new features).
- `unsupportedFeature` — dialect fundamentally cannot support (semantic, not effort).
- `needsInvestigation` — failure mode not yet understood.
- `assertErrorMismatch` — test uses `assertError` but the thrown string differs across dialects.
