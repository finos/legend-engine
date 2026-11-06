---
name: navfilterjoin-sqlgen-wiring
description: How navigationJoin/filterJoin are lowered to SQL — processRelationMappingJoin dispatch, single-hop navigationJoin via compileModelJoinForBranch + one RelationalOperationElementWithJoin per column, filterJoin/chains via a source-PK-keyed sub-select, JoinKind defaults
metadata:
  type: project
---

**Dispatch:** `pureToSQLQuery.pure` `getSupportedFunctions` maps the 3- and 4-arg (`JoinKind`) overloads of `navigationJoin` and `filterJoin` to `processRelationMappingJoin`. `relationMappingJoinKind` returns `'nj'`/`'fj'`.

**Single-hop navigationJoin** (source is `$src`): `$src` has no SQL-generation binding, so the source set is the enclosing RFPM's `sourceSetImplementationId`. `relationMappingJoinTargetSet` builds a synthetic `RelationFunctionInstanceSetImplementation` (id `<source id>_<hopNodeId>_target`) over the target relation function (`parametersValues->at(1)`); `relationMappingJoinCondition` takes the lambda from the last parameter and applies `withJoinSideSentinels`; `compileModelJoinForBranch` (`relationalModelJoins.pure`) builds the join tree; `selectColumnsThroughJoin` exposes each target column (`extractRelationColumns`) as `^Alias(name, relationalElement = ^RelationalOperationElementWithJoin(joinTreeNode, ^TableAliasColumn(...)))`.

**filterJoin or any chain:** `processRelationMappingJoinAsKeyedSubselect` lowers the property to one join from the source onto a sub-select `SELECT <source PK>, <kept columns> FROM <source copy> JOIN <hops>`, keyed on the source primary key (asserted there, at plan generation, when the PK can't be determined; neither compiler checks it, as legend-pure has no SQL knowledge). `buildKeyedJoinChain` builds the hops innermost-first: a navigationJoin hop keeps the target alias/set, a filterJoin hop keeps the previous one. Key columns are named by `relationJoinKeyName`; the key equalities are combined with `andFilters`.

**Why one RelationalOperationElementWithJoin per column works:** the terminal `.col` compiles to a `meta::pure::metamodel::relation::Column` function expression, dispatched to `processColumnFunctionExpression`, which casts the receiver to `SelectWithCursor` and looks the column up by name (`findColumn`). The processor therefore returns a `SelectWithCursor` whose columns are self-contained join-carrying aliases; `.col` picks one, `evaluateRfpmValueFn` keeps `select.columns->toOne()`, and `processColumnsInRelationalOperationElements` → `resolveJoinElement` applies the join onto the real source.

**Gotchas:**
- `resolveJoinElement` reprocesses the terminal column's alias by matching: `NamedRelation` → `.name`, otherwise the alias's own name — a relation-function target wraps a `SelectSQLQuery`, not a `NamedRelation`.
- Join kind: `relationMappingJoinType(fe, default)` reads an explicit `JoinKind` literal (4-arg overloads) and otherwise uses the caller's default: a single-hop navigationJoin defaults `LEFT_OUTER`; in `buildKeyedJoinChain` a filterJoin hop defaults `INNER` (its gate) and a navigationJoin hop `LEFT_OUTER`. `JoinKind.RIGHT`/`FULL` are rejected at compile time by both compilers.

See [[modeljoin-joincondition-empty-params]] and docs/engineering/architecture/relation-mapping.md §9.5.
