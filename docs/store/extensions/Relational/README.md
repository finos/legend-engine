# Relational Store

Relational store specifications can be used to model and query SQL based databases. An example model can be found [here](Databases/bigquery/bigquery-example-model.md).
To support new databases, we need to implement connector extensions for them.

Developing a new relational connector has 5 main parts:

1. Db Specific Sql generation logic.
2. Modelling the connector specific data source and authentication strategy specifications.
3. Implementing the driver logic and authentication logic.
4. Testing 1 + 3 by writing an integration test that runs test suite against a test db instance.
5. Testing 2 + 3 by writing connection acquisition tests, which run against a test db instance.

[Step by step tutorial for adding a new connector](new-connector-tutorial.md)

[See list of currently supported connectors here](database-integration-tests/README.md)