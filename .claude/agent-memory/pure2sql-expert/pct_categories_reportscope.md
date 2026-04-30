---
name: PCT categories and ReportScope mapping
description: Seven PCT categories per relational dialect, each backed by a specific ReportScope provider, platform=compiled, plus the H2/Snowflake semistructured sibling suite and modulesToTest.json CI grouping
type: reference
---

Each fully-covered relational dialect registers seven `Test_Relational_<Dialect>_<Category>Functions_PCT.java` classes. All extend `PCTReportConfiguration` and follow the same shape:

```java
private static final ReportScope reportScope = <Provider>.<scope-field>;
private static final Adapter adapter = CoreRelational<Dialect>PCTCodeRepositoryProvider.<adapter>;
private static final String platform = "compiled";
private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
    one("<Pure function signature>", "<error substring>", AdapterQualifier.<kind>),
    pack("<Pure package>", "<shared error substring>"),
    ...
);

// Suite:
PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter)
```

## The 7 categories and their ReportScope providers

| Category | ReportScope | Provider class | Origin repo |
|---|---|---|---|
| Essential | `essentialFunctions` | `org.finos.legend.pure.m3.PlatformCodeRepositoryProvider` | legend-pure |
| Grammar | `grammarFunctions` | `org.finos.legend.pure.m3.PlatformCodeRepositoryProvider` | legend-pure |
| Standard | `standardFunctions` | `org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider` | legend-engine |
| Relation | `relationFunctions` | `RelationCodeRepositoryProvider` | legend-engine |
| Variant | `variantFunctions` | `VariantCodeRepositoryProvider` | legend-engine |
| Unclassified | `unclassifiedFunctions` | `CoreUnclassifiedFunctionsCodeRepositoryProvider` | legend-engine |
| ScenarioQuant | `scenario_Quant_Functions` | `CoreScenarioQuantCodeRepositoryProvider` | legend-engine |

Essential + Grammar are owned by legend-pure (do not modify lightly — they define the platform). Standard is where the vast majority of new functions land. Relation covers the relation/TDS-operating functions.

## Snapshot (2026-04-20) of locally-green PCT suites

14,004 tests across 12 suites. Default profile: H2, DuckDB, Postgres, ClickHouse, MemSQL, Oracle, Spanner, SqlServer, Trino, DeepHaven, Java Platform Binding, sql-reversePCT. Databricks + Snowflake are cloud-only (profile `pct-cloud-test`, secrets in CI).

H2 and Snowflake each ship an additional non-PCT `Test_Relational_<dialect>_Semistructured.java` suite alongside the 7 PCT categories for variant/semistructured data, co-located with the PCT files.

## Running a single suite

```bash
mvn clean test -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2/legend-engine-xt-relationalStore-h2-PCT -Dtest=Test_Relational_H2_EssentialFunctions_PCT
```

## Expected-failure DSL

- `one("<full Pure signature>", "<error substring>")` — exact match on one test.
- `one(..., "...", AdapterQualifier.X)` — with classification.
- `pack("<Pure package prefix>", "<shared error substring>")` — multi-match across every test in a Pure package.
- `AdapterQualifier` values: `needsImplementation`, `unsupportedFeature`, `needsInvestigation`, `assertErrorMismatch`. See `docs/pct/expected-failures-howto.md`.

## CI wiring

`.github/workflows/resources/modulesToTest.json` groups PCT modules across job shards. Verified entries:

- Group containing `legend-engine-xt-relationalStore-h2-PCT` (lines ~62), `-duckdb-PCT` (~69), `-postgres-PCT` (~76), `-memsql-PCT` (~83), `-snowflake-PCT` (~90), `-spanner-PCT` (~97).
- Separate singletons for `-sqlserver-PCT` (~103), `-databricks-PCT` (~109), `-oracle-PCT` (~115), `-trino-PCT` (~121), `-clickhouse-PCT` (~140).

When adding a new dialect PCT module, add it here in the appropriate group.
