---
name: relation-mapping-routing-pathway
description: How a Relation mapping (RelationFunctionInstanceSetImplementation) is routed and store-bound; there is NO in-memory execution arm — always relational SQL pushdown
metadata:
  type: project
---

Relation mappings (`RelationFunctionInstanceSetImplementation` / `RelationFunctionClassMapping`, `~func`/`~src`) execute only via relational-store SQL generation. There is no in-memory execution arm for a relation *mapping*, even though the relation *operators* (join/select/extend) have in-memory Java implementations used by the relation-function PCT suites for bare function calls.

**Evidence:**
- Store binding for a `RelationFunctionInstanceSetImplementation` is special-cased, not via `StoreContract.supportsSetImplementation`: `core/pure/router/store/cluster.pure` `storeContractForSetImplementation` has a dedicated arm that reads the store off the routed body's `StoreClusteredValueSpecification` (`$vs->at(0)->cast(@StoreClusteredValueSpecification).store`), then `storeContractFromStore`.
- No store contract lists `RelationFunctionInstanceSetImplementation` in `supportsSetImplementation`. The relational contract (`core_relational/relational/contract/storeContract.pure`) lists only `RelationalInstanceSetImplementation` and a `CrossSetImplementation` targeting a `Database`.
- The store is attached to the body only through a `RelationStoreAccessor` (`#>{db.table}#`), via `routing.pure` `processRelationStoreAccessor`. Relational is the only store that registers a `RelationElementAccessorExtension` (`core_relational/relational/extensions/extension.pure`, `helperFunctions.pure` `syntheticRelationalAccessorExtension`).
- `routing.pure` `potentiallyRouteRelationFunction` routes the `~func` body via `routeFunction`; `isRelationFunctionRouted` checks the body's first expression is a `ClusteredValueSpecification`. `pureToSQLQuery.pure` `processRelationFunctionClassMapping` then lowers the routed body to a `SelectSQLQuery`.
- The ModelJoin and navigationJoin/filterJoin arms (`relationalModelJoins.pure` `compileModelJoinForBranch`, `buildModelJoinPlaceholder`, `buildBranchSourceSWC`; `pureToSQLQuery.pure` `processRelationMappingJoin`) are all in `meta::relational::functions::pureToSqlQuery` and emit SQL AST. No model-store equivalent exists.

**Compile-time note:** `ClassMappingFirstPassBuilder.visit(RelationFunctionClassMapping)` builds the set implementation without binding a Database. So a `~func` with no `#>{db.table}#` compiles, but at routing there is no `StoreClusteredValueSpecification`, so the `cluster.pure` assert fails ("... may not have been routed correctly. Found a X instead of a StoreClusteredValueSpecification."). Inferred from the assert and the store-binding path, not from a run.

Executing a relation mapping in memory would need a new `StoreContract` claiming `RelationFunctionInstanceSetImplementation` plus a route to the relation-operator in-memory implementations; neither exists.
