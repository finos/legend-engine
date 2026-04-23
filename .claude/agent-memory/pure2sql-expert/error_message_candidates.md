---
name: Error-message improvement candidates
description: Cryptic runtime messages that users hit during Pure-to-SQL translation, with exact source locations so they can be upgraded to actionable diagnostics later
type: reference
---

These are the highest-leverage error sites to improve — each appears hundreds of times in PCT expectedFailures lists and is the first thing developers see when a new function isn't wired. Do not edit silently; each change is a breaking diagnostic change and needs tests.

## 1. `No SQL translation exists for the PURE function '<sig>'`

- **Origin:** `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure:2349`.
- **Current wording:** `'No SQL translation exists for the PURE function \'…\'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.'`
- **Gap:** no dialect name, no source info (Pure `SourceInformation` not threaded here), no list of siblings that ARE supported. Typical hit: `forAll_T_MANY__Function_1__Boolean_1_`, `add_T_MANY__Integer_1__T_1__T_$1_MANY$_`.
- **Improvement target:** include the `DatabaseType`, the dyna-function name attempted, and a pointer to `docs/pct/wiring-howto.md`.

## 2. `[unsupported-api] The function '<name>' (state: [<Side>, <inWhenClause>]) is not supported yet`

- **Origin:** `legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbExtension.pure:1041` (inside `dynaFuncDispatch` fallback, around `assertSize`).
- **Current wording:** `[unsupported-api] The function 'X' (state: [Select, false]) is not supported yet`.
- **Gap:** `state` tuple leaks Pure internal state; users can't map `[Select, false]` to meaning. No dialect named. No hint that the fix is to add a `dynaFnToSql` row.
- **Improvement target:** include dialect, drop state telemetry (or move it behind debug), link to wiring-howto.

## 3. `Couldn't find DynaFunction to Postgres model translation for <func>().`

- **Origin:** `legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/sqlDialectTranslation/toPostgresModel.pure:264` inside `state.dynaFunctionConverterMap->get(...)->toOne('...')`.
- **Current wording:** `Couldn\'t find DynaFunction to Postgres model translation for ` + name + `().`
- **Gap:** Users hitting this from H2/DuckDB are confused why Postgres is mentioned — this is the new **legend-sql** lowering layer, not the H2 dialect. Dominant in H2 expectedFailures (`toVariantList`, `toVariantObject`).
- **Improvement target:** rename message to `Couldn't lower DynaFunction <X> into the legend-sql relational AST (Postgres-model converter). Known converters: …`; and/or document that "Postgres model" is actually the shared SQL IR.

## 4. `<typename> is not managed yet!`

- **Origin:** `pureToSQLQuery.pure:9758` — `fail ($genericType->printGenericType(false) + ' is not managed yet!')`. Appears in `testConcatenateMixedType` as `Any is not managed yet!`.
- **Gap:** `Any` is a meaningless word to users; doesn't say what stage of type dispatch this is.
- **Improvement target:** clarify that this is literal-type inference inside `processValueSpecification`, include the function context and the Pure call stack frame.

## 5. `The database type '<X>' is not supported yet!`

- **Origin:** `core_relational/relational/sqlQueryToString/extensionDefaults.pure:712–713` inside `joinStrings` category dispatch.
- **Gap:** The error uses `$sgc.dbConfig.dbType->id()` but doesn't say which category (groupByCat vs stringCat) was missing, so the dialect author doesn't know which hook to implement.
- **Improvement target:** add the concrete category/function name.

## 6. `Unused format args. [N] arguments provided to expression "<format>"`

- **Origin:** `ToSql.format` vs `$params->size()` mismatch surfaces deep in the formatter (needs to be traced). Dominates Postgres stdDev/variance/corr/covar/max-on-arrays failures.
- **Improvement target:** the error needs to name the offending `DynaFunctionToSql.funcName` and the dialect so the fix site is obvious. Currently forces developers to grep the format string to find the registration.

## 7. `Found 0 nodes` / `Found N nodes, expected 1` (findOneNode)

- **Origin:** `legend-pure` platform `findOneNode` — generic assertion `size == 1` on the result of `findNode`.
- **Seen via:** `isolateTdsSelect` 4-param overload in `pureToSQLQuery.pure` ~line 3052-3068 (fixed session 2026-04-22 to fall back to `$newNode` when remap returns `[]`). Same pattern may exist in other tree-rewriting helpers that do cursor remap without a fallback.
- **Gap:** the message has zero context — no query, no operation (`pivot`, `concatenate`, `isolateTdsSelect`), no hint that the call-site invariant is "cursor should be remappable into the new isolated tree". Developers see the error and have no signal on where the assumption broke.
- **Improvement target:** at pure2sql call sites to `findOneNode`, wrap the call or add a richer assertion naming the operation (`'isolateTdsSelect: could not remap cursor from <old root printout> to <new root printout>'`) before falling back.
- **Reference-equality gotcha:** `findNode` uses `oldTarget == oldNode` (reference equality). Two `root[]` nodes that print identically can still be distinct instances — that's the class of shape that triggers "Found 0 nodes" via the CTE path for pivot-after-concatenate.

## 8. `NODE VALIDATION ERROR: currentTreeNode root DOESN'T CONTAIN root`

- **Origin:** post-processor validation inside relational plan generation (search `NODE VALIDATION ERROR` in `pureToSQLQuery.pure`).
- **Seen via:** inline `#TDS#->concatenate(#TDS#)->pivot(...)` — a DIFFERENT code path from the `isolateTdsSelect` findOneNode path. Same "pivot after concatenate" shape, different direction, depending on whether CTEs (`let a = #TDS#; ...`) are used.
- **Gap:** no hint which query/operation caused the mismatch, no printout of the two roots. Users cannot tell whether this is the same bug as the `findOneNode` one or distinct.
- **Improvement target:** include both node identities (printGenericType-style) in the message, and name the containing operation.

## Companion: platform error strings seen in PCT failures

These come from legend-pure / platform; don't edit in-tree:

- `Cannot cast a collection of size 0 to multiplicity [1]` — `toOne` on empty at runtime.
- `Function does not exist 'meta::pure::functions::collection::find(String[3], …)'` — compile-time signature mismatch after a Pure signature change.
- `Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda` — fold translation state machine mismatch inside relationalMappingExecution.

Improvement must-haves (project conventions): Java side uses `EngineException(SourceInformation, EngineErrorType.{COMPILATION|EXECUTION|PARSER|INTERNAL})`; Pure side uses `fail('msg')` / `assert(cond, | 'msg')`. Keep messages SLF4J-safe and never log credentials.
