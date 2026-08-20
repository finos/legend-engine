# Non-Relational to Relational — Variant Temp Table Bridge

> **Audience:** Developers building cross-store pipelines that need to move data from a
> non-relational store (ServiceStore, in-memory, etc.) into a relational store for SQL-based
> processing, filtering, projection, or Data Quality validation.
>
> **See also:**
>
> - [TDS and Relation](../reference/tds-and-relation.md) — background on the Relation API.
> - [Router and Pure-to-SQL Pipeline](router-and-pure-to-sql.md) — routing and plan-gen pipeline.
> - [PCT](../../pct/) — cross-store compatibility testing for Relation functions.

---

## 1. Overview

When a query fetches data from a non-relational store (e.g. ServiceStore via `graphFetch`) and
needs to apply SQL-native operations (filters, joins, aggregations, DQ validations) in a
relational store (e.g. Snowflake), Legend Engine bridges the gap by materialising the source data
into a **transient single-column VARIANT temp table** and returning a `Relation<Z>` over it.

This is conceptually analogous to cross-store join — where relational temp tables are used to
pass data across a cluster boundary — but in this case the source is non-relational (platform
cluster) and the target is a relational SQL engine.

**The trigger expression:**

```pure
source->fromJson()->toMany(@Variant)->flatten(~colName)
```

When the router detects that `flatten(toMany(@Variant, fromJson(source)), ~colName)` is called
and `source` comes from a non-relational cluster (cross-store `PlanVarPlaceHolder`), it
automatically routes the entire pipeline through the variant temp table mechanism. No explicit
write function is needed — the intent is inferred from the expression shape.

---

## 2. Usage Patterns

### 2.1 Inline (single expression)

The full pipeline is expressed as one chain. The router cleanly separates the non-relational
source from the relational operations that follow.

```pure
dummy::Metrics.all()
  ->graphFetch(#{ dummy::Metrics { code, status } }#)
  ->serialize(#{ dummy::Metrics { code, status } }#)
  ->from(dummy::MetricsMapping, dummy::ServiceStoreCrossStoreRuntime)
  ->fromJson()
  ->toMany(@Variant)
  ->flatten(~value)
  ->select(~[value])
  ->project(~[
      code: row | $row.value->get('code')->to(@String),
      status: row | $row.value->get('status')->to(@Integer)
    ])
  ->from(dummy::TempTableRuntime)
```

### 2.2 Accessing variant fields

Inside `project()`, each row's variant column is accessed via `get()` and cast with `to()`:

```pure
row | $row.value->get('fieldName')->to(@String)
row | $row.value->get('count')->to(@Integer)
```

### 2.3 DataQuality integration

Works with `DataQualityRelationValidation`. Place the full pipeline in the `query:` field;
validations apply assertions on top of the resulting relation.

```pure
DataQualityRelationValidation testing::DQTest
{
  query: |dummy::Metrics.all()
      ->graphFetch(#{ dummy::Metrics { code, status } }#)
      ->serialize(#{ dummy::Metrics { code, status } }#)
      ->from(dummy::MetricsMapping, dummy::ServiceStoreCrossStoreRuntime)
      ->fromJson()
      ->toMany(@Variant)
      ->flatten(~value)
      ->select(~[value])
      ->project(~[
          code: row | $row.value->get('code')->to(@String),
          status: row | $row.value->get('status')->to(@Integer)
        ])
      ->from(dummy::TempTableRuntime);
  validations: [
    { name: 'codeNotValid'; assertion: rel | $rel->filter(row | $row.code == 'Invalid')->assertRelationEmpty(~[code]); }
  ];
}
```

## 3. How It Works — The Trigger Pattern

The pattern `->fromJson()->toMany(@Variant)->flatten(~col)` is the signal that triggers this
mechanism. Specifically, `flatten_T_MANY__ColSpec_1__Relation_1_` is detected by the relational
store when its source matches the `toMany(@Variant, fromJson(cross-store-source))` shape.

When detected, instead of attempting SQL generation for the non-relational source (which would
fail), the engine:

1. Plans the non-relational fetch as an upstream sub-plan
2. Streams its output into a transient VARIANT temp table
3. Executes subsequent `select`, `project`, `filter`, etc. as SQL over that temp table

---

## 4. Temp Table Mechanics

### 4.1 Location

Temp tables are created in `LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA` (Snowflake, via
`SnowflakeCommands.processTempTableName`). Other dialects don't override `processTempTableName`
and use their native, unqualified session-temp location (DuckDB: session temp; H2: its default
schema, e.g. `PUBLIC`).

