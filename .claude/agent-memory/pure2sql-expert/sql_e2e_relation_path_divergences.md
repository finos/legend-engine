---
name: sql-e2e-relation-path-divergences
description: Root causes for SQL e2e parity corpus failures that hit only the Relation accessor path (quantified/scalar subqueries, LIMIT+OFFSET, NULLS ordering, IS DISTINCT FROM) - investigated 2026-09-29
metadata:
  type: project
---

Investigated 2026-09-29 on branch `fix-not-in-subquery-three-valued`, using the interpreted
Pure LSP daemon against the `legend-engine-xt-sql-e2e-tests` parity corpus.

## 0. THE RULE THAT MATTERS: interpreted corpus runs are not a valid oracle for ERROR-class failures

Three separate ERROR-class failure groups (quantified/scalar subqueries, LIMIT+OFFSET,
NULLS ordering) all turned out to be **Pure-interpreter artifacts**, not product bugs - two
distinct interpreter defects (`pair` generic resolution; `reactivate`/preeval) rather than
anything in the SQL transform. Meanwhile the FAIL-class results (wrong rows returned from a
really-executed plan against Postgres) were all trustworthy and did surface real bugs.

So when triaging a corpus run from the LSP dev loop:
- status ERROR  -> suspect the interpreter first; re-verify compiled before opening a bug.
- status FAIL   -> trustworthy, the plan really ran; treat as a real divergence.

## 1. `pair(enum, <generic function reference>)` breaks in the Pure INTERPRETER

**This is the single highest-value finding.** `meta::pure::functions::collection::pair<U,V>` fails at
`platform/pure/essential/collection/anonymous/pair/pair.pure:41` (`^Pair<U,V>(first=$first, second=$second)`)
whenever the *second* argument is a reference to a **type-parameterised** function.

Isolated repro (all four probes run interpreted via `pure-lsp execute`):
- `pair('m', minus_Integer_MANY__Integer_1_)` (monomorphic fn) -> OK
- `[equalAny_..., greaterThanAny_...]` (generic fns in a plain list, no pair) -> OK
- `pair('eq', equalAny_U_$0_1$__Relation_1__Boolean_1_)` -> **FAILS**
- `pair('eq', equalAny_...->cast(@Function<Any>))` -> OK  <- the fix shape

Error text (standalone form):
`Error instantiating the type 'Pair<String, ConcreteFunctionDefinition<<U,Z> {U[0..1], Relation<Z=(?:U)>[1]->Boolean[1]}>>'.`
`Could not resolve type for the property 'second': ConcreteFunctionDefinition<{String[0..1], Relation<Z=(?:String)>[1]->Boolean[1]}>`

Note the captured substitution: `pair`'s own `U` (bound to the *key* type, here `String`) got
substituted into the *function's* independently-quantified `U`, and the `<U,Z>` quantifier was
dropped. Classic type-variable name capture - `pair<U,V>` and
`equalAny<U,Z>`/`greaterThanAll<U,Z>`/... all use the name `U`.

Deep inside the SQL transform the same defect surfaces with a *different* message:
`Type Error: 'ConcreteFunctionDefinition<<U,Z> {...}>' not a subtype of 'SqlTransformContext'`.
`SqlTransformContext` comes from an enclosing frame's `V` binding - `utils::trace<V|m>`
(`core_external_query_sql/binding/fromPure/utils.pure:73`) wraps `processRootQuery`, which returns
`SqlTransformContext`. When `pair`'s own `V` cannot be resolved the interpreter falls back to an
outer frame's same-named type variable. Same root cause, two manifestations.

Affected call sites (both build `[pair(ComparisonOperator.X, <generic relation fn>), ...]->getValue(op)`):
- `fromPure.pure:3539-3556` `quantifiedComparisonFunction` (ANY/ALL/SOME)
- `fromPure.pure:3608-3617` `scalarComparisonFunction` (scalar subquery comparison)

Compiled mode appears unaffected: the generated Java
(`target/generated-sources/.../core_external_query_sql_binding_fromPure_fromPure.java:7447`)
builds `Pair<Enum, ConcreteFunctionDefinition<?>>` with Java wildcards and does no runtime
generic-type resolution. NOT independently verified by a compiled test run.

Consequence: the checked-in `<<test.Test>>` suite
`queryToPure::tests::testQuantifiedComparison*` / `testScalarSubquery*`
(`core_external_query_sql/binding/fromPure/tests/testTranspile.pure:5500+`) **cannot be run
through the Pure LSP / interpreted mode at all**. Don't mistake that for a product regression.

## 2. TDS vs Relation on quantified + scalar subqueries is NOT a divergence

