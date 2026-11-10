---
name: modeljoin-joincondition-empty-params
description: Where a ModelJoin join condition's two side names come from — every compiled ModelJoin condition (association, XStore-converted, inline relation-mapping modelJoin) reaches SQL-gen on the _mj_src/_mj_tgt sentinels; navigationJoin/filterJoin are renamed to them at SQL-gen time
metadata:
  type: project
---

**Association ModelJoin (grammar):** `HelperMappingBuilder.rewriteModelJoinExpression` rewrites the user's lambda body onto the `_mj_src`/`_mj_tgt` sentinels (`HelperMappingBuilder.MODEL_JOIN_SOURCE_VAR`/`MODEL_JOIN_TARGET_VAR`) and sets the protocol lambda's `parameters = Collections.emptyList()` (docs/engineering/architecture/model-join.md §4, "Compiler output for the running example"). `PropertyMappingBuilder.compileModelJoinBooleanLambda` then compiles it with two parameters named `_mj_src`/`_mj_tgt`, so the compiled M3 function type carries those names.

**XStore-converted ModelJoin:** `routing.pure` `renameXStoreLambdaVars` rewrites only the body (`this`→`_mj_src`, `that`→`_mj_tgt`); the function type keeps the stale `this`/`that` parameter names. Never read side names from an association-owned condition's function type.

**Inline relation-mapping modelJoin** (`modelJoin({p, f | $p.firmId == $f.id})`, model-level, no `$src`): `PropertyMappingBuilder.compileRelationModelJoinCondition` (called from `ClassMappingThirdPassBuilder.compileRelationModelJoins`) rewrites the user's positional parameters onto `_mj_src`/`_mj_tgt` with `ModelJoinLambdaRewriteVisitor` and types them to each set's MappingClass (or class), exactly like an association ModelJoin. legend-pure's `RelationFunctionInstanceSetImplementationProcessor` does the same for PAR-compiled mappings.

**Reading the names:** the router hard-codes `['this', '_mj_src', '_mj_tgt']` (`router_routing.pure`) and `router/store/builder.pure` `potentiallyRoutePropertyMapping` binds `_mj_src`/`_mj_tgt`; `compileModelJoinForBranch` (`relationalModelJoins.pure`) always receives `['_mj_src', '_mj_tgt']`.

**navigationJoin/filterJoin:** conditions keep the user's names through compilation (`Handlers.NavigationJoinFilterJoinInference`), but SQL generation renames them to `_mj_src`/`_mj_tgt` (`pureToSQLQuery.pure` `withJoinSideSentinels`, applied by `relationMappingJoinCondition`) before `compileModelJoinForBranch`: SQL generation inlines a user function by binding its parameters by name, so a helper parameter sharing a side's name would capture it.
