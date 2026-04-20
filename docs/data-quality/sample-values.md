# Sample Values Profiling

## Overview

The **sample values** feature produces a ranked-frequency table for every column in a relation.
The most frequent values (up to `maxNumberOfSampleValues`, default **20**) are returned together
with their occurrence count.  Each value is placed in a sparse TDS column matching its type.

The caller supplies a relation query either as:

* an **inline `LambdaFunction`** (the `query` field), or
* a **path to a `ConcreteFunctionDefinition`** that returns a `Relation` (the `functionPath` field).

---

## Output Shape

The result is a single TDS with a fixed set of columns.  There is **one row per (column, value) pair**, not one row per column.

| Column | Type | Description |
|--------|------|-------------|
| `column_name` | `String` | Name of the source relation column |
| `column_data_type` | `String` | Pure type name (`String`, `Integer`, `Float`, `StrictDate`, `Boolean`, …) |
| `count` | `Integer` | Number of rows with this value |
| `string_value` | `String` &#124; null | Populated when the column type is `String`; null otherwise |
| `int_value` | `Integer` &#124; null | Populated when the column type is `Integer`; null otherwise |
| `float_value` | `Float` &#124; null | Populated when the column type is `Float`, `Decimal`, or any non-integer `Number`; null otherwise |
| `date_value` | `StrictDate` &#124; null | Populated when the column type is `StrictDate`, `Date`, or `DateTime`; null otherwise |
| `boolean_value` | `Boolean` &#124; null | Populated when the column type is `Boolean`; null otherwise |

**Example:**

```
#TDS
column_name, column_data_type, count, string_value, int_value, float_value, date_value, boolean_value
myCol1,      String,           10,    myValue1,     null,      null,        null,       null
myCol1,      String,           5,     myValue2,     null,      null,        null,       null
myCol1,      String,           1,     null,         null,      null,        null,       null
myCol2,      Integer,          15,    null,         888,       null,        null,       null
myCol2,      Integer,          4,     null,         555,       null,        null,       null
#
```

### Key design points

* Rows are ordered by **relation column order** first, then **descending by count** within each
  column block.
* A row where all value columns are null represents the **null/missing count** for that column; it
  appears only if the null count ranks within the top N.
* Only the top `maxNumberOfSampleValues` rows per column are returned.
* The result is produced as a single query built as a **UNION ALL** of per-column sub-queries,
  mirroring the pattern used by the existing data-profile lambda generator.
* `DateTime` columns map to `date_value` — no separate column is needed.
* All non-integer numeric types (`Float`, `Decimal`, `Number`) map to `float_value`.

---

## REST API

The endpoint is available at `POST /pure/v1/dataquality/sampleValues` - this executes the sample-values query and returns a TDS result.

### Request body — `DataQualitySampleValuesInput`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `clientVersion` | `String` | yes | Protocol version (e.g. `vX_X_X`) |
| `model` | `PureModelContext` | yes | Model context (pointer or data) |
| `query` | `LambdaFunction` | one of `query` / `functionPath` | Inline relation query ending with `->from(…)` |
| `functionPath` | `String` | one of `query` / `functionPath` | Fully-qualified path to a `ConcreteFunctionDefinition` that returns a `Relation` |
| `maxNumberOfSampleValues` | `Integer` | no | Max rows per column (default **20**) |
| `lambdaParameterValues` | `List<ParameterValue>` | no | Parameter bindings for the query |

> `query` and `functionPath` are **mutually exclusive** — supply exactly one.

---

## See Also

* [Data Quality Overview](data-quality-overview.md)
* Source: `core_dataquality/generation/samplevalues.pure`

