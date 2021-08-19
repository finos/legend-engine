Refactoring Goals 
=================== 
* Make it "easy" for developers to add new relational connectors to Legend.     
  * Provide simple Java interfaces by which new relational connectors can be added. 
  * Enable reuse of authentication code across multiple relational connectors.
* Make it "easy/convenient" for programs to consume Legend jars.
  * Allow programs to include dependencies on specific relational connectors (and not all the connectors)

Proposed High Level Refactoring 
===================
* store-relational-connection
  * Remove all database, authentication specific impl classes from store-relational-connection
  * Only keep the extension interfaces and other generic impl classes 
* store-relational-connection-auth-XXXXX
  * Code to implement an authentication scheme XXXX is pushed into a specific module store-relational-connection-auth-userpass
  * These modules are discovered using a service loader 
* store-relational-connection-datasource-YYYY
  * Code to add support for database YYYY is pushed into a specific module e.g store-relational-connection-datasource-snowflake
  * Each datasource module declares (Maven) dependencies on the auth modules that it supports
* store-relational
  * This module now just has the generic execution code like RelationalExecutionManager 
  * This module is also an "assembly" module that declares (Maven) dependencies on the different datasource modules 
  * TODO : Programs that use the Legend jars can explicitly exclude the top level datasource modules that they do not need
  
Open Questions 
===================
* Test Database Support 
* OAuth Profiles 
* Vault 
    * Use of a vault to obtain secrets/key material does not distinguish an authentication type
    * i.e 2 strategies which use the same authentication scheme (e.g user/password) but differ in where the password is fetched from (e.g properties file vs AWS Secrets Manager) are still the same auth type

Module : store-relational 

Module : store-relational-connection

- AuthStrategyKey 
- AuthStrategy
- DataSourceSpecificationKey
- DataSourceSpecification 
- DatabaseManager 
- DriverWrapper
- RelationalDatabaseCommands
- RelationalDatabaseCommandsVisitor
- StrategicConnectionExtension
- ConnectionExtension 
- RelationalStoreExecutorExtension

Module : store-relational-connection-auth-userpass 

- UserPassAuthStrategyKey
- UserPassAuthStrategyKeyGenerator (via a StrategicConnectionExtension) 
- UserPassAuthStrategy
- UserPassAuthStrategyTransformer (via a StrategicConnectionExtension)

Module : store-relational-connection-database-snowflake 

- SnowflakeDataSourceSpecificationKeyGenerator (via a StrategicConnectionExtension) 
- SnowflakeDataSourceSpecificationTransformer (via a StrategicConnectionExtension) 
- SnowflakeManager (via a ConnectionExtension)
- SnowflakeDriverWrapper
- SnowflakeDatabaseCommands

Gotchas 
===================

1/ StrategicConnectionExtension is not fine grained 

An implementation of StrategicConnectionExtension has to provide both auth and datasource classes. 
But in the proposed refactoring, the auth module cannot provide datasource classes and vice versa. 

Tactical solution : Return nulls and skip nulls in RelationalExecutionManager 

2/ RelationalDatabaseCommandsVisitor 

Visitor is defined with concrete visit methods which introduces a dependency cycle (store-relational-connection -> store-relational-connection-database-snowflake -> relational-connection)

public interface RelationalDatabaseCommandsVisitor<T>
{
    T visit(SnowflakeCommands snowflakeCommands);
    T visit(H2Commands h2Commands);
    T visit(BigQueryCommands bigQueryCommands);
}

Solution : Remove the RelationalDatabaseCommandsVisitor. Replace with additional methods on DatabaseManager.
i.e We are already in the context of a DatabaseManager. Why not ask the DatabaseManager to do stuff.

Instead of ...

       databaseManager.relationalDatabaseSupport().accept(RelationalDatabaseCommandsVisitorBuilder.getStreamResultToTempTableVisitor(relationalExecutionConfiguration, connectionManagerConnection, res, tempTableName, databaseTimeZone));
 
.. we do 

	databaseManager.relationalDatabaseSupport().prepareTempTable(RelationalExecutionConfig )
	databaseManager.relationalDatabaseSupport().doSomethingElse(...)

However, this introduces another dependency cycle : store-relational depends on relational-connection, relational-connection depends on store-relational (via the dependency on RelationalExecutionConfig) 

Solution : Break the dependency by introducing new config type in relational-connection

3/ What about StaticDataSourceSpecification which is not specific to a database type ?


Questions                                                                  
===================

1/ Singleton RelationalStoreExecutorExtension 