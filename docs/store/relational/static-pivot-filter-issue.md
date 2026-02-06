# Static Pivot Filter Condition Issue

## Overview

In the `pureToSQL` transformation for static pivot operations, filter conditions applied before the pivot are not being properly retained. This results in incorrect query results where the pre-pivot filtering is ignored.

## Problem Description

### Affected Test Case

The issue is demonstrated in the test `test_Static_Pivot_Project_Filter` located in:
```
legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/
legend-engine-pure-functions-relation-pure/src/main/resources/core_functions_relation/
relation/tests/composition.pure
```

### Example Query

```pure
#TDS
    city, country, year, treePlanted
    NYC, USA, 2011, 5000
    NYC, USA, 2000, 5000
    SAN, USA, 2000, 2000
    SAN, USA, 2011, 100
    LDN, UK, 2011, 3000
    SAN, USA, 2011, 2500
    NYC, USA, 2000, 10000
    NYC, USA, 2012, 7600
    NYC, USA, 2012, 7600
#
->project(~[newCity:c|$c.city->toOne(), newCountry:c|$c.country->toOne(), 
            newYear:c|$c.year->toOne(), newTreePlanted:c|$c.treePlanted->toOne()])
->filter(x|$x.newCity=='NYC')
->pivot(~newYear, ~'newCol' : x | $x.newTreePlanted : y | $y->plus())
```

**Expected Result:**
Only rows where `newCity == 'NYC'` should be included in the pivot aggregation.

```
newCity,newCountry,'2000__|__newCol','2011__|__newCol','2012__|__newCol'
NYC,USA,15000,5000,15200
```

**Actual Result:**
The filter condition `->filter(x|$x.newCity=='NYC')` is being ignored, and all rows are included in the pivot operation.

## Root Cause

### Implementation Location

The issue exists in the `processStaticPivot` function in:
```
legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/
legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/
src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure
```

### Technical Analysis

In the `processStaticPivot` function (starting around line 6088), the code does the following:

1. **Processes the input query** including any filters from previous operations:
   ```pure
   let inputQuery = processValueSpecification($expression.parametersValues->at(0), ...)
   let inputSelect = $inputQuery.select->cast(@TdsSelectSqlQuery);
   ```

2. **Extracts existing filter from inputSelect**:
   ```pure
   let existingFilter = $inputSelect.filteringOperation;
   ```

3. **Creates a new filter for pivot values** and combines with existing filter:
   ```pure
   let filteringOp = ^DynaFunction(name = 'in', parameters = [$matchedPivotColumnName, ^LiteralList(values = $staticPivotValues)]);
   let combinedFilter = if($existingFilter->isEmpty(),
                           |$filteringOp,
                           |^DynaFunction(name = 'and', parameters = [$existingFilter->toOne(), $filteringOp]));
   ```

4. **Creates filtered input select**:
   ```pure
   let filteredInputSelect = ^$inputSelect(filteringOperation = $combinedFilter);
   ```

5. **Problem**: The filter is added to the `filteredInputSelect`, but then the code creates a new `TdsSelectSqlQuery` at the end that uses `StaticPivot` structure. The issue is that **the filtering operation from the input query may not be properly propagated through the SQL generation when the pivot is rendered**.

The static pivot SQL generation creates a structure where:
- The input query with filters becomes a subquery
- The pivot operation is applied
- However, the filter conditions from before the pivot may not be included in the generated SQL properly

### Comparison with Dynamic Pivot

The dynamic pivot implementation (in `processDynamicPivot` around line 6145) handles this differently:
- It uses `let inputTableAlias = ^TableAlias(name = $inputQueryAlias, relationalElement=$inputSelect);`
- It directly uses the `$inputSelect` which already contains the filtering operations
- The input query is treated as a proper subquery with all its conditions intact

## Workarounds

### Workaround 1: Add a `limit` operation

Adding a `limit` operation after the filter but before the pivot forces the query to be properly materialized:

```pure
->filter(x|$x.newCity=='NYC')
->limit(1000000)  // Forces materialization
->pivot(~newYear, ~'newCol' : x | $x.newTreePlanted : y | $y->plus())
```

### Workaround 2: Use Dynamic Pivot

Dynamic pivot does not have this issue. If the pivot values are known, they can still be specified, but using the dynamic pivot syntax:

```pure
->filter(x|$x.newCity=='NYC')
->pivot(~[newYear], ~['newCol' : x | $x.newTreePlanted : y | $y->plus()])
```

## Impact

- **Correctness**: Query results are incorrect when filters are applied before static pivot
- **Data Integrity**: Aggregations include rows that should have been filtered out
- **Silent Failure**: The query executes successfully but produces wrong results without error
- **Scope**: Affects all static pivot operations with preceding filter conditions in SQL generation

## Recommended Solution

The fix should ensure that when `processStaticPivot` creates the final `TdsSelectSqlQuery`, it properly preserves the filter conditions from the input query. This likely involves:

1. Ensuring the `filteredInputSelect` is used as a proper subquery in the data source
2. Verifying that the `RootJoinTreeNode` properly references the filtered input
3. Potentially wrapping the input query similar to how dynamic pivot handles it

The implementation should align with the dynamic pivot approach where the input query (with all its filters) is treated as a complete subquery before the pivot operation is applied.

## Related Code Locations

- **Static Pivot Implementation**: `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` (line ~6088)
- **Dynamic Pivot Implementation**: Same file (line ~6145)
- **Test Case**: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources/core_functions_relation/relation/tests/composition.pure` (line ~1404)
- **Pivot Function Definition**: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources/core_functions_relation/relation/functions/transformation/pivot.pure`

## Status

**Known Issue** - Documented but not yet fixed.

---

*Document created: February 4, 2026*

