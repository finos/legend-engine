---
name: New-feature wiring recipes
description: Minimal file sets to touch when adding a new Pure function, SQL construct, or dialect to the pure2sql layer
type: reference
---

Baseline: `docs/pct/wiring-howto.md`, `docs/pct/purefunction-howto.md`, `docs/pct/native-howto.md` cover the end-to-end story. These notes capture only what is not in the docs or is load-bearing.

## Adding a new ANSI SQL scalar (e.g. `cosh`)

1. Register the Pure function signature in `Handlers.java` (Alloy compiler) — both places (one per arity/variant).
2. Add the dyna-function mapping in `core_relational/relational/sqlQueryToString/extensionDefaults.pure`:
   ```pure
   dynaFnToSql('cosh', $allStates, ^ToSql(format='cosh(%s)')),
   ```
3. Wire the function expression into the Pure-to-SQL dispatch in `pureToSQLQuery.pure` (add to `supportedFunctions` map or matching process lambda).
4. Write PCT tests against `essentialFunctions` or `standardFunctions` scope, mark `<<PCT.test>>`.
5. Run one dialect PCT to confirm translation; if other dialects fail, decide per-adapter: add override vs mark `expectedFailure`.

## Adding a dialect-divergent function (e.g. `timeBucket`)

1. Same Handlers.java + pureToSQLQuery.pure + PCT steps as above.
2. Do NOT add to extensionDefaults — instead add per-dialect `dynaFnToSql` in each `<Dialect>Extension.pure`:
   - DuckDB: `cast(time_bucket(...) as timestamp_s)` with offset helpers
   - Snowflake: `TIME_SLICE(...)` with `constructInterval` helper
3. Decide routing: if semantics differ (e.g. null behaviour), leave it in-SQL and accept divergence, or mark `shouldStopRouting` via `meta::pure::router::routing::shouldStopFunctions` to force platform execution (see `router_routing.pure`).

## Adding an aggregate

Same as scalar but verify: (a) `state` in `dynaFnToSql` tuple includes both `GroupBy` and `Select` states as needed; (b) `joinStringsProcessor` override not needed unless it's a string-agg variant; (c) the plan generator produces a `GroupBy` clause, check with a `TdsSelectSqlQuery` assertion test.

## Adding a window function

- Window column rendering is controlled by `DbExtension.windowColumnProcessor`. Default is `processWindowColumnDefault`.
- Dialects with non-standard `OVER (…)` (e.g. ClickHouse, Sybase IQ) register a custom processor — grep for `windowColumnProcessor = processWindowColumn_*` to find the per-dialect override.

## Adding a new Pure function for which no SQL translation exists

If the function should be fully platform-evaluated:
- Add it to `shouldStopRouting` in `meta::pure::router::routing::shouldStopFunctions`. The router will stop clustering at that node and the platform (Java bytecode compiled from Pure) executes it.
- Required when SQL and platform semantics diverge in a way that would break correctness (e.g. null-safe comparisons, `between`).

## Adding a new dialect (summary — see docs/engineering/architecture/router-and-pure-to-sql.md §7)

- Pure: create `core_relational_<dialect>/relational/sqlQueryToString/<dialect>Extension.pure` with `<<db.ExtensionLoader>>`; add `DatabaseType.<X>`.
- Java: `DatabaseManager` subclass + `ConnectionExtension` + `StrategicConnectionExtension` + `DatabaseAuthenticationFlowProvider`.
- Grammar/protocol: grammar+compiler+composer+protocol extensions if the DSL needs dialect-specific constructs.
- PCT: `<dialect>-PCT` sub-module with 7 `Test_Relational_<Dialect>_*_PCT.java` classes, `CoreRelational<Dialect>PCTCodeRepositoryProvider`, services for `PCTReportProvider` + `TestConnectionIntegration`.
- CI: add the PCT module to `.github/workflows/resources/modulesToTest.json`.
- Decide default vs cloud-only; if cloud, guard with `pct-cloud-test` profile.

## Adding a new PCT test for an existing function

- Write the Pure test in the legend-pure or legend-engine module that owns its ReportScope (Essential/Grammar in legend-pure; Standard/Relation/Variant/Unclassified/ScenarioQuant in legend-engine).
- Decorate with `<<PCT.test>>` so the PCT runner picks it up automatically across all registered adapters.
- Run the relevant `Test_Relational_<Dialect>_<Category>Functions_PCT.java`; failing adapters either get a fix or a new `one(...)` in their `expectedFailures`.

## Decision tree for PCT failures on new work

- Platform-compiled or H2 fails → real bug; fix before merging.
- Cloud-only dialect fails → note expected outcome and escalate (CI runs there).
- Adapter fails for legitimate absence → `one("<sig>", "<substring>", AdapterQualifier.needsImplementation|unsupportedFeature)`.
- Error message drifts across dialects → `AdapterQualifier.assertErrorMismatch`.
- Unknown / don't-have-bandwidth → `AdapterQualifier.needsInvestigation` (but open an issue).
