# ADR-003: Support Negating `relation::in()` on Relational Stores

**Status:** Accepted
**Date:** 2026-09-25 (implemented 2026-09-26)
**Deciders:** _Pending review — implemented, not yet merged_

---

> Implemented. The design below is what was built, with two deviations from the original draft, both
> noted inline: the generic-negation fallback resolves its operand through the cursor's
> `filteringOperation` instead of a synthetic `SimpleFunctionExpression` wrapper, and `and`/`or`
> recursion suffixes the `nodeId` per operand. See *Verification* at the end for what was actually run.

## Context

`relation::in(value, rel)` is `Boolean[1]`: `value->isNotEmpty() && rel->exists(row | col(row) == value)`.
PR #5066 made the relational router (`pureToSqlQuery::processNot`) reject any `!$value->in($rel)`
outright, because the only translation available at the time — a raw `NOT (value IN (subquery))`,
compensated with `OR value IS NULL` to match `in()`'s two-valued answer — puts a subquery under a
top-level `OR`, a shape some dialects' optimizers refuse to unnest (Snowflake failed on exactly one
test, `testIn_Negated_NullValueAndNullInRelation`, whose compared column wasn't in the outer
projection).

A separate piece of work in the same area needed a *different* (three-valued, SQL-`NOT IN`-matching)
rewrite for the SQL-text transpiler (`fromPure.pure`'s `processNotInSubquery`), and in verifying it,
found that folding the null-guard **inside** a single `NOT EXISTS`'s own `WHERE` — rather than `OR`-ing
it outside a subquery-bearing predicate — sidesteps the whole "subquery under OR" class of problem,
because `NOT EXISTS` is never itself `UNKNOWN` regardless of what's inside it. The same trick applies
to `relation::in()`'s own (two-valued) negation:

```
!$value->in($rel)  ==  isEmpty(value) || !exists(match)
                   ==  !exists(row | value->isNotEmpty() && col(row) == value)     -- single NOT EXISTS
```

Verified on H2 and DuckDB (via the Pure LSP dev loop) against the full truth table (null value +
null-in-relation, value present with an unrelated null in the relation, empty relation + null value,
no nulls at all) — matches `relation::in()`'s own two-valued semantics exactly in every case, and
generates exactly one `NOT EXISTS (... WHERE value IS NOT NULL AND col = value)`, confirmed via
`ExecDebug`/`ShowLocalPlan`.

This proposal removes the #5066 rejection and wires the relational router to emit that rewrite
instead — for both the **direct** case (`!$value->in($rel)`) and the **nested** case
(`!($a && $value->in($rel))`, `!($a || $value->in($rel))`, arbitrarily deep), matching the full scope
`negatesRelationIn` already detects today (it just currently rejects everything it finds instead of
fixing it).

**This is separate from, and does not change,** `fromPure.pure`'s `processNotInSubquery` — that
implements the strictly different *three-valued* semantics real SQL `NOT IN (subquery)` needs (the
classic nulls-trap), which `relation::in()`'s own two-valued negation deliberately does not reproduce.
Both are correct, for different starting expressions.

## Decision

All changes are confined to
`legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure`.

**Why direct `RelationalOperationElement`/`DynaFunction` construction, not a Pure-AST rewrite-and-recompile:**
confirmed via research that `sfe`/`iv`/`lambda` (the AST-builder helpers `fromPure.pure` uses) live in
`meta::external::query::sql::transformation::compile::utils`, in the `xt-sql-pure` module — and
`xt-relationalStore-core-pure` (this file's module) is a *dependency of* `xt-sql-pure`, not the reverse,
so those helpers are not importable here. Building a **fresh `LambdaFunction`** from scratch at this
layer has no precedent in the file and real gotchas (`classifierGenericType`/`genericType` wiring for
`withRelationVarScope` to bind the row variable). The file's own established idiom for negating a
Relation predicate is instead **direct construction reusing the positive form's own resolution
helper** — see `processNegatedRelationScalarComparison` (line 7304), which reuses
`buildRelationScalarComparisonOp` the same way `processRelationIn`/`processRelationExists` reuse
`buildRelationSubSelect`. No new `LambdaFunction` is needed at all: `buildRelationSubSelect` already
returns a `SelectSQLQuery` whose `.columns->at(0)` is the resolved single column and whose
`.filteringOperation` already carries any existing filters — exactly what's needed to bolt on
`AND col = value` directly.

### 1. New leaf builder: `processNegatedRelationIn`

Mirrors `processRelationIn` (line 7183) structurally, but builds `NOT EXISTS` instead of `IN`, and
needs none of `excludeNullsFromSearchedColumn`'s compensation (that machinery exists solely because raw
`IN` is three-valued when the subquery has nulls; `EXISTS`/`NOT EXISTS` never is, in a filter *or* a
projection context):

```pure
function <<access.private>> meta::relational::functions::pureToSqlQuery::processNegatedRelationIn(expression:FunctionExpression[1], currentPropertyMapping:PropertyMapping[*], operation:SelectWithCursor[1], vars:Map<VariableExpression, ValueSpecification>[1], state:State[1], nodeId:String[1], aggFromMap:List<ColumnGroup>[1], context:DebugContext[1], extensions:Extension[*]):RelationalOperationElement[1]
{
   let valueCursor = processValueSpecification($expression.parametersValues->at(0), $currentPropertyMapping, $operation, $vars, ^$state(inFilter = true), JoinType.LEFT_OUTER, buildNodeId($nodeId, '_v'), $aggFromMap, $context, $extensions)
                       ->toOne()->cast(@SelectWithCursor);
   let value = $valueCursor.select.filteringOperation->toOne();

   let subSelect = buildRelationSubSelect($expression, 1, $currentPropertyMapping, $operation, $vars, $state, $nodeId, $aggFromMap, $context, $extensions);
   assert($subSelect.columns->size() == 1, | 'in(..., Relation) expects a single-column relation, got ' + $subSelect.columns->size()->toString());

   let guardedMatch = ^DynaFunction(name = 'and', parameters = [
                          ^DynaFunction(name = 'isNotNull', parameters = $value),
                          ^DynaFunction(name = 'equal', parameters = [$subSelect.columns->at(0), $value])
                       ]);
   let matchSelect = ^$subSelect(columns = [^Literal(value = 1)],
                                  filteringOperation = $subSelect.filteringOperation->concatenate($guardedMatch)->andFilters($extensions));

   ^DynaFunction(name = 'not', parameters = ^DynaFunction(name = 'exists', parameters = $matchSelect));
}
```

### 2. Recursive dispatcher: `processNegatedRelationExpression`

Only entered when `negatesRelationIn(...)` is true (i.e. an `in()` really is reachable through
and/or/not). Pushes the negation down via De Morgan, special-casing `in()` (-> #1) and buried
scalar-relation-comparisons (-> `buildNegatedRelationScalarComparisonPredicate`, factored out of the
existing `processNegatedRelationScalarComparison` so a mixed `!($x->equalTo($rel) || $y->in($rel2))`
is handled on *both* sides), and otherwise falling back to a generic negation.

**Deviation from the draft (1/2) - how a non-`in()` operand is resolved.** The draft proposed
delegating to `processUnary` via a synthetic `SimpleFunctionExpression` wrapper. That does not work:
`processUnary` returns a `SelectWithCursor`, but the De Morgan walk composes its results into
`DynaFunction` *parameters*, which need a bare expression. Feeding a cursor in fails at SQL-dialect
translation with `Match failure: ... instanceOf SelectWithCursor` (observed on
`testSimpleIn_Negated_And`). The fix is the file's own idiom - process with `inFilter` set and read
the `filteringOperation` back off the cursor, exactly as `processRelationIn` resolves its value
operand. No synthetic AST node is needed at all:

```pure
function <<access.private>> ...::processRelationNegationOperandPredicate(v:ValueSpecification[1], ...):RelationalOperationElement[1]
{
   let cursor = processValueSpecification($v, $currentPropertyMapping, $operation, $vars, ^$state(inFilter = true), JoinType.LEFT_OUTER, $nodeId, $aggFromMap, $context, $extensions)
                  ->toOne()->cast(@SelectWithCursor);
   $cursor.select.filteringOperation->toOne();
}

function <<access.private>> ...::processGenericRelationNegation(v:ValueSpecification[1], ...):RelationalOperationElement[1]
{
   ^DynaFunction(name = 'not', parameters = $v->processRelationNegationOperandPredicate(...));
}
```

The same helper also serves the double-negation case (`!(!X)` -> `X`), which the draft likewise had
returning a raw cursor.

**Deviation from the draft (2/2) - per-operand `nodeId`.** The draft flagged as "verify empirically"
that `and`/`or` recursion passed the *same* `nodeId` to both operands, so two `in()` calls under one
connective could collide on the aliases `buildRelationSubSelect` derives from it. Rather than leave
that to chance it is now suffixed per operand, mirroring `processDynaFunction`'s `'_dy'+index`:

```pure
function <<access.private>> ...::processNegatedRelationConnective(connective:String[1], f:SimpleFunctionExpression[1], ...):RelationalOperationElement[1]
{
   ^DynaFunction(name = $connective,
                 parameters = $f.parametersValues->size()->range()->zip($f.parametersValues)
                                ->map(p | $p.second->processNegatedRelationExpression(..., buildNodeId($nodeId, '_ng' + $p.first->toString()), ...)));
}
```

### 3. `processNot` — replace only the top assert

Current (line 3527, first two lines only shown):
```pure
assert(!$f.parametersValues->at(0)->negatesRelationIn(), | 'Negating in(value, Relation) is not supported...');
```
becomes:
```pure
if($f.parametersValues->at(0)->negatesRelationIn(),
   | $operation->attachPredicate(processNegatedRelationExpression($f.parametersValues->at(0), $currentPropertyMapping, $operation, $vars, $state, $nodeId, $aggFromMap, $context, $extensions), $state);,
   | <rest of the existing function body, unchanged: the buriedRelationScalarComparisonConnective
      assert + match on scalarComparisonNameForNegation / processUnary fallback>
);
```
The existing `buriedRelationScalarComparisonConnective` assert and its guidance message are otherwise
**untouched** — it only fires in the `else` branch (no `in()` present at all), so the
still-real, still-unaddressed "scalar comparison alone buried under and/or" restriction is preserved
exactly as today.

## Other files changed

- **`legend-engine-core/.../relation/functions/quantification/in.pure`**
  - Doc comment on `in()`: the "Do not negate it against a relation... rejected on every relational
    store" paragraph is replaced by a description of what negation now does (two-valued, matching the
    positive form's own null handling). The "differs from SQL `NOT IN`" callout is kept and sharpened,
    since it is still true and is exactly why `processNotInSubquery` exists separately for SQL text.
  - `testSimpleIn_Negated` no longer pins the rejection — same data, now asserting the real answer
    (`2,Support`).
  - Eight new PCT tests. Five restore the null/edge coverage that #5066 deleted when it introduced the
    rejection (`testIn_Negated_NullInRelation`, `..._NullValue`, `..._NullValueAndNullInRelation`,
    `..._InExtend_NullValueAndNullInRelation`, `..._GroupedRelationWithNull` — the last exercising the
    `HAVING` branch of `excludeNullsFromSearchedColumn`), one adds the empty-relation case
    (`testIn_Negated_EmptyRelation`), and three cover this change's **nested** scope:
    `testSimpleIn_Negated_And`, `testSimpleIn_Negated_Or` (two independent `in()`s under one `or`,
    which is also what exercises the per-operand `nodeId`), and
    `testSimpleIn_Negated_Or_MixedWithScalarComparison` (a negated `in()` and a negated `equalTo()`
    under the same `or`, exercising the scalar-comparison delegation).

  Note `testIn_Negated_NullValueAndNullInRelation` reproduces the exact historical shape that failed
  on Snowflake under the old rewrite — null value, null in relation, and the compared column absent
  from the outer projection.

- **11 relational-store PCT manifests** — `testSimpleIn_Negated` exclusion entry removed from
  `relational-h2`, `-postgres`, `-oracle`, `-sqlserver`, `-trino`, `-snowflake`, `-spanner`,
  `-databricks`, `-clickhouse`, `-memsql`, `-duckdb`
  (all under `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/<adapter>/<adapter>-PCT/src/main/resources/pct-manifests/<adapter>/RelationFunctions_manifest.json`).

- **`java` and `deephaven` PCT manifests** — their pre-existing `testSimpleIn_Negated` exclusions are
  **kept**: they record unrelated limitations (the Java binding cannot translate a `#TDS` literal;
  Deephaven has no `relation::exists`/`in` translation), not this rejection. But because both manifests
  blanket-exclude *every* test in the `tests::in::` and `tests::equalTo::` families for those reasons,
  the nine new tests need the same treatment, so nine matching entries were added to each.

- **`fromPure.pure`'s `processNotInSubquery`, `composition.pure`'s `testNotInPattern_*` tests and
  `testTranspile.pure`** are changed too, but for a *separate* reason — see "Also fixed here" below.
  The two code paths stay independent: they answer different questions (two-valued negation of
  `relation::in()` vs SQL's three-valued `NOT IN`), and `processNotInSubquery` still never calls
  `in()`.

- **No changes** to the SQL e2e parity YAMLs — no parity case exercises a negated `relation::in()`
  directly, and the `NOT IN` cases keep the same results.

## Consequences

**Positive:** removes an artificial restriction on a documented, otherwise-normal Pure API
(`relation::in()`'s negation), for both the direct and and/or-nested shapes; reuses only existing,
proven helpers (`buildRelationSubSelect`, `buildRelationScalarComparisonOp`, `processValueSpecification`) —
no new AST-construction machinery, no cross-module dependency changes; the single-`NOT EXISTS` shape is
architecturally distinct from the subquery-under-`OR` shape that caused the original Snowflake failure.

**Cross-dialect portability - the AND- vs OR-correlation line.** The original #5066 failure and the
adapters that still cannot take this change turn out to be explained by one rule, confirmed
empirically (see *Verification*):

| Correlated-subquery shape | Snowflake | ClickHouse / MemSQL | everyone else |
|---|---|---|---|
| outer column under `AND` (`NOT EXISTS (... WHERE v IS NOT NULL AND col = v)`) - **this change** | works | fails | works |
| outer column inside an `OR` (`... WHERE v IS NULL OR col IS NULL OR col = v`) - the three-valued `testNotInPattern_*` pattern | **fails** | fails | works |
| subquery under a top-level `OR` (`v NOT IN (sub) OR v IS NULL`) - the pre-#5066 rewrite | fails | fails | works |

So this change lands in the best-supported row. The evidence that Snowflake accepts it, without it
being directly testable here, is `exists::testExistsCorrelated_Negated` - literally
`!$employees->exists(e | $e.department == $d.departmentId)`, i.e. this change's shape minus the
`IS NOT NULL` conjunct. It is excluded on exactly clickhouse, memsql and deephaven, and **passes on
Snowflake** and every other relational adapter.

**Negative / risk:** ClickHouse and MemSQL cannot express this at all, so they keep a
`testSimpleIn_Negated` exclusion (its message updated from the old rejection text to the real
engine error) and gain one for each new test. That is not a regression - they already exclude the
whole `in`/`exists`/`equalTo` family for the same underlying reason. Snowflake remains
reasoned-but-not-directly-executed for this specific shape; if CI disproves the reasoning, the fix
is a Snowflake-only exclusion, not restoring the blanket rejection.

## Verification (performed)

Run through the Pure LSP dev loop against the real H2 and DuckDB backends.

**PCT suite** — `meta::pure::functions::relation::tests::in` (20 tests: 11 pre-existing + 9 new):

| Adapter | Result |
|---------|--------|
| H2 | **20 passed, 0 failed** |
| DuckDB | **20 passed, 0 failed** |

**Generated SQL** (captured with `ExecDebug`) is the intended single-`NOT EXISTS` shape, with the
null-guard inside the subquery's own `WHERE` and no subquery under a top-level `OR`:

```sql
-- !$d.departmentId->in($employees->select(~department))
select ... from departments as "departments_0"
where not exists (select 1 from employees as "employees_0"
                  where "departments_0"."departmentId" is not null
                    and "employees_0"."department" is not distinct from "departments_0"."departmentId")

-- !($d.departmentId->in($employees) || $d.departmentName->in($supportNames))   -- De Morgan -> AND of two NOT EXISTS
where not exists (select 1 from employees     as "employees_0"    where ... ) 
  and not exists (select 1 from supportNames  as "supportnames_0" where ... )
```

The nested case confirms both the De Morgan flip and that the per-operand `nodeId` keeps the two
subquery aliases distinct. `equal` lowers to `is not distinct from` on this path; under the
`IS NOT NULL` guard that is equivalent to `=`, since the guard already excludes the only input for
which the two differ.

**No regression in the neighbouring `equalTo` family:** the only failures on H2 are the three tests
that were already manifest-excluded before this change (`testEqualTo_MoreThanOneRow`,
`testEqualTo_NegatedBuriedInAnd`, `testEqualTo_NegatedBuriedInOr`), and the latter two still emit the
*same* `buriedRelationScalarComparisonConnective` rejection — confirming that restriction survives
intact in the new `else` branch. (The LSP runner does not apply manifest exclusions, so expected
failures surface as failures there.)

**ClickHouse, on a real container** (via Testcontainers through the LSP) rejects this change's SQL:

```
DB::Exception: Resolve identifier 'departments_0.departmentId' from parent scope only supported for
constants and CTE. ... In scope (SELECT 1 FROM employees AS employees_0
WHERE (departments_0.departmentId IS NOT NULL) AND (employees_0.department = departments_0.departmentId ...))
(UNSUPPORTED_METHOD)
```

That is the *same* message as ClickHouse's ten pre-existing `exists::`/`in::` correlated-subquery
exclusions — it supports no correlated subquery at all — so this is a pre-existing engine limit, not
a regression. MemSQL's container image is unavailable in this environment, but it already excludes
**every** `in`/`exists`/`equalTo` test (including the *uncorrelated* `testSimpleIn` and
`testExistsUncorrelated`) with `"cannot appear in the definition of common table expression."`, so it
cannot express this either.

**Resulting exclusion matrix** (`testSimpleIn_Negated` + the 9 new negated tests):

| Adapter | negated-`in()` tests | why |
|---|---|---|
| h2, duckdb | **run, pass** | verified above |
| postgres, oracle, sqlserver, trino, spanner, databricks | **run** | all pass `testExistsCorrelated_Negated` |
| snowflake | **7–8 of 10 run**; 2 excluded, 1 provisional | see "Snowflake: what CI actually found" below |
| clickhouse, memsql | excluded | no correlated subqueries / no subqueries in a CTE |
| java, deephaven | excluded | cannot translate a `#TDS` literal / no `relation::exists` translation |

**Not verified here, by design:**

- Postgres, Oracle, SQL Server, Trino, Spanner and Databricks — no access in this environment.
  Their `testSimpleIn_Negated` exclusions were removed on the `testExistsCorrelated_Negated` evidence
  above, and CI has since confirmed all of them pass. Snowflake did **not** fully hold — see
  "Snowflake: what CI actually found".
- `deephaven` only to the letter of its message. Its 20 failures were reproduced through the LSP and
  are the same failure at the same place (`Match failure: ... instanceOf LambdaFunction`,
  `pure_to_deephaven.pure:335`), but the *interpreted* runtime renders the operand as an instance id
  (`@_01so9j8(...)`) where the *compiled* runtime CI uses renders it as `LambdaFunctionObject`. The
  manifest keeps the compiled-mode wording that all ~500 existing deephaven entries already use.

`java` **is** verified: with the adapter driven through the LSP, all 20 tests fail with exactly
`Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated` — the string used in
its manifest entries.

## Snowflake: what CI actually found

The reasoning above (that Snowflake accepts this shape because it passes
`exists::testExistsCorrelated_Negated`) held for 7 of the 10 negated-`in()` tests. CI found three
failures, all `Unsupported subquery type cannot be evaluated`, from **two distinct causes**:

**1. A subquery under a top-level `OR` — our bug, fixed.** De Morgan on an `and` turned
`!(A && in(v, rel))` into `!A OR NOT EXISTS(...)`:

```sql
where not "d_0"."departmentId" is not null or not exists (select 1 from employees ...)
```

That is the very shape #5066 was opened for, reintroduced through the nested path. Because the
sibling conjuncts only reference the outer row, they can be folded into the subquery instead —
`A AND EXISTS(P) == EXISTS(A AND P)` when `A` is invariant across inner rows — giving one
`NOT EXISTS` and no disjunction. `processNegatedRelationAnd` does this; shapes that genuinely need a
disjunction (two `in()`s under one `and`) still fall back to plain De Morgan.

The fold alone was not enough. `testSimpleIn_Negated_And` failed again on the next CI run, still
`Unsupported subquery type cannot be evaluated`, at a position 50 characters earlier — exactly the
length of the `not "…"."departmentId" is not null or ` prefix the fold had removed, confirming the
disjunction was gone and a second cause remained. That cause: the conjunct being folded in is itself
a null check on the same value, and `processNegatedRelationIn` already adds one. They are different
`DynaFunction`s — the user writes `isNotEmpty`, the guard emits `isNotNull` — that render to
identical SQL, so the subquery carried the same `IS NOT NULL` twice. Logically a no-op; enough to
stop Snowflake's decorrelator. `isNotNullCheckOn` drops the duplicate, matching on the operand
rather than the whole predicate (the two are resolved down different nodeIds, so they carry
different alias instances for the same column, and `buildUniqueName` must be called without alias
names for them to compare equal). With it, the emitted SQL is byte-identical — verified in the real
Snowflake dialect — to the already-passing `testSimpleIn_Negated`.

**2. The correlated column is absent from the outer projection — a Snowflake limitation, excluded.**
`testIn_Negated_NullValueAndNullInRelation` and `testIn_Negated_InExtend_NullValueAndNullInRelation`
generate a `WHERE` clause **byte-identical** to the passing `testIn_Negated_NullInRelation`; the only
difference is the outer `SELECT` list, which drops `departmentId`:

```sql
-- passes:  select "d_0"."departmentId", "d_0"."departmentName" from departments "d_0" where not exists (...)
-- fails:   select                      "d_0"."departmentName" from departments "d_0" where not exists (...)
--                                                                                  ^ same predicate
```

Referencing a table's column in a correlated subquery without projecting it is ordinary SQL, and
every other dialect runs it. Snowflake will not decorrelate it. This is exactly the failure #5066
recorded ("the one test whose predicate column is not projected") — now reproduced on a completely
different SQL shape, which confirms it was never about the `OR` for those two. Nothing at this layer
can spell around it, so both carry a Snowflake-only exclusion, per the mitigation stated above.

Net on Snowflake: 8 of 10 negated-`in()` tests run, versus 0 before this ADR.

**Provisional third exclusion.** Snowflake cannot be reached from this environment, so the dedupe's
effect on `testSimpleIn_Negated_And` is argued from generated SQL, not observed. It ships together
with a Snowflake exclusion for that test so the next CI run arbitrates: if the dedupe works the run
fails with `Test was expected to fail … but now passes — run with rebase to update the manifest`,
and the entry is removed; if it does not, the entry was correct and stays. Either outcome is a
definite answer, which repeated reasoning about Snowflake's optimizer is not.

## Also fixed here: the three-valued `NOT IN (subquery)` pattern

A full CI run surfaced that the seven `composition::testNotInPattern_*` tests - the **separate**,
three-valued pattern that `fromPure.pure`'s `processNotInSubquery` emits for SQL `NOT IN (subquery)`
- failed on memsql, clickhouse, deephaven and java (all seven), and on Snowflake for two of them.
The Snowflake ones are the interesting case, because Snowflake handles this ADR's own shape fine.

The old pattern folded everything into one `exists()`:

```sql
NOT EXISTS (SELECT 1 FROM employees e
            WHERE d.departmentId IS NULL OR e.department IS NULL OR e.department = d.departmentId)
```

`d.departmentId IS NULL` is an **outer-row constant trapped inside the subquery**, and because it is
`OR`-ed it makes the subquery match every inner row regardless of the join column. There is no
equijoin key to extract, so the subquery cannot be decorrelated into an anti-join. Snowflake refuses
(`Unsupported subquery type cannot be evaluated`; the second test trips an internal-error incident
instead), ClickHouse refuses (`from parent scope only supported for constants and CTE`), and the
engines that *do* run it fall back to a per-outer-row rescan.

`processNotInSubquery` now hoists the loop-invariant parts out, preserving the semantics exactly:

```
(value is not null OR the relation is empty) AND no null in the column AND no matching row
```

```sql
WHERE ("d_0"."departmentId" IS NOT NULL OR NOT EXISTS (SELECT 1 FROM e))  -- OR holds an UNCORRELATED subquery
  AND NOT EXISTS (SELECT 1 FROM e WHERE "e_0"."department" IS NULL)       -- uncorrelated
  AND NOT EXISTS (SELECT 1 FROM e WHERE "e_0"."department" = "d_0"."departmentId")  -- correlated, decorrelatable
```

Top level is a pure conjunction; the single `OR` holds only an uncorrelated subquery; the one
correlated subquery is a plain equality. The empty-relation disjunct is what keeps a null value
surviving an empty relation, which is what SQL answers.

### Measured

A standalone JDBC harness (real tables, 3 reps, best-of) comparing the three formulations. All
return identical row counts, which independently corroborates the rewrite:

| DuckDB, 200k outer x 500k inner | no nulls in column | a null in the column |
|---|---|---|
| native `NOT IN (subquery)` | 56 ms | 16 ms |
| old (OR-correlated `EXISTS`) | **193,848 ms** | **260,332 ms** |
| new (AND-correlated) | **79 ms** | **1.4 ms** |

The old shape is ~2,400x slower in the first case and ~186,000x in the second. The new form actually
beats native `NOT IN` when the column contains a null, because the uncorrelated "any null?" probe
proves the answer is empty and short-circuits the whole query - something the single-`exists()` form
structurally cannot do. (H2 numbers are omitted: all three reported sub-millisecond for 9,876 result
rows, which is not credible and looks like result caching.)

### Remaining follow-up: emit a native `NOT IN`

Native `NOT IN` was fastest in the no-null case, and because its subquery is *uncorrelated* it would
probably also work on ClickHouse (which passes the uncorrelated `testSimpleIn`). Getting it requires
three pieces, the third being the real blocker:

1. A DynaFunction rendered **without** compensation. Both existing renderers -
   `sqlQueryToString::processNotIn` and `toPostgresModel::processNotIn` - append `OR left IS NULL` to
   match Pure's *two-valued* `in()`, which is exactly the subquery-under-`OR` shape #5066 hit.
2. A router leaf emitting it, skipping `excludeNullsFromSearchedColumn` (the two-valued compensation
   on the inner side).
3. A Pure-level trigger. `fromPure.pure` emits Pure, not SQL, and no Pure function maps to a native
   three-valued `NOT IN` - so this needs a new PCT function `meta::pure::functions::relation::notIn`.
   Its Pure body for the in-memory/Java engines would be exactly the formula implemented above.

Two caveats: Pure's `Boolean[1]` cannot represent `UNKNOWN`, so a projected `notIn()` would answer
`false` where SQL answers `NULL` (true of the current implementation too, but a named function makes
it an API promise); and native `NOT IN` is the *worst* option on Postgres, which cannot hash a
nullable `NOT IN` and degrades to O(N x M). So it is a per-dialect optimisation, not a blanket
replacement for the `NOT EXISTS` form.