The location can be configured via `tempTableDb` / `tempTableSchema` in the
`SnowflakeDatasourceSpecification`. If not configured, `LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA` is
used as the default — this database/schema must exist in the Snowflake account with CREATE
TEMPORARY TABLE permission.

### 4.2 Naming

```
variant_temp_col_<colName>     (inline/constant source)
variant_temp_<planVarName>     (cross-cluster source via PlanVarPlaceHolder)
```

### 4.3 Lifecycle

The temp table is created, populated, queried, and dropped all within the same SQL session and
block connection. It is never persisted beyond the execution.

### 4.4 Memory-efficient loading

Source objects are streamed row-by-row via a batched `PreparedStatement` — the stream is never
fully materialised in memory:

- Each object is serialised to one JSON string and bound to a single `?` parameter.
- Batches flush at **4 MB** of accumulated JSON **or 1 000 rows**, whichever comes first.
- The source stream is always closed in a `finally` block.

---

## 5. Dialect Support

The **column type** comes from each dialect's existing `dataTypeToSqlText` registration for the abstract `SemiStructured` Pure type (in that
dialect's own `typeConversion.pure`, e.g. `dataTypeToSqlTextSnowflake`), the same lookup used
elsewhere for DDL/column-type rendering. Dialects without their own registration (Redshift,
MemSQL) fall through to the generic default (`getColumnTypeSqlTextDefault`), which maps
`SemiStructured` to `VARCHAR(4000)`. The table below reflects what's registered:

| Dialect | Column type (via `dataTypeToSqlText`) | INSERT pattern |
|---|---|---|
| **DuckDB** | `JSON` | `INSERT ... VALUES (cast(? as JSON))` |
| **Snowflake** | `VARIANT` | `INSERT ... SELECT parse_json(?)` |
| **Databricks** | `VARIANT` | `INSERT ... SELECT parse_json(?)` |
| **H2** | `VARCHAR(<maxVarcharLength>)` | `INSERT ... VALUES (legend_h2_extension_json_parse(?))` |
| **PostgreSQL** | `JSON` | `INSERT ... VALUES (CAST(? AS JSON))` |
| **Redshift** | `VARCHAR(4000)` (no dialect override registered) | `INSERT ... VALUES (JSON_PARSE(?))` |
| **MemSQL** | `VARCHAR(4000)` (no dialect override registered) | `INSERT ... VALUES (?)` |
| **ClickHouse** | `JSON` (table still declared `ENGINE=Memory`) | `INSERT ... VALUES (?)` |

The **INSERT pattern** (how to bind a JSON string value into that column via a parameterized
statement — `parse_json(?)`, `cast(? as JSON)`, plain `?`, ...) is a separate, still-necessary
per-dialect hook (`RelationalDatabaseCommands.getSemiStructuredInsertStatement`) — nothing in the
existing type-conversion machinery covers it, since it's a data-binding concern rather than a
static DDL-type concern.

---

## 6. Architecture

### 6.1 Routing

`flatten_T_MANY__ColSpec_1__Relation_1_` is in:
- **`shouldStopRouting`** — prevents the router's first pass from routing flatten's arguments,
  avoiding mapping-context contamination from the non-relational `from(mapping, runtime)` inside
  the source chain.
- **`routeFunctionExpressions`** — intercepts flatten before argument routing and returns
  `shouldBeRouted=false`, so downstream `select`, `project`, `with` are unaffected by the
  non-relational routing context.
- **`planExecution` dispatch** — when flatten is the outermost function AND its source matches
  the variant pattern, dispatches to `planWriteToVariantTempTableExecution`.

### 6.2 Plan generation

`planWriteToVariantTempTableExecution` in `relationalMutation_relation.pure` handles the source
after unwrapping the `toMany(@Variant, fromJson(source))` chain:

| Source shape | Handling |
|---|---|
| `PlanVarPlaceHolder` (cross-cluster — let binding) | Upstream cluster streams its result; `CreateAndPopulateTempTableExecutionNode` reads from it |
| `from(expr, mapping, runtime)` (inline cross-store) | Planned as a sub-plan via `executionPlan()` wrapped in `AllocationExecutionNode` |

Plan structure:

```
SequenceExecutionNode
  AllocationExecutionNode          ← upstream non-relational sub-plan result
  RelationalBlockExecutionNode (isolationLevel=2)
    CreateAndPopulateTempTableExecutionNode (tempTableColumnMetaData: one SemiStructured column)
    SQLExecutionNode               ← SELECT from temp table
```

