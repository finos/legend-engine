# PCT composition tests

Most PCT tests exercise **one** function. A handful exercise the way functions behave **together**,
and those live in dedicated `composition.pure` files. This page is the reference for them, because —
as [Why they are not in the function reference](#why-they-are-not-in-the-function-reference)
explains — they are the one category of PCT test that the generated function documentation cannot
show you.

- [Why composition tests exist](#why-composition-tests-exist)
- [Where they live](#where-they-live)
- [Why they are not in the function reference](#why-they-are-not-in-the-function-reference)
- [Anatomy of a composition test](#anatomy-of-a-composition-test)
- [The relation suite by theme](#the-relation-suite-by-theme)
  - [Operator order](#1-operator-order)
  - [Which SQL clause a filter becomes](#2-which-sql-clause-a-filter-becomes)
  - [Pivot and cast](#3-pivot-and-cast)
  - [Window compositions](#4-window-compositions)
  - [Null semantics](#5-null-semantics)
  - [Variant columns](#6-variant-columns)
  - [Identifiers and aliases](#7-identifiers-and-aliases)
  - [Expression shapes](#8-expression-shapes)
- [The variant suite](#the-variant-suite)
- [The stale `TO FIX` banner](#the-stale-to-fix-banner)
- [Portability](#portability)
- [Adding a composition test](#adding-a-composition-test)

## Why composition tests exist

A store passes a per-function PCT test by implementing that function correctly in isolation. That is
not enough. Relational execution translates a **whole expression tree** into one SQL statement, and
the translation decisions are contextual:

- `filter` becomes `WHERE`, `HAVING`, or `QUALIFY` depending on what precedes it and what it
  references.
- `pivot` produces a column set that is not known until the data is read, which changes the type of
  everything downstream.
- A window aggregate and a filter can commute or not, depending on which columns the filter touches.
- Two adjacent operations may collapse into one `SELECT`, or force a sub-select or a CTE.

Every one of those is a property of the *pair*, not of either function. `groupBy` and `filter` can
both be perfect on their own and still produce wrong SQL when adjacent. Composition tests pin down
the pair.

## Where they live

| File | Tests |
|---|---|
| `core_functions_relation/relation/tests/composition.pure` | 72 |
| `core_functions_variant/functions/tests/composition.pure` | 21 |

Two other files carry the same name but are not PCT suites:
`core_external_query_sql_reverse_pct/relation/composition.pure` records the expected results of
**reverse** (Pure → SQL → Pure) translation for the relation suite, and the pandas reverse-PCT
module holds an equivalent list. When you add or rename a composition test, those files may need the
matching entry.

## Why they are not in the function reference

PCT tests are attached to a function **by source file**. `FunctionsGeneration.addPCTTestToFunctionsDB`
looks up a `FunctionDefinition` keyed on the test's `sourceId`; that map is populated only from
`<<PCT.function>>` declarations. A `composition.pure` declares none, so the lookup misses and the
test is discarded — along with any documentation on it:

```java
FunctionDefinition functionInfo = functionsDB.get(_function.getSourceInformation().getSourceId());
// FunctionDefinition can be null if the PCT Tests are meant to test functions composition.
// Documentation on such a test is dropped here, just as its count is.
if (functionInfo != null)
```

That comment was added by legend-pure's own documentation pass (finos/legend-pure#1305), so this is
a known and accepted gap rather than a bug in our wiring. The effect is visible in the generated
report: `FUNCTIONS_relation.json` contains 46 `functionDefinitions` and **no entry** for
`composition.pure`, and none of the 72 test names appear anywhere in it.

Two practical consequences:

1. **A `'''…'''` literal on a composition test does not publish.** Every test in both files carries
   one, and each parses and lands as a `doc.doc` tagged value that Pure can query and that a reader
   of the source sees immediately — but none of them reach the generated function documentation.
   Until the drop is fixed upstream, the literal is for people reading the `.pure` file and this page
   is for everything cross-cutting: the themes below, the shared conventions, the divergences.
2. **Coverage percentages exclude these tests.** An adapter that fails every composition test still
   reports full coverage of `filter`, `groupBy` and `pivot`.

## Anatomy of a composition test

```pure
'''
The filter runs against the narrowed relation, so it may only name columns that `distinct` kept.
'''
function <<PCT.test, PCTRelationQualifier.relation, PCTRelationQualifier.aggregation>>
meta::pure::functions::relation::tests::composition::test_Distinct_Filter<T|m>(
    f:Function<{Function<{->T[m]}>[1]->T[m]}>[1]):Boolean[1]
{
    let expr = {
               | #TDS
                  val, str, str2
                  2, a, b
                  3, a, b
                #->distinct(~[val, str])->filter(x|$x.val > 2)
              };

    let res =  $f->eval($expr);

    assertEquals( '#TDS\n'+
                  '   val,str\n'+
                  '   3,a\n'+
                  '#', $res->sort(~val->ascending())->toString());
}
```

- **The `'''…'''` literal** says what the test pins down, in a sentence or two — not what it does,
  which the body already shows. See [Why they are not in the function
  reference](#why-they-are-not-in-the-function-reference) for where it does and does not surface.
- **`f` is the adapter.** Each store supplies an evaluator; the test hands it the *unevaluated*
  lambda `$expr` so every store gets the identical expression tree and translates it its own way.
  This is why the pipeline is built inside `{| … }` rather than executed directly.
- **`#TDS … #` is the source relation**, written inline. Columns are untyped unless annotated
  (`payload:meta::pure::metamodel::variant::Variant`, `fromDate:Date[1]`), and `null` is a literal.
- **`->sort(…)` before `->toString()`** is a convention, not part of what is being tested: row order
  out of a relational store is not guaranteed, so the assertion sorts first. A test that omits the
  sort is asserting that the pipeline itself determines the order.
- **`assertTdsEquivalent(expected, actual, tolerance)`** replaces `assertEquals` where floats or
  dates are involved; the trailing numbers are tolerances.
- **Qualifier stereotypes** (`PCTRelationQualifier.relation` / `.aggregation` / `.olap`,
  `PCTCoreQualifier.variant`) classify the test by feature area and are carried into the PCT report.

## The relation suite by theme

### 1. Operator order

The suite is deliberately a matrix: the same two or three operators in each order, so a routing bug
that only shows up in one arrangement is caught.

| Test | Pipeline | What it pins down |
|---|---|---|
| `test_Distinct_GroupBy` | `distinct` → `groupBy` | Deduplication happens before aggregation, so the sums differ from the reverse order |
| `test_GroupBy_Distinct` | `groupBy` → `distinct` | `distinct` on a subset of the grouped keys collapses the aggregate away |
| `test_GroupBy_GroupBy` | `groupBy` → `groupBy` | An aggregate column can itself be aggregated |
| `test_Distinct_Filter` | `distinct` → `filter` | |
| `testProjectDistinct` | `project` → `distinct` | `distinct` sees the *projected* values — two rows differing only in case collapse once `toLower` is applied |
| `test_Extend_GroupBy_Project` / `test_Extend_GroupBy_Extend_Select` | | Renaming a `groupBy` result two ways — via `project`, and via `extend`+`select` |
| `test_Extend_Sort_Project` / `test_Extend_Sort_Extend_Select` | | The same pairing after a `sort` instead |
| `testExtendFilter` | `extend` → `filter` | A filter may reference a column that `extend` just created |
| `testFilterPostProject` | `project` → `filter` | Filtering a projection built from class instances rather than a `#TDS` |
| `testProjectJoinWithProjectProject` | `project` → `join` → `project` | Projections either side of a join, re-projected |

### 2. Which SQL clause a filter becomes

Where a `filter` sits, and which columns it names, decides what it compiles to. Two tests were
written to pin exactly that, and say so in their own documentation:

| Test | Filter references | Recorded intent |
|---|---|---|
| `testExtendWindowFilter` | a window (`over`) output column | *"Filtering on a window column produces qualify grammar rather than a sub-select."* |
| `testGroupByFilterExtendFilter` | a `groupBy` output **and** a window output, in one pipeline | *"A pipeline that filters an aggregate and then filters a window column produces having and qualify grammar together."* |

`test_GroupBy_Filter` is the minimal version of the first half — `groupBy` then a filter on the
aggregate column, nothing else — and `test_GroupBy_Distinct_Filter` and `test_Distinct_GroupBy_Filter`
add a `distinct` on either side of the aggregation.

`testExtendFilterOutNull` is the case that goes the other way, and it is the surprising one — see
[Null semantics](#5-null-semantics).

### 3. Pivot and cast

**Every `pivot` in the suite is followed by `->cast(@Relation<(…)>)`.** This is not decoration.
`pivot` turns row values into columns, so its output type depends on the data and cannot be inferred
at compile time; the `cast` is how you tell the compiler what the shape will be, and without it
nothing downstream can reference a column.

The generated column name is `<pivoted value>__|__<aggregate column>`, and **the name itself
contains quote characters**, which is why the cast has to double them up:

```pure
->pivot(~[year], ~['newCol' : x | $x.treePlanted : y | $y->plus()])
->cast(@Relation<(city:String, country:String, '\'2011__|__newCol\'':Integer)>)
```

`test_Extend_Filter_Select_ComplexGroupBy_Pivot` produces columns named `2011`, `4022` and `6035`,
which looks like corruption and is not. The pipeline groups by city and country while **summing the
year column**, so SAN's two 2011 rows become `4022` and NYC's `2011 + 2012 + 2012` becomes `6035`.
Those sums are then the values that get pivoted into column names. The test is checking that pivot
handles a computed pivot column; the arithmetic is incidental.

| Test | Shape |
|---|---|
| `test_Pivot_Filter` | Dynamic pivot, then filter on a pass-through column |
| `test_Extend_Filter_Select_ComplexGroupBy_Pivot` | The summed-year case above |
| `test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit` | Seven operators; `extend` after the cast |
| `test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort` | Pivot **before** groupBy — the group-by then has to name the generated quoted columns |
| `test_Static_Pivot_Filter` | Static pivot: `pivot(~year, [2000, 2011], …)` fixes the value list up front |
| `test_Project_Filter_Before_Static_Pivot` | Static pivot fed by a projection |
| `testStaticPivot_AfterConcatenate` | Pivot over a `concatenate`; with no grouping key collapsed, each source row stays its own row |
| `testStaticPivot_AfterExtendConcatenate` | The left arm carries a window column, the right arm supplies it as a literal |

### 4. Window compositions

`over(~grp)` with **no sort** frames the whole partition, so the aggregate is the partition total and
identical on every row of that partition — 16 for all three rows of group 1 in
`testOLAPCastAggWithPartitionWindow`. Add an order and the default frame changes; that is a property
of `over` itself and is documented on `over.pure`.

| Test | Pins down |
|---|---|
| `testWindowFunctionsAfterProject` | `lead`/`lag` over a projection built from class instances; the first and last row of each partition come back `null` |
| `testGroupByCastBeforeAgg` / `testGroupByCastAfterAgg` | `cast` is a no-op whether applied to the values or the aggregate result |
| `testOLAPCastAggWithPartitionWindow` and three siblings | The same no-op property in all four positions available in a windowed aggregate |
| `testProjectExtendNestedIfLeadAdjust` | `lead` inside a nested `if`, with `adjust` on dates |
| `testExtendLeadAdjustDerivedOffset` | An `adjust` whose offset is itself read from the `lead` row |

### 5. Null semantics

The most load-bearing cluster in the file. Two things are worth knowing before reading any of it.

**Equality is null-safe.** `null == null` is **true** and `null == 5` is **false** — never unknown.
This is not SQL's three-valued logic, and it is visible directly in the assertions:

- `testFilterEqualOnNullableColumns` keeps the `BothNull` row.
- `testFilterNotEqualOnNullableColumns` keeps `LeftNull` and `RightNull` but drops `BothNull`.
- `testProjectEqualityOnNullableColumns` projects the comparison into a column: every row gets
  `true` or `false`, none get `null`.
- `testJoinOnNullKey` is the consequence — an **INNER** join on `$x.id == $y.id2` matches the two
  null-keyed left rows against the two null-keyed right rows and emits all four combinations. Under
  SQL's default semantics that join would emit nothing for those rows.

Relational execution gets this by emitting null-safe equality rather than a bare `=`; the SQL API can
opt back into three-valued semantics per query with
`Feature.LEGACY_SQL_NULL_UNSAFE_EQUALS` (see `pureToSQLQuery.pure` and
`testLegacyNullUnsafeEquals.pure`).

**`groupBy` gives null its own group.** `testGroupByOnNull` sums the three null-group rows to 12 and
returns `null` as a group key, matching SQL's `GROUP BY`.

| Test | Behaviour asserted |
|---|---|
| `testExtendAddOnNull` | A window `sum` over a partition whose values are **all** null returns `null`, not `0` |
| `testExtendJoinStringOnNull` | `joinStrings` over a window containing nulls; the assertion re-splits and re-sorts the result, so what it pins down is which values survive, not the order they arrive in |
| `testMultiCoalesceInProject` | Multi-argument `coalesce` in a projection, including an all-empty row falling through to the literal default |
| `testCoalesceInPreFilter` | `coalesce` on the left of a comparison inside `filter` |

#### Two divergences that are excluded rather than fixed

`testExtendAddOnNull` and `testExtendFilterOutNull` are excluded on **`core-compiled` and
`core-interpreted`** — the in-memory Pure engines — and the recorded diffs say exactly why.

For `testExtendAddOnNull`, the in-memory engines return `0` where the relational stores return
`null` for a sum over an all-null partition.

`testExtendFilterOutNull` is the more consequential one. The pipeline is
`extend(over(~p), sum(i)) → filter(o->isNotEmpty())`, and the two families disagree about **order**:

| | Partition `p = 100` | Reading |
|---|---|---|
| Relational stores (asserted) | `60` | Filter first, then window — SQL applies `WHERE` before window functions |
| `core-compiled` / `core-interpreted` | `110` | Window first, then filter — the pipeline read literally |

Both readings are defensible; the suite asserts the SQL one. Note the contrast with
`testExtendWindowFilter`, where the filter references the *window output* and therefore cannot be
pushed below it — there the two families agree, and that test is excluded nowhere.

### 6. Variant columns

Fifteen tests covering `Variant`-typed relation columns. The governing rule: **`toMany(@T)` on a
variant holding JSON `null` yields an empty collection**, and every downstream function then behaves
as it does for any empty collection.

| Test | Behaviour |
|---|---|
| `testVariantColumn_isEmpty` / `_isNotEmpty` | `'[]'` and `'null'` are both empty; `'[1]'` is not |
| `testVariantColumn_indexOf` | `-1` when absent, and `-1` for a `'null'` payload |
| `testVariantColumn_contains` | `false` for a `'null'` payload |
| `testVariantArrayColumn_joinStrings` | Empty output for a `'null'` payload |
| `testVariantColumn_slice` | `slice(2,3)` → one element; **negative bounds yield `[]`**, they do not index from the end |
| `testVariantArrayColumn_sort` / `_reverse` | Round-trip through `toMany(@Integer)` and back via `toVariant` |
| `testVariantColumn_distinct_removeDuplicates` | The two deduplication functions agree |
| `testVariant_if` | A variant column as either branch of an `if` |
| `testVariantColumn_extend_indexExtraction_filter` | `get(0)` in an `extend`, then filtered on |
| `testVariantColumn_functionComposition` | The filter predicate calls a user-defined function over the extracted collection |
| `testVariantColumn_modelOutputNotSupported` | `to(@SomeClass)` as a **projected column** is an error — asserted, not worked around |
| `testVariantColumn_projectModelProperty` | …but `to(@SomeClass).name` — projecting a *property* — works |
| `testVariantMapColumn_keys_LateralFlatten` / `_values_LateralFlatten` | `lateral` + `flatten` over `Map<String, Variant>` keys and values, the row-multiplying case |

### 7. Identifiers and aliases

| Test | Pins down |
|---|---|
| `testMixColumnNamesRenameFilter` | Five chained `rename`s over names differing only in case and leading underscores, then a filter referencing all three final names |
| `testMixColumnNamesRenameExtend` | The same with an `extend` and a `select` interleaved |
| `testGroupBy_Conflicting_Alias_With_Table_Columns` | A projection alias colliding with a real column name on the other side of a join |

### 8. Expression shapes

Regression tests for specific code-generation shapes rather than for a semantic rule.

| Test | Shape |
|---|---|
| `testTDSPlusTimesMinus` | Binary `+ * -` inside a `project` |
| `testProjectNumbersPlusTimesMinus` | The variadic `plus([…])` / `times([…])` / `minus([…])` forms over mixed Integer and Float |
| `testFilterArithmeticComparisonExpression` | Arithmetic on both sides of a comparison inside `filter` |
| `testNestedJoinArithmeticComparisonExpression` | A join condition combining an equality with an arithmetic inequality |
| `testProjectOfComputedColumn_withCast` | `cast` narrowing a computed column to `Integer[1]` |
| `testMultiIfTDS` | The `if([pair(…), pair(…)], |default)` multi-branch form |
| `TestJoin_CurrentUserId` | `currentUserId()` inside a join condition — a value the store supplies, not the data |
| `testConcatenateWithJoinOnOneSide` | A `concatenate` whose left arm is a join and whose right arm is not; the two arms reach the CTE with different column representations, which is what the inline comments in the test are about |

## The variant suite

`core_functions_variant/functions/tests/composition.pure` holds 21 tests in two groups.

**Variant collections through platform iteration functions** — `map`, `fold` and `filter` applied to
`toMany(@Variant)` and to `toMany(@Integer)`. Each function is tested both ways because the two take
different paths: `@Variant` keeps every element boxed and needs an explicit `to(@Integer)` inside the
lambda, while `@Integer` converts up front and lets the lambda use plain arithmetic. Both must give
the same answer.

**Variant to model** — `to(@Person)` / `toMany(@Person)` followed by property access. What is being
tested is the *combination*: conversion alone is covered by `to.pure`, but conversion followed by a
nested property, a qualified property, or a `[*]` traversal exercises paths that conversion alone
does not. `testToClassAndAccessNestedProperty_manyToMany` walks `[*] → [*]` and asserts the exact
flattening order.

Three tests cover subtype discrimination: the default `_type` key, a custom key
(`to(@Pet, '__type', [])`), and a custom key with a value-to-class lookup table
(`[pair('gato', Cat), pair('perro', Dog)]`).

The classes these tests need — `Person`, `Address`, `Pet`, `Dog`, `Cat` — are declared
`<<access.private>>` at the bottom of the same file.

## The stale `TO FIX` banner

Line 338 of the relation file reads:

```pure
// ------------------------------------- TO FIX -------------------------------------
```

It has no closing marker, so it visually captures the remaining 60 tests. It does not mean that.
It was added by finos/legend-engine#2979 (`relation: add support for pivot()`) when it sat at the
**end** of the file and covered exactly the three tests written directly beneath it:
`test_GroupBy_Filter`, `test_GroupBy_Distinct_Filter`, `test_Distinct_GroupBy_Filter`. Everything
after them was appended later.

Those three now pass on every relational store — they are excluded only on `java` and `deephaven`,
in both cases with the generic `Instance of type 'meta::pure::metamodel::relation::TDS' can't be
translated` that those adapters apply to essentially every relation test. Their assertions are also
arithmetically correct under standard SQL semantics. The banner is stale; treat it as history, and
do not read the tests under it as recording known-wrong behaviour.

## Portability

How many of the 72 relation composition tests each adapter excludes, from the `*_manifest.json`
files:

| Adapter | Excluded |
|---|---|
| `java` | 71 |
| `deephaven` | 70 |
| `relational-spanner` | 42 |
| `relational-clickhouse` | 34 |
| `relational-sqlserver` | 32 |
| `relational-trino` | 31 |
| `relational-memsql`, `relational-oracle` | 26 |
| `relational-h2`, `relational-postgres` | 23 |
| `relational-databricks` | 14 |
| `core-interpreted` | 4 |
| `core-compiled` | 3 |
| `relational-duckdb`, `relational-snowflake` | 1 |

`java` and `deephaven` exclude nearly the whole suite for one structural reason each
(`Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated`), so their counts say
nothing about the individual tests — `java` runs exactly one, `testCoalesceInPreFilter`. Among the
relational stores the exclusions are mostly feature gaps rather than disagreements: Spanner has no
window columns, ClickHouse has no `concatWithSeparator` aggregate, SQL Server and Trino have no
`joinStrings` in a select position.

When counting these yourself, note that `test_Slice_Size`, `test_Limit_Size` and `test_Distinct_Size`
also sit in the `tests::composition::` **package** but are declared in `size.pure`. Because that file
does declare a `<<PCT.function>>`, they attach to `size` and do publish normally — grepping the
manifests by package name picks them up and overstates the counts above by three.

## Adding a composition test

1. **Only if it is genuinely about a pair.** If one function is wrong, the test belongs in that
   function's own file, where it will be counted and published. `composition.pure` is for behaviour
   that neither function exhibits alone.
2. **Name it for the pipeline**, in order: `test_Extend_Sort_Project`, `testGroupByFilterExtendFilter`.
   The existing names use both `test_A_B` and `testAB` styles; match the neighbours in the section
   you are adding to.
3. **Keep the source `#TDS` minimal** but large enough that the orders being contrasted give
   *different* answers — a composition test whose two orderings agree proves nothing.
4. **Sort before asserting**, unless the ordering is the thing under test.
5. **Tag it** with the right `PCTRelationQualifier` stereotypes.
6. **Give it a `'''…'''` literal**, like every one of its neighbours — one or two sentences saying
   what the test pins down, not what it does. Add it to the matching theme table here too; the
   literal will not publish, so this page is where a reader outside the file will find it.
7. **Run it, then record honest exclusions** per
   [expected-failures-howto.md](expected-failures-howto.md). The runner prints a copy-paste snippet
   on failure.
8. **Check the reverse-PCT files** (`core_external_query_sql_reverse_pct`, and the pandas equivalent)
   for whether an entry is needed.

## See also

- [Writing PCT function documentation](pct-documentation.md) — the `'''…'''` standard for function
  signatures and for tests that *do* publish.
- [PCT conventions](conventions.md) — naming, file layout, one function per file.
- [Expected failures](expected-failures-howto.md) — how to record an exclusion.
- [PCT overview](overview.md) and [taxonomy](taxonomy.md).