`processQuantifiedComparisonExpression` (fromPure.pure:3500) and
`processScalarSubqueryComparison` (fromPure.pure:3562) both open with
`assertRelation($context.relation, ...)`, which raises
`Unsupported: quantified comparison only supported on relation inputs` from
`utils.pure:64`. So the TDS path bails out *before* the pair() bug. The corpus records
`expected_tds_status: ERROR` for exactly these cases. "TDS passes" = "TDS errors as expected".

## 3. LIMIT+OFFSET: the unfolded `offset + limit` is FINE - preeval folds it in compiled mode

`fromPure.pure:2280-2283` builds `^ArithmeticExpression(ADD, left=$offset, right=$limit)`, so the
transform emits `slice(rel, 2, plus(2,3))` rather than `slice(rel, 2, 5)`. **This is intended and
mode-independent** - the checked-in CI test `queryToPure::tests::testLimitOffset`
(`testTranspile.pure:547-565`) literally asserts `->slice(2, 2 + 1)` for BOTH the TDS and Relation
expected lambdas, and that test PASSES interpreted.

The fold happens later, in **pre-evaluation**, via `reactivate`:
`preeval.pure:859`  `let valX = $newSfe->reactivate($state.inScopeVars)->cast(@Any);`

`reactivate` has two independent native implementations:
- `legend-pure-runtime-java-engine-compiled/.../natives/essentials/meta/reflect/Reactivate.java`
- `legend-pure-runtime-java-engine-interpreted/.../natives/essentials/meta/reflect/Reactivate.java`

Compiled folds `plus(2,1)` -> `3`, so `processSlice`'s `instanceOf(Literal)` assert is satisfied.
Interpreted passes the raw `InstanceValue` argument nodes to `plus`; stack bottom proves it:
```
reactivate(ValueSpecification[1], Map<String, List<Any>>[1]):Any[*]  <- preeval.pure:859
plus(Integer[*]):Integer[1]                                         <- compileUtils.pure lines:102c3-110c3
```
-> TDS: `Not a number: @_0...(...) instanceOf InstanceValue`.
-> Relation: preeval never folds that node, so the `plus` survives to
   `pureToSqlQuery::processSlice` (`pureToSQLQuery.pure:3854`) ->
   `Invalid type for second parameter inside the slice function`.

**Misleading error location trap:** `compileUtils.pure:102c3-110c3` is only the SourceInformation
stamped on the `plus` node when `sfe` created it. The failing code is in preeval/reactivate.
Good candidate for an error-message improvement.

## 3b. `order_*nulls*` (Relation) is the same subsystem

`Execution error at (resource:/core/pure/router/preeval/preeval.pure line:238 column:47)` with no
detail. Bottom frame is `Property values{InstanceValue[1]->Any[*]}` - i.e. reading `$iv.values` at
`preeval.pure:238` blows up in the interpreter. Reached from the router via
`prevalFunctionExpressionIfRequired` (`router_routing.pure:241,252`). The Relation sort wrapper is
built differently from TDS at `fromPure.pure:5840-5846` (Relation passes an explicit resolved
genericType, TDS does not) - plausible trigger, unconfirmed.

## 4. Three-valued-logic leaks: Pure formula is two-valued, emitted SQL is three-valued

- `createIsDistinctFrom` (`fromPure.pure:4822`) =
  `not( (isEmpty(l) and isEmpty(r)) or (l == r) )`. Correct under Pure's two-valued `==`;
  in SQL with `age` NULL it becomes `NOT(FALSE OR (age = 30))` -> `NOT NULL` -> UNKNOWN -> row
  dropped. Observed: Relation returns 8 rows where Postgres returns 9.
- `convertToSearchedCaseExpression` (`fromPure.pure:3947`) rewrites `CASE x WHEN y` into
  `x = y`, so `CASE age WHEN NULL` becomes `age == []`, which is TRUE in Pure for a null age.
  Observed: Relation labels the null-age row 'null' where Postgres/TDS say 'not null'.

This is the same family of hazard the branch's `processNotInSubquery` /
`createColumnIsNullLambda` / `createColumnMatchesLambda` work is navigating: a `not(... or ...)`
built over a nullable column is NULL-poisoned once it reaches SQL.

## 5. Corpus baseline errors found (not code bugs)

`null_equals_null` and `null_not_equals` (`structural/null_semantics.yaml:26,33`) return `true` on
**both** TDS and Relation (Postgres gives NULL). `expected_tds_status: FAIL` is right;
`expected_rel_status: PASS` is wrong and should be FAIL. Not a Relation-path divergence at all.
