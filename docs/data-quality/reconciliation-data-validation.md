# Reconciliation Data Validation User Guide

## Overview

**Reconciliation** (recon) is a data quality technique for comparing two datasets — a
**source** and a **target** — to identify rows that differ between them.  

For more efficient execution across distributed database, rather than
comparing every column value directly and potentially moving large amounds of raw data,
Legend generates an **MD5 hash** of the selected
columns for each row and then performs a **full outer join** on the key columns, returning
only the rows where the source and target hashes differ.

Typical use cases:

* Verify that a downstream copy or transformation of a table matches the upstream original.
* Confirm that two independent systems that should hold the same data are in sync.
* Validate that a migration or ETL job has not silently dropped or corrupted rows.

---

## How it works

For each dataset (source and target) Legend automatically:

1. Selects only the columns needed for hashing and key joining.
2. Normalises column values by data type (e.g. floats to IEEE 754, dates to ISO-8601) so
   that equivalent values in different representations are treated as equal.
3. Computes an MD5 **row hash** (`DIGEST_SOURCE` / `DIGEST_TARGET`) across the selected
   columns, or uses a **pre-computed hash column** if one already exists on the dataset.
4. Optionally **aggregates** multiple row hashes into a single hash per key group
   (for group-level reconciliation).
5. Performs a **full outer join** on the key columns and filters to rows where
   `DIGEST_SOURCE != DIGEST_TARGET` — these are the defects.

---


## Structure of a DataQualityRelationComparison

```legend
###DataQualityValidation
DataQualityRelationComparison my::package::MyRecon
{
   // Queries returning the datasets to compare — must produce a Relation
   source: |#>{my::db.sourceTable}#->from(my::SourceRuntime);
   target: |#>{my::db.targetTable}#->from(my::TargetRuntime);

   // Column(s) used as the join key — must exist on both datasets
   keys: [id];

   // Optional: restrict which columns are hashed (defaults to all columns)
   columnsToCompare: [amount, quantity];

   // Hashing strategy — currently only MD5Hash is supported
   strategy: MD5Hash;

   // Optional: expected proportion of matching rows (0.0–1.0), used for monitoring
   expectedMatch: 0.99;
}
```

N.B. `expectedMatch` is not yet fully supported as of May 2026.

### `MD5Hash` strategy options

```legend
   strategy: MD5Hash
   {
      sourceHashColumn: srcHash;   // use a pre-computed hash column on the source
      targetHashColumn: tgtHash;   // use a pre-computed hash column on the target
      aggregatedHash: true;        // aggregate row hashes per key group before comparing
   };
```

All three options are optional.  If the `strategy` block is empty it must be omitted
entirely — `strategy: MD5Hash;` (without a block) is the correct minimal form.

---

## Examples

All examples use a fictional **Person** domain:

```
personTable
-----------
ID          INTEGER  (primary key)
FIRSTNAME   VARCHAR
LASTNAME    VARCHAR
FIRMID      INTEGER
HASH        VARCHAR  (pre-computed hash column, present on some variants)
DOB         DATETIME
CODE        FLOAT
COUPOUN     DECIMAL
CREATED     DATE
```

---

### a) Basic row-level recon — hash computed dynamically

The simplest case: compare two queries on the same (or equivalent) table, hashing a
specified set of columns and joining on a primary key.

> **Code example:** [`testLambdaGeneration_dataRecon_dynamicHash`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L27)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonRowRecon
{
   source: |#>{trading::db.personTable}#->select(~[ID, FIRSTNAME, LASTNAME])->from(trading::SourceRuntime);
   target: |#>{trading::db.personTable}#->select(~[ID, FIRSTNAME, LASTNAME])->from(trading::TargetRuntime);
   keys: [ID];
   columnsToCompare: [FIRSTNAME, LASTNAME];
   strategy: MD5Hash;
}
```

**What happens:** Legend hashes `FIRSTNAME` and `LASTNAME` for each row on both sides,
joins on `ID`, and returns rows where the hashes differ.  The output contains
`ID_SOURCE`, `DIGEST_SOURCE`, `ID_TARGET`, `DIGEST_TARGET` for each defect row.

---

### b) Recon using a pre-computed hash column

If a hash column already exists on the dataset (e.g. populated upstream), you can
point the recon at it directly, skipping re-computation.

> **Code example:** [`testLambdaGeneration_dataRecon_preComputedHash`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L53)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonPrecomputedHashRecon
{
   source: |#>{trading::db.personTable}#->select(~[ID, HASH])->from(trading::SourceRuntime);
   target: |#>{trading::db.personTable}#->select(~[ID, HASH])->from(trading::TargetRuntime);
   keys: [ID];
   strategy: MD5Hash
   {
      sourceHashColumn: HASH;
      targetHashColumn: HASH;
   };
}
```