This is the same `CreateAndPopulateTempTableExecutionNode` shape used by cross-store join. 
The node's single `TempTableColumnMetaData` column has `dataType` resolved to the target dialect's 
concrete SQL type.

For **nested** flatten (`flatten(...)->select(...)`/`project(...)`, i.e. flatten is not the
outermost function of the `StoreQuery`), the Normal Flow in `planExecution` calls
`collectWriteToVariantExprs` (which also scans `inScopeVars` for let-bound expressions) to find
the flatten node and prepend the create/load prerequisites via `buildVariantCreateAndLoadNode`.
The SELECT side for this case is **not** built by `buildVariantSelectNode` — flatten here is
compiled as an ordinary sub-expression of the enclosing `select`/`project`, through the generic
`pureToSqlQuery` dispatch table (§6.4, `processWriteToVariantTempTable`). This matters because
that function has no `DatabaseConnection` in scope (see §6.3) — it's a materially different code
path from the direct-dispatch case, with its own SQL-qualification handling (a default
post-processor, since the dispatch function itself can't do it — see §6.3).

### 6.3 SQL name qualification

There are **two independent code paths** that build a `Table` reference to the variant temp table
for the generated `SELECT`, and they differ in whether dialect qualification is even possible:

- **Direct dispatch** (`flatten` is the outermost function, e.g. `flatten(...)->with(runtime)`
  with no `select`/`project`): `buildVariantSelectNode` (`relationalMutation_relation.pure`) is
  invoked directly from `planWriteToVariantTempTableExecution`, which has the `DatabaseConnection`
  in scope. It calls `$dbConfig.procesTempTableName($tempName, $dbConnection)` — the same
  extension point relational `graphFetch` uses for its own temp tables — **before** constructing
  the `Table`, so the name is already fully dialect-qualified (e.g.
  `LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.variant_temp_col_value` for Snowflake) by the time it reaches
  SQL generation. `schema.name` is set to the `'default'` sentinel so nothing gets double-prefixed
  on top of that already-qualified name.
- **Nested dispatch** (`flatten(...)->select(...)`/`project(...)`, e.g. flatten inside a
  `project(row | $row.value->get(...))`): `processWriteToVariantTempTable`
  (`pureToSQLQuery.pure`, §6.4) builds the same kind of `Table` reference, but it is a leaf entry
  in the generic per-function `pureToSqlQuery` dispatch table (`get()`, `add()`, `flatten()`, ...)
  — that dispatch signature has **no `DatabaseConnection` parameter at all**, by design, since it's
  meant for translating individual sub-expressions independent of dialect. It therefore cannot call
  `procesTempTableName` itself and always embeds the bare, unqualified temp table name at that
  stage (`schema.name` is still set to `'default'` here, avoiding a *false* schema prefix on top of
  the bare name — necessary but not sufficient on its own for dialects that qualify their temp
  tables, like Snowflake).

  The qualification for this path happens one stage later, in a dedicated **default
  post-processor**, `qualifyVariantTempTableNames`
  (`relationalMutation_relation.pure`) — registered in `postProcessorList()`
  (`defaultPostProcessor.pure`) alongside `processInOperation`. Post-processors run over the fully
  assembled query with the `DatabaseConnection` in scope (unlike the leaf `pureToSqlQuery` dispatch
  table), so this is the same stage `processInOperation`'s `IN (...)`-clause temp-table spill
  already uses to qualify its own temp table via `procesTempTableName`. `qualifyVariantTempTableNames`
  walks the query's `RelationalOperationElement` tree and rewrites only `Table` nodes matching
  **all three** of:
  - `temporaryTable == true` — never true for a real/permanent table;
  - has a column of type `SemiStructured` — this is the SELECT-side `Column` built locally by
    `buildVariantSelectNode`/`processWriteToVariantTempTable` (kept as the abstract marker type,
    unlike `tempTableColumnMetaData`'s `dataType` — see §6.5), so this check is unaffected by how
    that other, separate field is resolved; nothing else in the codebase produces a `SemiStructured`
    column;
  - `name->startsWith('variant_temp_')` — the raw-name convention from
    `extractVariantWriteTempName`/`extractVariantColName`; this also guards idempotency, since it
    only matches the still-bare name and therefore can't double-qualify the direct-dispatch case's
    already-qualified `Table` (re-applying `procesTempTableName` is not idempotent for dialects
    like Snowflake, which unconditionally prepends its prefix).

### 6.4 pureToSQLQuery dispatcher

`processFlattenOrVariantTempTable` is registered in `getSupportedFunctions()` for the flatten
fn-id. It detects whether the flatten is:
- **Variant temp-table case** (source is `toMany(@Variant, fromJson(...))` or `PlanVarPlaceHolder`)
  → calls `processWriteToVariantTempTable` which generates `SELECT col FROM temp_table` (see §6.3
  for the SQL-qualification caveat specific to this path)
- **Regular relational lateral flatten** → delegates to `processVariantFlatten`

### 6.5 Protocol and execution dispatch

`CreateAndPopulateTempTableExecutionNode` carries no variant-specific fields. It uses the same
`tempTableColumnMetaData: TempTableColumnMetaData[*]` field as cross-store join, where each
`TempTableColumnMetaData.column` is an `SQLResultColumn` with a `label` (column name) and
`dataType` (`String`).

```java
boolean isSemiStructuredIngest = resultList.stream().anyMatch(r -> r instanceof JsonStreamingResult);
```

When true, it calls `loadVariantTempTable`, which streams the source row-by-row via a batched
`PreparedStatement` instead of the CSV-file path cross-store join normally uses for
temp table population. Because the column type is already resolved, `createTempTable` needs no
variant-specific logic at all — every dialect's override just renders `column.type` as-is, the
same as it always has for any other column type. The one remaining variant-specific hook is:

| Method | Purpose |
|---|---|
| `getSemiStructuredInsertStatement(tableName, columnName)` | Returns the full `INSERT` SQL for binding a JSON string into that column via `?`. **The base implementation throws** — every dialect participating in the variant temp-table bridge must override it. Not covered by the type-conversion machinery since it's about binding runtime values, not declaring a static DDL type. |

---

## 7. Key Files

| Layer | File | Purpose |
|---|---|---|
| Trigger functions | `core_functions_variant/functions/convert/fromJson.pure`, `toMany.pure`, `flatten.pure` | Existing platform functions — no changes needed |
| Store contract | `core_relational/relational/contract/storeContract.pure` | `clusteringEscapeFunctions`, `shouldStopRouting`, `routeFunctionExpressions`, `planExecution` dispatch |
| Plan-gen | `core_relational/relational/mutation/relationalMutation_relation.pure` | `planWriteToVariantTempTableExecution`, `buildVariantCreateAndLoadNode`, `buildVariantInlineAllocNode`, `qualifyVariantTempTableNames` (post-processor), helpers |
| SQL sub-expr | `core_relational/relational/pureToSQLQuery/pureToSQLQuery.pure` | `processFlattenOrVariantTempTable` dispatcher, `processWriteToVariantTempTable` |
| Post-processor registration | `core_relational/relational/postprocessor/defaultPostProcessor/defaultPostProcessor.pure` | `postProcessorList()` — `qualifyVariantTempTableNames` registered alongside `processInOperation` |
| Protocol POJO | `CreateAndPopulateTempTableExecutionNode.java` | Same node cross-store join uses — no variant-specific fields; carries `tempTableColumnMetaData` |
| Executor | `RelationalExecutionNodeExecutor.java` | Detects a `JsonStreamingResult` upstream input, dispatches to `loadVariantTempTable` (batched `PreparedStatement` streaming load) |
| Dialect DDL/DML | `RelationalDatabaseCommands.java` + `*Commands.java` per dialect | `getSemiStructuredInsertStatement` (JSON-parse INSERT wrapping) — `createTempTable` needs no variant-specific logic, `dataType` arrives already resolved |
| Type resolution | Each dialect's `typeConversion.pure` (`dataTypeToSqlText`) | Existing, pre-feature per-dialect mapping for the abstract `SemiStructured` type — reused rather than duplicated in Java (see §5) |

---

## 8. Testing

The bridge is exercised end-to-end via PCT-style parameterised tests. The test bodies live once
in the shared core-pure module and are re-run against every wired dialect's live connection.

| Layer | File |
|---|---|
| Shared test bodies (`<<paramTest.Test>>`) | `core_relational/relational/mutation/tests/testNonRelationalToRelational.pure` |
| Per-dialect `TestCollection` wrapper (Pure) | `core_relational_<db>_pct/testNonRelationalToRelational.pure` in each `-PCT` module — calls `collectParameterizedTests('<db>', getTestConnection(DatabaseType.<Db>), [], [])` under `meta::relational::tests::pct::<db>::nonRelationalToRelational::testCollection` |
| Per-dialect JUnit runner | `Test_Relational_<Db>_NonRelationalToRelational.java` in each `-PCT` module |

Dialects currently wired: **H2**, **DuckDB**, **PostgreSQL**, **Snowflake**, **Databricks**.