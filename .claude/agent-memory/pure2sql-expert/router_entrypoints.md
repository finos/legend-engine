---
name: Router entry points and dispatch map
description: Concrete file paths and Pure symbols for the Pure-to-SQL pipeline (router, store contract, pureToSqlQuery, sqlQueryToString, dbExtension)
type: reference
---

## Pipeline stages (Pure side)

1. **Pre-eval** — `core/pure/router/preeval/...` (entry via `RouterExtension.shouldStopPreeval`).
2. **Router** — entry `meta::pure::router::routeFunction` in `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/router_main.pure` (multiple overloads at lines 30, 35, 123, 128, 133).
3. **Relational StoreContract** — `meta::relational::contract::relationalStoreContract` in `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/contract/storeContract.pure`. Wires `planExecution`, `supports`, `shouldStopRouting`, `shouldStopPreeval`, `routeFunctionExpressions`.
4. **Pure→SQL translation** — `meta::relational::functions::pureToSqlQuery::toSQLQuery` in `core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` (~10k lines). Dispatch table: `supportedFunctions` map inside `State`. Uses `processValueSpecification`/`processQuery`. Union handling in `pureToSQLQuery_union.pure`. Intermediate metamodel additions in `pureToSQLQuery/metamodel.pure`.
5. **SQL text generation** — `meta::relational::functions::sqlQueryToString::sqlQueryToString` in `core_relational/relational/sqlQueryToString/dbExtension.pure`. Default dispatch in `extensionDefaults.pure` (~1k+ dynaFnToSql registrations).

## Java entry

`org.finos.legend.engine.plan.generation.PlanGenerator` at `legend-engine-core/legend-engine-core-base/legend-engine-core-executionPlan-generation/legend-engine-executionPlan-generation/src/main/java/org/finos/legend/engine/plan/generation/PlanGenerator.java` bridges to the compiled Pure runtime and invokes `routeFunction`.

## Router hook points

- `StoreContract.supports(fe)` — gates which function expressions a store claims.
- `StoreContract.shouldStopRouting` — a hard list of Pure functions that are already bound to a store and must not be further routed (e.g. `tableToTDS`, `viewToTDS`, `join(TDS,…)`, `tableReference`).
- `StoreContract.routeFunctionExpressions` — `(matcher, handler)` pairs that override routing per expression type.
- `RouterExtension.routeFunctionExpressions` — same at extension level.
- `RouterExtension.shouldStopPreeval` — controls preval short-circuiting.

## Dialect loader mechanism

Dialect registration uses the Pure stereotype `<<db.ExtensionLoader>>` on a function returning `DbExtensionLoader`. `getDbExtensionLoaders()` (in `dbExtension.pure`) uses `db->stereotype('ExtensionLoader')` reflection to discover them.

## Debugging

Pass `DebugContext(debug=true, space='')` to `routeFunction`. For iteration on `.pure` without rebuilding Java, use PureIDE (main: `org.finos.legend.engine.ide.PureIDELight`, config: `legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json`, UI at http://127.0.0.1:9200/ide).