It is also valid to have a pre-computed hash on only one side — Legend will generate the
hash dynamically for the other:

> **Code example:** [`testLambdaGeneration_dataRecon_oneSidePrecomputedHash`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L180)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonOneSideHashRecon
{
   source: |#>{trading::db.personTable}#->select(~[FIRSTNAME, LASTNAME])->from(trading::Runtime);
   target: |#>{trading::db.personTable}#->select(~[HASH])->from(trading::Runtime);
   columnsToCompare: [FIRSTNAME, LASTNAME];
   strategy: MD5Hash
   {
      targetHashColumn: HASH;
   };
}
```

---

### c) Aggregated hash — group-level recon

When row order within a group may differ between source and target (e.g. after a
`GROUP BY`), use `aggregatedHash: true`.  Legend sorts the row hashes before aggregating
so that row ordering within the group does not affect the result.

> **Code example:** [`testLambdaGeneration_dataRecon_aggregatedHash`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L75)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::FirmAggregatedHashRecon
{
   source: |#>{trading::db.personTable}#->select(~[FIRMID, HASH])->from(trading::SourceRuntime);
   target: |#>{trading::db.personTable}#->select(~[FIRMID, HASH])->from(trading::TargetRuntime);
   keys: [FIRMID];
   strategy: MD5Hash
   {
      sourceHashColumn: HASH;
      targetHashColumn: HASH;
      aggregatedHash: true;
   };
}
```

**What happens:** Rows are grouped by `FIRMID`, the individual `HASH` values within each
group are sorted and concatenated, then a final MD5 is taken over the concatenation.
A defect row is emitted for each `FIRMID` where the source and target group hashes differ.

> **No keys — whole-dataset aggregated hash:** if `keys` is empty and `aggregatedHash` is
> `true`, the entire dataset is collapsed to a single hash and compared as one row.  Useful
> for a quick "are these two datasets identical?" check.
>
> **Code example:** [`testLambdaGeneration_dataRecon_noKeysAndAggregatedHash`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L204)

---

### d) Recon with no explicit keys

If `keys` is left empty and `aggregatedHash` is `false`, the row hash digest itself is
used as the join key (a hash-join approach).  This is appropriate when there is no natural
primary key but you still want to detect rows that are present on one side but not the
other.

> **Code example:** [`testLambdaGeneration_dataRecon_noPrimaryOrGroupingKeys`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L154)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonNoKeyRecon
{
   source: |#>{trading::db.personTable}#->select(~[FIRSTNAME, LASTNAME])->from(trading::Runtime);
   target: |#>{trading::db.personTable}#->select(~[FIRSTNAME, LASTNAME])->from(trading::Runtime);
   columnsToCompare: [FIRSTNAME, LASTNAME];
   strategy: MD5Hash;
}
```

---

### e) Including column values in the output

`columnsToCompare` controls which column values appear in the defect output when
`includeColumnValues` is set at execution time via the API (see
[Execution with Engine APIs](#execution-with-engine-apis)).  By default the output contains
only the key column(s) and the digest columns.

> **Code example:** [`testLambdaGeneration_dataRecon_includeColumnValues`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L238)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonColumnValuesRecon
{
   source: |#>{trading::db.personTable}#->select(~[ID, FIRSTNAME, LASTNAME])->from(trading::Runtime);
   target: |#>{trading::db.personTable}#->select(~[ID, FIRSTNAME, LASTNAME])->from(trading::Runtime);
   keys: [ID];
   columnsToCompare: [FIRSTNAME, LASTNAME];
   strategy: MD5Hash;
}
```

With `includeColumnValues: true` at execution time, the output will contain
`ID_SOURCE`, `FIRSTNAME_SOURCE`, `LASTNAME_SOURCE`, `DIGEST_SOURCE`, `ID_TARGET`,
`FIRSTNAME_TARGET`, `LASTNAME_TARGET`, `DIGEST_TARGET` for each defect row.

> Note: `includeColumnValues` is ignored when `aggregatedHash` is `true`, because the
> aggregation step collapses individual rows and column values are no longer meaningful.

---

### f) Parameterised source and target queries

Both the `source` and `target` lambdas can accept parameters, allowing the recon to be
scoped at execution time (e.g. to a specific date partition).  Because the same invocation
call drives both, parameter names on source are prefixed `source_` and on target `target_`
when supplying values to the API.  If both sides happen to use the same parameter name, they
are still disambiguated by the prefix.

> **Code examples:**
> [`testLambdaGeneration_dataRecon_parameterizedSourceAndTarget`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L288),
> [`testLambdaGeneration_dataRecon_parameterizedSourceOnly`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L318),
> [`testLambdaGeneration_dataRecon_sameParamName`](../../legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L347)

