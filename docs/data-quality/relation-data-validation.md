# RelationDataValidation User Guide

## Overview

`DataQualityRelationValidation` is a Legend feature that lets you define and execute data quality
checks against **tabular / relational data** — tables, views, or any query that returns a
`Relation` (TDS).  Validations are declared in the `###DataQualityValidation` section of a
Legend model and are fully executable: Legend compiles each rule into an optimised SQL query
that can be run on demand or scheduled.

### When to use RelationDataValidation

| Approach | Best suited for |
|---|---|
| [Model Constraints](./data-quality-overview.md#model-constraints) | Per-instance business rules tied to a class |
| [Service Post Validations](./service-post-validations.md) | Ad-hoc assertions on the result of a Legend Service |
| **RelationDataValidation** | Reusable, named rule-sets attached to a relation/table query |

`RelationDataValidation` is particularly useful when:

* You want to ship data quality rules alongside the data model, versioned together.
* Rules need to operate on aggregate or dataset-level properties (e.g. uniqueness, row count).
* You need to cross-reference two datasets to check referential integrity.
* You want a named, reusable catalogue of rules that can be triggered independently.

### Validation types

All validations follow the same pattern: the assertion receives the **full relation**, applies
`filter`, `groupBy`, `join`, or other relation operations to isolate the violating rows, then
terminates with `meta::external::dataquality::assertRelationEmpty(~[...])`.

`assertRelationEmpty` is not just a check — it is what **produces the defect rows** in the
output.  Without it, no defect is recorded regardless of what the preceding operations return.
The columns passed to it are projected into each defect row so that failures can be
investigated without needing a follow-up query.

---

## Structure of a DataQualityRelationValidation

```legend
###DataQualityValidation
DataQualityRelationValidation my::package::MyValidation
{
  // The query that produces the dataset to validate.
  // Can reference any table/view via the #>{...}# relation accessor.
  query: |#>{my::db.myTable}#->select(~[COL1, COL2])->from(my::Runtime);

  // One or more named validation rules — name must be unique within the list
  validations: [
    {
       name: 'myRule';          // must be unique within this validations list
       description: 'Human-readable description of the rule';
       // assertion lambda — receives the full relation
       assertion: rel | $rel->filter(row | $row.COL1->isEmpty())->meta::external::dataquality::assertRelationEmpty(~[COL1]);
    }
  ];
}
```

> **Note:** the `name` field must be **unique within the `validations` list** of a single
> `DataQualityRelationValidation`.  It is used as the value of the `DQ_RULE_NAME` metadata
> column in the defect output and as the key when selecting individual rules to execute via
> the API (see [Executing a validation](#executing-a-validation) below).

---

## Examples

The examples below all use a fictional **Orders** domain:

```legend
###Relational
Database trading::db
(
  Table OrderTable
  (
    ORDER_ID    INTEGER PRIMARY KEY,
    ORDER_TYPE  VARCHAR(20),      // e.g. 'MARKET', 'LIMIT'
    PRICE       DECIMAL(18,4),    // nullable — not present for MARKET orders
    QUANTITY    INTEGER,
    USER_ID     INTEGER,
    STATUS      VARCHAR(20)
  )

  Table EmployeeTable
  (
    EMPLOYEE_ID INTEGER PRIMARY KEY,
    NAME        VARCHAR(200),
    DEPARTMENT  VARCHAR(100)
  )
)

###Runtime
Runtime trading::TradeRuntime
{
  mappings: [];
  connections:
  [
    trading::db:
    [
      id: trading::connection::TradeConnection
    ]
  ];
}
```

---

### a) Row-level validation — price must always be above 100

> **Code example:** [`testLambdaGeneration_relationValidation_row_level`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L178)

A **row-level** rule filters the relation down to rows that violate the condition and asserts
that filtered set is empty.  The assertion receives the full relation bound to `rel`, and a
`filter` expression checks each row.

```legend
###DataQualityValidation
DataQualityRelationValidation trading::OrderPriceValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, ORDER_TYPE, PRICE, QUANTITY])
           ->from(trading::TradeRuntime);

  validations: [
    {
       name: 'priceMustBeAbove100';
       description: 'Every order price must be greater than 100';
       // Keep rows that violate the rule, then assert none exist
       // PRICE is nullable — treat a missing price as a violation (isEmpty) as well as price <= 100
       assertion: rel | $rel
           ->filter(row | $row.PRICE->isEmpty() || $row.PRICE->toOne() <= 100)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE]);
    }
  ];
}
```

**How it works:** The `filter` selects rows where price is missing or ≤ 100.  Because `PRICE`
is a nullable column, the `->isEmpty()` check must come first — calling `->toOne()` on a null
value would throw at runtime.  `assertRelationEmpty` then verifies that no such rows exist.
The column array `~[ORDER_ID, PRICE]` specifies which columns are included in the defect
output — include the table's primary key so each defect can be uniquely identified, plus the
column(s) involved in the check so the offending value is immediately visible.

**Useful built-in helpers** (from `meta::external::dataquality`) that already encapsulate the
filter logic and return the violating sub-relation:

> **Code examples:** [`rowsWithEmptyColumnTest`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L109), [`rowsWithNegativeValueTest`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L156), [`rowsWithValueOutsideRangeTest`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L140), [`rowsWithColumnLongerThanTest`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L171), [`rowsWithColumnDiffersFromPatternTest`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L187)

```legend
// Rows where a column is empty / null
$rel->meta::external::dataquality::rowsWithEmptyColumn(~PRICE)

// Rows where a numeric column is negative or zero
$rel->meta::external::dataquality::rowsWithNegativeValue(~QUANTITY)

// Rows where value falls outside [min, max]
$rel->meta::external::dataquality::rowsWithValueOutsideRange(~PRICE, 100, 10000)

// Rows where a string column exceeds a given length
$rel->meta::external::dataquality::rowsWithColumnLongerThan(~ORDER_TYPE, 20)

// Rows where a string column does not match a regex pattern
$rel->meta::external::dataquality::rowsWithColumnDiffersFromPattern(~ORDER_TYPE, '[A-Z]+')
```

Example using a helper:

```legend
    {
       name: 'priceNotNegative';
       description: 'Price must be positive';
       assertion: rel | $rel
           ->meta::external::dataquality::rowsWithNegativeValue(~PRICE)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE]);
    }
```

---

### b) Conditional validation across columns — if order type is LIMIT, price must be specified

> **Code example:** [`testLambdaGeneration_relationValidation_withLimit`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L530)

For conditional rules, use an `if` expression inside the `filter` so that only relevant rows
are checked:

```legend
###DataQualityValidation
DataQualityRelationValidation trading::LimitOrderValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, ORDER_TYPE, PRICE])
           ->from(trading::TradeRuntime);

  validations: [
    {
       name: 'limitOrderMustHavePrice';
       description: 'If the order type is LIMIT, the price must be specified (not null)';
       // Filter to LIMIT orders that are missing a price — assert none exist
       assertion: rel | $rel
           ->filter(row | $row.ORDER_TYPE == 'LIMIT' && $row.PRICE->isEmpty())
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE]);
    }
  ];
}
```

You can combine multiple conditional checks in the same rule:

```legend
    {
       name: 'limitOrderPriceAndQuantityValid';
       description: 'LIMIT orders require both price > 0 and quantity > 0';
       assertion: rel | $rel
           ->filter(row | $row.ORDER_TYPE == 'LIMIT'
                            && !($row.PRICE->isNotEmpty() && $row.PRICE->toOne() > 0
                                   && $row.QUANTITY->isNotEmpty() && $row.QUANTITY->toOne() > 0))
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE, QUANTITY]);
    }
```

---

### c) Aggregate / dataset-level validation — a column must be distinct

> **Code examples:** [`testLambdaGeneration_relationValidation_aggregate_withTotalDefectCountAndLimit`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L397), [`assertRelationEmptyWhenEmpty`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_relation_helper_test.pure#L89)

For dataset-level rules the assertion operates on the **entire relation** — no per-row filter
needed.  The following example checks that `ORDER_ID` is unique across all rows:

```legend
###DataQualityValidation
DataQualityRelationValidation trading::OrderDatasetValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, ORDER_TYPE, STATUS])
           ->from(trading::TradeRuntime);

  validations: [
    {
       name: 'orderIdMustBeDistinct';
       description: 'ORDER_ID must be unique — no duplicate order IDs allowed';
       // Group by ORDER_ID, keep groups with more than one row, then assert none exist
       assertion: rel | $rel
           ->groupBy(~ORDER_ID, ~[duplicateCount: x | 1 : y | $y->count()])
           ->filter(row | $row.duplicateCount > 1)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, duplicateCount]);
    },

    {
       name: 'datasetNotEmpty';
       description: 'The order dataset must contain at least one row';
       // Aggregate the total row count, keep it if it is zero,
       // then assert the result is empty — the defect row will contain the actual count
       assertion: rel | $rel
           ->aggregate(~rowCount: x | 1 : y | $y->count())
           ->filter(row | $row.rowCount == 0)
           ->meta::external::dataquality::assertRelationEmpty(~[rowCount]);
    },

    {
       name: 'datasetRowCountWithinExpectedRange';
       description: 'Expect between 1 000 and 1 000 000 orders per run';
       // Aggregate the total row count, keep it if it falls outside the expected range,
       // then assert the result is empty — the defect row will contain the actual count
       assertion: rel | $rel
           ->aggregate(~rowCount: x | 1 : y | $y->count())
           ->filter(row | $row.rowCount < 1000 || $row.rowCount > 1000000)
           ->meta::external::dataquality::assertRelationEmpty(~[rowCount]);
    },

    {
       name: 'noInvalidStatus';
       description: 'No rows should have an unrecognised order status';
       assertion: rel | $rel
           ->filter(row | !['ACTIVE', 'CANCELLED', 'FILLED']->contains($row.STATUS))
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, STATUS]);
    }
  ];
}
```

#### Assert function (from `meta::external::dataquality`)

Every assertion must terminate with `assertRelationEmpty`.  It is responsible for
**producing the defect rows** in the output — the preceding `filter`, `groupBy`, or `join`
operations narrow the relation down to violations, but it is `assertRelationEmpty` that
emits each of those rows as a defect record, projected to the specified columns:

```legend
->meta::external::dataquality::assertRelationEmpty(~[COL1, COL2, ...])
```

The column list is **required** — the compiler will reject a call without it.

#### Choosing columns for `assertRelationEmpty`

The column array passed to `assertRelationEmpty(~[...])` controls which columns are included
in the defect output rows returned to the caller.  They have no effect on whether a row is
flagged as a defect — that is determined entirely by the `filter` / `groupBy` logic before it.
The columns are **required** — the compiler will reject a call to `assertRelationEmpty` without them.

As a guideline, include:

* **The primary key column(s)** of the table so that each defect row can be uniquely identified
  and looked up in the source system.
* **The column(s) involved in the check** so that the actual offending value is visible without
  needing a follow-up query.

For example, a price check on `OrderTable` should include `ORDER_ID` (PK) and `PRICE` (the
checked column):

```legend
->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE])
```

A conditional check across multiple columns should include all of them:

```legend
->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE])
```

---

### d) Cross-dataset join — validate a FK / PK relationship

> **Code example:** [`testLambdaGeneration_relationValidation_all_validations`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L718)

To check that every `USER_ID` in the `OrderTable` exists in the `EmployeeTable`, use a
left join inside the assertion.  A left join followed by filtering for null on the right-side
key column implements a left-anti join — returning only the orphaned FK values.

```legend
###DataQualityValidation
DataQualityRelationValidation trading::OrderReferentialIntegrityValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, USER_ID])
           ->from(trading::TradeRuntime);

  validations: [
    {
       name: 'userIdMustExistInEmployeeTable';
       description: 'Every USER_ID on an order must exist as an EMPLOYEE_ID in the employee table';
       assertion: rel | $rel
           ->join(
               // Reference the lookup / dimension table directly
               #>{trading::db.EmployeeTable}#
                 ->select(~[EMPLOYEE_ID])
                 ->from(trading::TradeRuntime),
               // LEFT join — orders without a matching employee will have null EMPLOYEE_ID
               meta::pure::functions::relation::JoinKind.LEFT,
               // Join condition: order USER_ID matches employee EMPLOYEE_ID
               {order, emp | $order.USER_ID == $emp.EMPLOYEE_ID}
           )
           // Keep only rows with NO match in the employee table (null right-side key)
           ->filter(row | $row.EMPLOYEE_ID->isEmpty())
           // Project the FK columns into the defect output, then assert none exist
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, USER_ID]);
    }
  ];
}
```

#### How the left-anti join pattern works

```
OrderTable                     EmployeeTable
ORDER_ID | USER_ID             EMPLOYEE_ID | NAME
---------|--------             ------------|------
1001     | 42          ──────►  42         | Alice
1002     | 99          ──────►  ✗  (no match → EMPLOYEE_ID is null → flagged as defect)
1003     | 42          ──────►  42         | Alice
```

---

### e) Using `#SQL{...}#` in the query or assertion

> **Code examples:** [`testTable`](../../legend-engine-xts-sql/legend-engine-xt-sql-expression/legend-engine-xt-sql-expression-compiler/src/test/java/org/finos/legend/engine/sql/compiler/test/TestCompilerFromGrammar.java#L108), [`testJoin`](../../legend-engine-xts-sql/legend-engine-xt-sql-expression/legend-engine-xt-sql-expression-compiler/src/test/java/org/finos/legend/engine/sql/compiler/test/TestCompilerFromGrammar.java#L122)

As an alternative to the Pure relation API (`#>{...}#->select(...)->filter(...)` etc.), you
can write raw SQL using the `#SQL{...}#` expression.  The SQL block compiles to a `Relation`
and can be used anywhere a relation is expected — in the `query` field, in an `assertion`
lambda, or both.

Inside the SQL block, use `tb('package::DB.TableName')` to reference a model table, and
`var('name')` to reference a Pure variable that is in scope (e.g. the `rel` parameter of an
assertion lambda).

#### SQL in the `query`

Useful when the dataset to validate needs non-trivial SQL that would be verbose to express
in the Pure relation API (e.g. `GROUP BY`, `CASE`, computed columns):

```legend
###DataQualityValidation
DataQualityRelationValidation trading::OrderSqlQueryValidation
{
  // SQL query — use tb('...') to reference a model table
  query: |#SQL{
      SELECT ORDER_ID, ORDER_TYPE, PRICE, QUANTITY, USER_ID, STATUS
      FROM tb('trading::db.OrderTable')
      WHERE STATUS != 'CANCELLED'
    }#->from(trading::TradeRuntime);

  validations: [
    {
       name: 'priceMustBeAbove100';
       description: 'Every active order price must be greater than 100';
       assertion: rel | $rel
           ->filter(row | $row.PRICE->isEmpty() || $row.PRICE->toOne() <= 100)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE]);
    }
  ];
}
```

#### SQL in the `assertion`

Useful when the check itself is more naturally expressed in SQL.  Use `var('rel')` to
reference the relation passed into the assertion lambda:

```legend
###DataQualityValidation
DataQualityRelationValidation trading::OrderSqlAssertionValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, ORDER_TYPE, PRICE, QUANTITY])
           ->from(trading::TradeRuntime);

  validations: [
    {
       name: 'priceMustBeAbove100';
       description: 'Every order price must be greater than 100';
       // Use var('rel') inside the SQL block to reference the assertion lambda parameter
       assertion: rel | #SQL{
           SELECT ORDER_ID, PRICE
           FROM var('rel')
           WHERE PRICE IS NULL OR PRICE <= 100
         }#->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE]);
    },
    {
       name: 'orderIdMustBeDistinct';
       description: 'ORDER_ID must be unique';
       assertion: rel | #SQL{
           SELECT ORDER_ID, COUNT(*) AS duplicateCount
           FROM var('rel')
           GROUP BY ORDER_ID
           HAVING COUNT(*) > 1
         }#->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, duplicateCount]);
    }
  ];
}
```

#### SQL in both `query` and `assertion`

```legend
###DataQualityValidation
DataQualityRelationValidation trading::FullSqlValidation
{
  query: |#SQL{
      SELECT ORDER_ID, ORDER_TYPE, PRICE, USER_ID
      FROM tb('trading::db.OrderTable')
    }#->from(trading::TradeRuntime);

  validations: [
    {
       name: 'limitOrderMustHavePrice';
       description: 'LIMIT orders must have a price specified';
       assertion: rel | #SQL{
           SELECT ORDER_ID, ORDER_TYPE, PRICE
           FROM var('rel')
           WHERE ORDER_TYPE = 'LIMIT' AND PRICE IS NULL
         }#->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE]);
    }
  ];
}
```

---

## Combining multiple rules in one definition

> **Code examples:** [`testLambdaGeneration_relationValidation_all_validations`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L718), [`testLambdaGeneration_relationValidation_some_validations`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L673)

```legend
###DataQualityValidation
DataQualityRelationValidation trading::ComprehensiveOrderValidation
{
  query: |#>{trading::db.OrderTable}#
           ->select(~[ORDER_ID, ORDER_TYPE, PRICE, QUANTITY, USER_ID, STATUS])
           ->from(trading::TradeRuntime);


  validations: [
    {
       name: 'priceMustBeAbove100';
       description: 'Every order price must be greater than 100';
       assertion: rel | $rel
           ->filter(row | $row.PRICE->isEmpty() || $row.PRICE->toOne() <= 100)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, PRICE]);
    },
    {
       name: 'limitOrderMustHavePrice';
       description: 'If order type is LIMIT, price must be specified';
       assertion: rel | $rel
           ->filter(row | $row.ORDER_TYPE == 'LIMIT' && $row.PRICE->isEmpty())
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE]);
    },
    {
       name: 'orderIdMustBeDistinct';
       description: 'ORDER_ID values must be unique';
       assertion: rel | $rel
           ->groupBy(~ORDER_ID, ~[cnt: x | 1 : y | $y->count()])
           ->filter(row | $row.cnt > 1)
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, cnt]);
    },
    {
       name: 'userIdMustExistInEmployeeTable';
       description: 'USER_ID must reference a valid employee';
       assertion: rel | $rel
           ->join(
               #>{trading::db.EmployeeTable}#->select(~[EMPLOYEE_ID])->from(trading::TradeRuntime),
               meta::pure::functions::relation::JoinKind.LEFT,
               {o, e | $o.USER_ID == $e.EMPLOYEE_ID}
           )
           ->filter(row | $row.EMPLOYEE_ID->isEmpty())
           ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, USER_ID]);
    }
  ];
}
```

---

## Parameterised validations

> **Code examples:** [`testLambdaGeneration_parameterized_relationValidation_row_level`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L234), [`testLambdaGeneration_relationValidation_withParametersInAssertion`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/dataquality_test.pure#L503)

Both the `query` and individual `assertion` lambdas can accept parameters.  At invocation
time the same set of values is passed to both, so **any parameter declared in the assertion
must also be declared in the query**.  The assertion does not need to declare all of the
query's parameters — only the ones it actually uses.  `rel` is always the **last** parameter
in the assertion lambda:

```legend
###DataQualityValidation
DataQualityRelationValidation trading::ParameterisedOrderValidation
{
  // Query declares both parameters: asOfDate (used to filter rows) and minPrice
  query: {asOfDate: Date[1], minPrice: Decimal[1] |
            #>{trading::db.OrderTable}#
              ->select(~[ORDER_ID, ORDER_TYPE, PRICE, QUANTITY])
              ->filter(row | $row.TRADE_DATE == $asOfDate)
              ->from(trading::TradeRuntime)};

  validations: [
    {
       name: 'limitOrderMustHavePriceAboveThreshold';
       description: 'LIMIT orders must have price above the supplied threshold';
       // Assertion only needs minPrice — rel is declared last
       assertion: {minPrice: Decimal[1], rel |
                     $rel
                         ->filter(row | $row.ORDER_TYPE == 'LIMIT'
                                          && ($row.PRICE->isEmpty() || $row.PRICE->toOne() <= $minPrice))
                         ->meta::external::dataquality::assertRelationEmpty(~[ORDER_ID, ORDER_TYPE, PRICE])};
    }
  ];
}
```

---

## Execution with Engine APIs

### Output schema

Each executed validation returns a relation with the columns selected by the validation query
plus the following DQ metadata columns:

| Column | Type | Description |
|---|---|---|
| `DQ_RULE_NAME` | `String` | Name of the rule that produced this defect |
| `DQ_LOGICAL_DEFECT_ID` | `String` | MD5 hash of the defect row values (stable across runs — useful for de-duplication) |
| `DQ_DEFECT_ID` | `String` | GUID unique to this execution (useful for audit trails) |

When `defectsLimit` is set and `includeTotalDefectCount` is enabled, an additional
`DEFECT_COUNT` column is included with the total count before the limit was applied.


### Endpoint

```
POST /api/pure/v1/dataquality/execute
Content-Type: application/json
```

### Request body (`DataQualityExecuteTrialInput`)

| Field | Type | Required | Description |
|---|---|---|---|
| `model` | `PureModelContext` | ✓ | The full model context containing the `DataQualityRelationValidation` element and its dependencies |
| `packagePath` | `String` | ✓ | Fully-qualified path of the `DataQualityRelationValidation` to execute (e.g. `trading::OrderPriceValidation`) |
| `validationNames` | `Set<String>` | | Names of specific rules to run.  If empty or omitted, **all** rules in the validation are executed |
| `lambdaParameterValues` | `List<ParameterValue>` | | Parameter values for parameterised `query` or `assertion` lambdas (see [Parameterised validations](#parameterised-validations)) |
| `defectsLimit` | `Integer` | | Cap on the number of defect rows returned per rule.  If omitted, all defects are returned |
| `includeTotalDefectCount` | `Boolean` | | When `true`, adds a `DEFECT_COUNT` column with the total count of defects before the `defectsLimit` was applied.  Defaults to `false` |
| `enrichDQColumns` | `Boolean` | | When `true` (default), the response includes the `DQ_RULE_NAME`, `DQ_LOGICAL_DEFECT_ID`, and `DQ_DEFECT_ID` metadata columns.  Set to `false` to suppress them |

### Example: run all rules

```json
{
  "model": { ... },
  "packagePath": "trading::ComprehensiveOrderValidation"
}
```

### Example: run a single named rule

Use `validationNames` to run only a subset of the rules defined in the validation:

```json
{
  "model": { ... },
  "packagePath": "trading::ComprehensiveOrderValidation",
  "validationNames": ["priceMustBeAbove100"]
}
```

### Example: run with a defect cap and total count

```json
{
  "model": { ... },
  "packagePath": "trading::ComprehensiveOrderValidation",
  "defectsLimit": 100,
  "includeTotalDefectCount": true
}
```

### Example: run a parameterised validation

Supply values for any parameters declared in the `query` or `assertion` lambdas:

```json
{
  "model": { ... },
  "packagePath": "trading::ParameterisedOrderValidation",
  "lambdaParameterValues": [
    { "name": "asOfDate",  "value": { "_type": "dateValue",    "value": "2026-05-15" } },
    { "name": "minPrice",  "value": { "_type": "decimalValue", "value": 50.0 } }
  ]
}
```

### Defect-count-only endpoint

To retrieve only the **number** of defects per rule without fetching the full defect rows, use:

```
POST /api/pure/v1/dataquality/execute/relation-validation/rowcount
```

The request body is identical to the main execute endpoint.

---

## Further reading

* [Data Quality Overview](./data-quality-overview.md)
* [Service Post Validations](./service-post-validations.md)
* [Sample Values](./sample-values.md)

