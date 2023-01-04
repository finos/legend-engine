# Relational Store

Relational store specifications can be used to model and query SQL based databases. An example model can be found [here](Databases/bigquery/bigquery-example-model.pure).
To support new databases, we need to implement connector extensions for them.

Developing a new relational connector has 5 main parts:

1. Db Specific Sql generation logic.
2. Modelling the connector specific data source and authentication strategy specifications.
3. Implementing the driver logic and authentication logic.
4. Testing 1 + 3 by writing an integration test that runs test suite against a test db instance.
5. Testing 2 + 3 by writing connection acquisition tests, which run against a test db instance.

Module structure of a relational connector:

1. **\<dbType\>-protocol** module defines the POJOs representing connector specific data source and authentication strategy specifications.
2. **\<dbType\>-pure** module contains code for db specific sql generation logic written in PURE language.
   It also specifies PURE to JSON conversion and vice-versa for specifications defined in \<dbType\>-protocol module.
3. **\<dbType\>-grammar** module contains ANTLR based code for bi-directional conversion between textual DSLs and POJOs for specifications defined in \<dbType\>-protocol module.
   End users write the specifications using the textual DSLs instead of POJOs. Module also contains a POJO to PURE compiler.
4. **\<dbType\>-execution** module defines the driver and authentication logic.
5. **\<dbType\>-execution-tests** module contains integration tests for connectivity and execution against a test db instance.

[Step by step tutorial for adding a new connector](new-connector-tutorial.md)

[See list of currently supported connectors and their integration tests here](database-integration-tests/README.md)