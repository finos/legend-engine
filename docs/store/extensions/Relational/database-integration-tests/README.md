# Database Integration Tests

This document lists the integration tests run against various supported databases. 

**All Databases Tests Run Summary**

Click here: [summarize-sql-generation-integration-tests](https://github.com/finos/legend-engine/actions/workflows/summarize-sql-generation-integration-tests.yml)

See the summary printed on latest run.

**Individual Databases Connectivity Tests**

[BigQuery](https://github.com/finos/legend-engine/actions/workflows/database-bigquery-integration-test.yml)
&emsp; [Databricks](https://github.com/finos/legend-engine/actions/workflows/database-databricks-integration-test.yml)
&emsp; [MSSqlServer](https://github.com/finos/legend-engine/actions/workflows/database-mssqlserver-integration-test.yml)

[Postgres](https://github.com/finos/legend-engine/actions/workflows/database-postgresql-integration-test.yml)
&emsp; [Redshift](https://github.com/finos/legend-engine/actions/workflows/database-redshift-integration-test.yml)
&emsp; [Snowflake](https://github.com/finos/legend-engine/actions/workflows/database-snowflake-integration-test.yml)

[Spanner](https://github.com/finos/legend-engine/actions/workflows/database-spanner-integration-test.yml)
&emsp; [Athena](https://github.com/finos/legend-engine/actions/workflows/database-athena-integration-test.yml)

**Individual Databases Sql Generation and Execution Tests**

[BigQuery](https://github.com/finos/legend-engine/actions/workflows/database-bigquery-sql-generation-integration-test.yml)
&emsp; [Databricks](https://github.com/finos/legend-engine/actions/workflows/database-databricks-sql-generation-integration-test.yml)
&emsp; [MSSqlServer](https://github.com/finos/legend-engine/actions/workflows/database-mssqlserver-sql-generation-integration-test.yml)

[Postgres](https://github.com/finos/legend-engine/actions/workflows/database-postgresql-sql-generation-integration-test.yml)
&emsp; [Redshift](https://github.com/finos/legend-engine/actions/workflows/database-redshift-sql-generation-integration-test.yml)
&emsp; [Snowflake](https://github.com/finos/legend-engine/actions/workflows/database-snowflake-sql-generation-integration-test.yml)

[Spanner](https://github.com/finos/legend-engine/actions/workflows/database-spanner-sql-generation-integration-test.yml)
&emsp; [Athena](https://github.com/finos/legend-engine/actions/workflows/database-athena-sql-generation-integration-test.yml)

**Reasons for Failing Tests**

We can classify the test results into 5 categories:

1) Test passed completely.
2) Test ignored as feature is not supported by the db extension.
3) Test partially ignored if feature works but deviates from standard specified by connector interfaces. Such functionality would be deprecated over time.
4) Test failure which needs to be fixed.
5) Real test status unknown, because of setup issues which prevent proper running of integration test. This includes incorrect db settings, incorrect password, incorrect DDLs to create test tables etc.

4 and 5 contribute towards making the overall status red.

**Fixing Failing Tests**

1) Run the Test\_Relational\_DbSpecific\_\<DbType\>\_UsingPureClientTestSuite test to figure out the cases for which sql generation/execution is broken.
Run the ExternalIntegration\_TestConnectionAcquisitionWithFlowProvider\_\<DbType\> test for testing connectivity.

    These tests require you to be able to connect to a test db instance for the dbType.

    For some dbs, test db is docker based, and requires docker app running in background.

    For others, you may need permission to connect to the finos hosted db instance.

    If you want to use some other db instance for testing, you can configure it in userTestConfig\_with\<DbType\>TestConnection.json (for sql gen/execution) or \<dbType\>RelationalDatabaseConnections.json (for connectivity).

    Alternatively, you can browse through the logs of workflow corresponding to dbType and scenario you are interested in, to figure out the failures. They are listed at beginning of this page.
Then you can fix them, raise a pull request, and ask someone with permission to trigger workflows to run the workflow for the dbType with your PR id.
The process can be repeated until you are happy with your fixes.

2) Read sections 6-12 of the main tutorial on [adding a new relational connector](../new-connector-tutorial.md) to get an understanding of the db connectivity and sql generation/execution process.

    If you want to mark a broken feature as unsupported/ignored, you need to fail the pure code with a message starting with '[unsupported-api] '.

    If the feature works, but is not in line with standard defined in test suite, you can mention the appropriate test suite fn as deviating from standard in \<dbType\>TestSuiteInvoker.pure.
You should use this procedure only if a lot of users depend on that feature, and fixing could break existing use cases. Feature should be gradually deprecated and removed.

    Sometimes, test-suite test could be failing, because it is not written in a dbType agnostic manner. In those cases, fixing the suite itself would be the right thing to do.

    Tests which need tables to be created beforehand, could fail because the setup code to create tables failed. You will need to fix the DDL sql generation code in that case.

    For connectivity failures due to incorrectly configured workflow files, make the needed changes, and get them merged to master.

    Read this tutorial section on [executing against database from pure ide](../new-connector-tutorial.md#executing-against-database-from-pure-ide) for making the development process faster.
