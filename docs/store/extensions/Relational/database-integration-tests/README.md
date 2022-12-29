# Database Integration Tests

This document lists the integration tests run against various supported databases. 

**Connectivity Tests**

![BigQuery](https://github.com/finos/legend-engine/actions/workflows/database-bigquery-integration-test.yml/badge.svg)

![Databricks](https://github.com/finos/legend-engine/actions/workflows/database-databricks-integration-test.yml/badge.svg)

![MSSqlServer](https://github.com/finos/legend-engine/actions/workflows/database-mssqlserver-integration-test.yml/badge.svg)

![Postgres](https://github.com/finos/legend-engine/actions/workflows/database-postgresql-integration-test.yml/badge.svg)

![Redshift](https://github.com/finos/legend-engine/actions/workflows/database-redshift-integration-test.yml/badge.svg)

![Snowflake](https://github.com/finos/legend-engine/actions/workflows/database-snowflake-integration-test.yml/badge.svg)

![Spanner](https://github.com/finos/legend-engine/actions/workflows/database-spanner-integration-test.yml/badge.svg)

![Athena](https://github.com/finos/legend-engine/actions/workflows/database-athena-integration-test.yml/badge.svg)

**Sql Generation and Execution Tests**

![BigQuery](https://github.com/finos/legend-engine/actions/workflows/database-bigquery-sql-generation-integration-test.yml/badge.svg)

![Databricks](https://github.com/finos/legend-engine/actions/workflows/database-databricks-sql-generation-integration-test.yml/badge.svg)

![MSSqlServer](https://github.com/finos/legend-engine/actions/workflows/database-mssqlserver-sql-generation-integration-test.yml/badge.svg)

![Postgres](https://github.com/finos/legend-engine/actions/workflows/database-postgresql-sql-generation-integration-test.yml/badge.svg)

![Redshift](https://github.com/finos/legend-engine/actions/workflows/database-redshift-sql-generation-integration-test.yml/badge.svg)

![Snowflake](https://github.com/finos/legend-engine/actions/workflows/database-snowflake-sql-generation-integration-test.yml/badge.svg)

![Spanner](https://github.com/finos/legend-engine/actions/workflows/database-spanner-sql-generation-integration-test.yml/badge.svg)

![Athena](https://github.com/finos/legend-engine/actions/workflows/database-athena-sql-generation-integration-test.yml/badge.svg)

**Tests Run Summary With More Details**

Click here: [summarize-sql-generation-integration-tests](https://github.com/finos/legend-engine/actions/workflows/summarize-sql-generation-integration-tests.yml)

See the summary printed on latest run.

We can classify the test results into 5 categories:

1) Test passed completely.
2) Test ignored as feature is not supported by the db extension.
3) Test partially ignored if feature works but deviates from standard specified by connector interfaces. Such functionality would be deprecated over time.
4) Test failure which needs to be fixed.
5) Real test status unknown, because of setup issues which prevent proper running of integration test. This includes incorrect db settings, incorrect password, incorrect DDLs to create test tables etc.

4 and 5 contribute towards making the overall status red.