```legend
###DataQualityValidation
DataQualityRelationComparison trading::PersonParameterisedRecon
{
   // Each lambda declares its own parameter — values are supplied at execution time
   source: {sourceFilter: String[1] |
               #>{trading::db.personTable}#
                 ->filter(row | $row.FIRSTNAME == $sourceFilter)
                 ->from(trading::SourceRuntime)};
   target: {targetFilter: String[1] |
               #>{trading::db.personTable}#
                 ->filter(row | $row.FIRSTNAME == $targetFilter)
                 ->from(trading::TargetRuntime)};
   keys: [ID];
   columnsToCompare: [FIRSTNAME, LASTNAME];
   strategy: MD5Hash;
}
```

---

## Type normalisation

Before hashing, Legend normalises column values to canonical string representations to
prevent spurious mismatches due to floating-point precision or timestamp format differences:

| Column type | Normalisation |
|---|---|
| `Float` | IEEE 754 scientific notation, 15 significant figures (e.g. `1.234500000000000E+000`) |
| `Decimal` | IEEE 754 scientific notation, 15 significant figures |
| `DateTime` | ISO-8601 with nanoseconds (e.g. `2026-05-15T09:30:00.000000000`) |
| `Date` | ISO-8601 date (e.g. `2026-05-15`) |
| `Binary` | Base64-encoded string |
| All other types | `toString()` as-is |

> **Code example:** [`testLambdaGeneration_dataRecon_normalizesAllTypes`](../../legend-engine-xt-dataquality/legend-engine-xt-dataquality-pure-test/src/main/resources/core_dataquality_test/datarecon_test.pure#L105)

---

## Output schema

Each defect row in the result contains:

| Column | Description |
|---|---|
| `<KEY>_SOURCE` | Key column value from the source dataset (one column per key) |
| `<KEY>_TARGET` | Key column value from the target dataset (one column per key) |
| `DIGEST_SOURCE` | MD5 hash of the source row (or `<sourceHashCol>_SOURCE` if a pre-computed column was used) |
| `DIGEST_TARGET` | MD5 hash of the target row (or `<targetHashCol>_TARGET` if a pre-computed column was used) |
| `<COL>_SOURCE` / `<COL>_TARGET` | Individual column values — only present when `includeColumnValues: true` |
| `DQ_RULE_NAME` | Always `DQ_RECONCILIATION` — only present when `enrichDQColumns: true` |
| `DQ_LOGICAL_DEFECT_ID` | MD5 of the two digest values — stable across runs for de-duplication. Only present when `enrichDQColumns: true` |

Rows present on the source but absent on the target will have null `<KEY>_TARGET` and
`DIGEST_TARGET` values (and vice versa for target-only rows), because a full outer join is
used.

---

## Execution with Engine APIs

### Endpoint

```
POST /api/pure/v1/dataquality/reconciliation
Content-Type: application/json
```

### Request body (`DataQualityReconInput`)

| Field | Type | Required | Description |
|---|---|---|---|
| `model` | `PureModelContext` | ✓ | The full model context containing the `DataQualityRelationComparison` element and its dependencies |
| `packagePath` | `String` | ✓ | Fully-qualified path of the `DataQualityRelationComparison` element to execute (e.g. `trading::PersonRowRecon`) |
| `includeColumnValues` | `Boolean` | | When `true`, the compared column values are included in the output alongside the key and digest columns. Defaults to `false` |
| `defectLimit` | `Long` | | Cap on the number of defect rows returned. If omitted, all defects are returned |
| `enrichDQColumns` | `Boolean` | | When `true`, adds `DQ_RULE_NAME` and `DQ_LOGICAL_DEFECT_ID` metadata columns to the output. Defaults to `false` |
| `sourceLambdaParameterValues` | `List<ParameterValue>` | | Parameter values for a parameterised `source` lambda |
| `targetLambdaParameterValues` | `List<ParameterValue>` | | Parameter values for a parameterised `target` lambda |

### Example: with defect limit and DQ metadata columns

```json
{
  "model": { ... },
  "packagePath": "trading::PersonRowRecon",
  "defectLimit": 500,
  "enrichDQColumns": true
}
```

### Example: running a parameterised recon

```json
{
  "model": { ... },
  "packagePath": "trading::PersonParameterisedRecon",
  "sourceLambdaParameterValues": [
    { "name": "sourceFilter", "value": { "_type": "stringValue", "value": "Alice" } }
  ],
  "targetLambdaParameterValues": [
    { "name": "targetFilter", "value": { "_type": "stringValue", "value": "Alice" } }
  ]
}
```

### Generate execution plan only

To generate the execution plan without running it:

```
POST /api/pure/v1/dataquality/reconciliation/generatePlan
```

The request body is identical to the execute endpoint.

---

## Further reading

* [Data Quality Overview](./data-quality-overview.md)
* [Relation Data Validation](./relation-data-validation.md)
* [Service Post Validations](./service-post-validations.md)

