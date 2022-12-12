# Step by step tutorial for adding a new relational connector

**Pre-requisite**: The dbType for which you are planning to add connector, should be included in the following DatabaseType enums
[here](https://github.com/finos/legend-pure/blob/master/legend-pure-m2-store-relational/src/main/resources/platform/relational/relationalRuntime.pure)
and [here](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-protocol/src/main/java/org/finos/legend/engine/protocol/pure/v1/model/packageableElement/store/relational/connection/DatabaseType.java)
and released. This is a temporary requirement till we replace these enums with a class instance.

1. **Archetype Generation**: Use the maven archetype org.finos.legend.engine:legend-engine-xt-relationalStore-dbExtension-archetype in interactive mode to generate the base project for adding the connector.
Let's say we are adding the connector for microsoft's SqlServer.

    * Base command is: mvn archetype:generate. Based on how your maven is installed/and whether you are invoking using intellij/commandline, there might slight variations.
    * You can pass -DoutputDirectory=<path> to configure the location where your project modules will be generated.
    * Choose the archetype mentioned in the beginning when prompted.
    * Archetype will ask for DbType property. Mention SqlServer there. (Note that it is in camel-case, and starts with a capital letter).
    * For the legendEngineVersion property, mention the latest released version of legend-engine.
    * Skip rest of the properties, defaults would be chosen for them.
    * Confirm the property values with a Y, when asked
    * The project will be generated for you. Now you can open the project in intellij.
    * Set your maven runner to use -Xmx4g as vm options, otherwise project may run out of heap space.
    * Run clean + install on the project. It should succeed. If it complains of java version being out of range, use the appropriate java version.
   Run maven refresh once clean + install is done, so that intellij recognizes generated-sources.

2. **Jdbc Driver**: Go to the generated SqlServerDriver.java, and add the DRIVER_CLASSNAME as "com.microsoft.sqlserver.jdbc.SQLServerDriver".
You will also need to add the following driver dependency to the pom.xml of sqlserver-execution module.

    ~~~xml
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <version>6.2.1.jre7</version>
        <scope>runtime</scope>
    </dependency>
    ~~~

    Go to SqlServerManager.java and implement the buildURL method to return the jdbc url:

    ~~~java
    return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + databaseName;
    ~~~

3. **Datasource and Authentication Specification**: See if one of the existing common DatasourceSpecification and AuthenticationStrategy subtypes work for connecting to databases within your use cases.
Check the listing [here](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-protocol/src/main/java/org/finos/legend/engine/protocol/pure/v1/RelationalProtocolExtension.java).
If yes, then choose the appropriate ones. Else go to the section on [Adding a new Datasource or AuthenticationStrategy Specification](#adding-a-new-datasource-or-authenticationstrategy-specification).

    For the sake of this tutorial lets continue with [StaticDatasourceSpecification](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-protocol/src/main/java/org/finos/legend/engine/protocol/pure/v1/model/packageableElement/store/relational/connection/specification/StaticDatasourceSpecification.java) plus [UserNamePasswordAuthenticationStrategy](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-protocol/src/main/java/org/finos/legend/engine/protocol/pure/v1/model/packageableElement/store/relational/connection/authentication/UserNamePasswordAuthenticationStrategy.java) to connect to our database.

4. **Authentication Flow**: Now we will add an authentication flow for SqlServer using StaticDatasourceSpecification with UsernamePasswordAuthenticationStrategy.
Let's create a class SqlServerStaticWithUserPasswordFlow in sqlserver-execution module.

    ~~~java
    // Copyright 2021 Goldman Sachs
    //
    // Licensed under the Apache License, Version 2.0 (the "License");
    // you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at
    //
    //      http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing, software
    // distributed under the License is distributed on an "AS IS" BASIS,
    // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    // See the License for the specific language governing permissions and
    // limitations under the License.
     
    package org.finos.legend.engine.authentication.flows;
     
    import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
    import org.finos.legend.engine.shared.core.identity.Credential;
    import org.finos.legend.engine.shared.core.identity.Identity;
    import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
    import org.finos.legend.engine.shared.core.vault.Vault;
     
    public class SqlServerStaticWithUserPasswordFlow implements DatabaseAuthenticationFlow<StaticDatasourceSpecification, UserNamePasswordAuthenticationStrategy>
    {
        @Override
        public Class<StaticDatasourceSpecification> getDatasourceClass()
        {
            return StaticDatasourceSpecification.class;
        }
     
        @Override
        public Class<UserNamePasswordAuthenticationStrategy> getAuthenticationStrategyClass()
        {
            return UserNamePasswordAuthenticationStrategy.class;
        }
     
        @Override
        public DatabaseType getDatabaseType()
        {
            return DatabaseType.SqlServer;
        }
     
        @Override
        public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, UserNamePasswordAuthenticationStrategy authStrategy) throws Exception
        {
            String userNameVaultKey = authStrategy.baseVaultReference == null ? authStrategy.userNameVaultReference : authStrategy.baseVaultReference + authStrategy.userNameVaultReference;
            String passwordVaultKey = authStrategy.baseVaultReference == null ? authStrategy.passwordVaultReference : authStrategy.baseVaultReference + authStrategy.passwordVaultReference;
            String userName = Vault.INSTANCE.getValue(userNameVaultKey);
            String password = Vault.INSTANCE.getValue(passwordVaultKey);
            return new PlaintextUserPasswordCredential(userName, password);
        }
    }
    ~~~
    
    Take a look at how we are constructing the credentials which can be passed to a SqlServer instance, based on the information from authStrategy specification (which uses vault references, as we don't want to specify credentials directly. Various vault impls can be plugged in at runtime, and this code reads from available impls).
    
    You would also need to add following deps for the above logic:

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-shared-core</artifactId>
    </dependency>
             
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-executionPlan-connection-authentication</artifactId>
    </dependency>
    ~~~

    Now clean + install the sqlserver-execution module.

5. **Register Authentication Flow For Testing**: Next, we will register our authentication flow, so that it can be used for testing connectivity.

    Add the flow instance in SqlServerTestDatabaseAuthenticationFlowProvider.flows() in sqlserver-execution-tests module

    ~~~java
    return Lists.immutable.of(
        new  SqlServerStaticWithUserPasswordFlow()
    );
    ~~~
    
    You will also need to add the dependency of sqlserver-execution module in pom.xml of sqlserver-execution-tests

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-sqlserver-execution</artifactId>
    </dependency>
    ~~~
    
    SqlServerTestDatabaseAuthenticationFlowProviderConfig can be modified in future to have deployment level properties for auth.
    It can then be read from server config or a test config directly during initialization.

6. **Test Database Instance**: There are 2 ways to declare a test db. If you have an already hosted instance, then you can use define a static test connection.
If your database supports it, we can alternatively launch a test instance at runtime using docker (dynamic test connection).

    Let's try to define a dynamic test connection for SqlServer. Define the below class in src/main/java section of sqlserver-execution-tests module.

    ~~~java
    // Copyright 2020 Goldman Sachs
    //
    // Licensed under the Apache License, Version 2.0 (the "License");
    // you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at
    //
    //      http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing, software
    // distributed under the License is distributed on an "AS IS" BASIS,
    // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    // See the License for the specific language governing permissions and
    // limitations under the License.
     
    package org.finos.legend.engine.dynamicTestConnections;
     
    import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
    import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
    import org.finos.legend.engine.shared.core.vault.Vault;
    import org.finos.legend.engine.shared.core.vault.VaultImplementation;
    import org.testcontainers.containers.MSSQLServerContainer;
     
    import java.util.Properties;
     
    public class SqlServerUsingTestContainer implements DynamicTestConnection
    {
        @Override
        public DatabaseType getDatabaseType()
        {
            return DatabaseType.SqlServer;
        }
     
        public MSSQLServerContainer mssqlserver = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest")
                .acceptLicense();
        private VaultImplementation vaultImplementation;
     
        @Override
        public void setup()
        {
            this.startMSSQLServerContainer();
            this.registerVault();
        }
     
        private void startMSSQLServerContainer()
        {
            System.out.println("Starting setup of dynamic connection for database: SqlServer ");
     
            long start = System.currentTimeMillis();
            this.mssqlserver.start();
            String containerHost = this.mssqlserver.getHost();
            int containerPort = this.mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
            long end = System.currentTimeMillis();
     
            System.out.println("Completed setup of dynamic connection for database: SqlServer on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
        }
     
        public void registerVault()
        {
            Properties properties = new Properties();
            properties.put("sqlServerAccount.user", "SA");   // this username/password is used by the default account present in SqlServer
            properties.put("sqlServerAccount.password", "A_Str0ng_Required_Password");
            this.vaultImplementation = new PropertiesVaultImplementation(properties);
            Vault.INSTANCE.registerImplementation(this.vaultImplementation);
        }
     
        @Override
        public RelationalDatabaseConnection getConnection()
        {
            StaticDatasourceSpecification sqlServerDatasourceSpecification = new StaticDatasourceSpecification();
            sqlServerDatasourceSpecification.host = this.mssqlserver.getHost();
            sqlServerDatasourceSpecification.port = this.mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
            sqlServerDatasourceSpecification.databaseName = "master";
            UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
            authSpec.baseVaultReference = "sqlServerAccount.";
            authSpec.userNameVaultReference = "user";
            authSpec.passwordVaultReference = "password";
            RelationalDatabaseConnection conn = new RelationalDatabaseConnection(sqlServerDatasourceSpecification, authSpec, DatabaseType.SqlServer);
            conn.type = DatabaseType.SqlServer;         
            conn.element = "";                          
            return conn;
        }
     
        @Override
        public void cleanup()
        {
            Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
            this.mssqlserver.stop();
        }
    }
    ~~~
   
    NOTE: You may need to change the initialization of mssqlserver container like below, if using Apple Silicon processor.

    ~~~java
    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge")
            .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
            .acceptLicense();
    ~~~

    You can go through the above code to understand how we use the container provided by test-containers library to launch an instance of SqlServer, and
    then use a properties vault to store its user name / password, so that we can model the authentication strategy as part of connection specification.
    
    Running the setup() method in above class requires that you have a docker instance running on your machine. As expected, you will need to add following dependency for above class to compile

    ~~~xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mssqlserver</artifactId>
        <version>1.16.3</version>
    </dependency>
    ~~~

    Also, we need to define the file "org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection" in src/main/resources/META-INF/services with this content

    ~~~
    org.finos.legend.engine.dynamicTestConnections.SqlServerUsingTestContainer
    ~~~

    This file allows SqlServerUsingTestContainer to be discoverable at runtime.
    
    Alternatively, If you have a static test connection, you can define it in sqlServerRelationalDatabaseConnections.json. Look at [RelationalDatabaseConnection](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-protocol/src/main/java/org/finos/legend/engine/protocol/pure/v1/model/packageableElement/store/relational/connection/RelationalDatabaseConnection.java) to understand the structure.

7. **Connection Acquisition Test**: Now we will fix the ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_SqlServer test. When it passes, it will verify that we are able to connect to the test database instance.

    First, we need to fix the below line

    ~~~java
    SqlServerTestDatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration = null;
    ~~~

    If you don't need any configuration properties (which is the case for the tutorial right now), simply replacing null with new SqlServerTestDatabaseAuthenticationFlowProviderConfiguration() will do.
    Else, you can define it as a config file, and load it via RelationalConnectionTest.readDatabaseFlowProviderConfigurations().
    
    If you have a static test connection defined in sqlServerRelationalDatabaseConnections.json, then test should pass now.
    If your connection spec needs some vault secrets, you can inject them via env variables, thanks to the below code in test, which makes env variables available as vault values.

    ~~~java
    @BeforeClass
    public static void setupTest()
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }
    ~~~

    If however, you want to test using the dynamic test connection, we defined in previous section, then we can modify the test like this

    ~~~java
    @Test
    public void testConnectivity() throws Exception
    {
        SqlServerUsingTestContainer sqlServerUsingTestContainer = new SqlServerUsingTestContainer();
        sqlServerUsingTestContainer.setup();
        try
        {
            RelationalDatabaseConnection systemUnderTest = sqlServerUsingTestContainer.getConnection();
            Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>) null, systemUnderTest);
            testConnection(connection, 1, "select 1");
        }
        finally
        {
            sqlServerUsingTestContainer.cleanup();
        }
    }
    ~~~

    If you run the above test with docker running in background, the test should pass.

8. **Working With Pure Code**: Next, we will work with sql generation code, which is written in [PURE](https://legend.finos.org/docs/reference/legend-language) language.

    To edit pure code, you need to launch the pure ide. Find PureIDELight in src/test section of sqlserver-pure module, and run it. It will the print port on which the ide server is running (say 9200). Then you can hit localhost:9200/ide in browser to see the ide. Wait for loading process to complete.

    Open the files tab, and you can see core_relational_sqlserver/relational/sqlQueryToString folder.
    Folder would have 2 files, sqlServerExtension.pure (which contains sql generation code), and sqlServerTestSuiteInvoker (which contains the code to invoke the standard test suite against the sql generation code).
    Standard test suite contains generic tests parametrized against dbType and connection. You can browse them at core_relational/relational/sqlQueryToString/testSuite.

    Few helpful key-bindings: F9 saves/interprets pure code. Ctrl-b, jumps to the definition. Ctrl-Shift-n can be used to search for a file.
    
    In the concept tab, if you browse to package mentioned in sqlServerTestSuiteInvoker (ie, meta::relational::tests::sqlQueryToString::sqlServer), right click on SqlServer and select "run tests", it will run the tests, and show you the results in test window.
    You will see that all tests are failing with unsupported-api exception, as we haven't implemented any generation yet.

    Pure code gets compiled to java code, when we do clean+install on sqlserver-pure module.
    You can run the same set of tests from java side, by running Test_Pure_Relational_DbSpecific_SqlServer.
    If you run them now, you will see that all tests will be ignored at runtime as sql gen code fails with unsupported-api exception.
    You can use this exception message to signal, what parts of sql gen are supported and what is not yet implemented.
    
9. **Select Sql Gen Code**: The first thing we will implement is Select Expr generation. Write the following expression inside the processSelectSQLQueryForSqlServer in sqlServerExtension.pure (removing the placeholder to fail with unsupported-api message)

    ~~~java
    let opStr = if($s.filteringOperation->isEmpty(), |'', |$s.filteringOperation->map(s|$s->processOperation($dbConfig, $format->indent(), ^$config(callingFromFilter = true), $extensions))->filter(s|$s != '')->joinStrings(' <||> '));
     let havingStr = if($s.havingOperation->isEmpty(), |'', |$s.havingOperation->map(s|$s->processOperation($dbConfig, $format->indent(), $config, $extensions))->filter(s|$s != '')->joinStrings(' <||> '));
     
     assert($s.fromRow == [] && $s.toRow == [], '[unsupported-api] slice/limit operations are not implemented');
     assert($s.groupBy == [] && $havingStr == '', '[unsupported-api] groupBy/having operations are not implemented');
     
     $format.separator + 'select ' + processTop($s, $format) + if($s.distinct == true,|'distinct ',|'') +
     processSelectColumns($s.columns, $dbConfig, $format->indent(), true, $extensions) +
     if($s.data == [],|'',| ' ' + $format.separator + 'from ' + $s.data->toOne()->processJoinTreeNode([], $dbConfig, $format->indent(), [], $extensions)) +
     if (eq($opStr, ''), |'', | ' ' + $format.separator + 'where ' + $opStr) +
     if ($s.orderBy->isEmpty(),|'',| ' ' + $format.separator + 'order by '+ $s.orderBy->processOrderBy($dbConfig, $format->indent(), $config, $extensions)->makeString(','));
     ~~~
 
    This implements basic select functionality supporting where, joins and orderBy. Now if you run the tests from pure ide, you will see that some of the tests become green.
The framework is set in a way that as you incrementally implement more features, more tests will start passing, instead of being ignored.
You don't need to implement all the features before you can deploy and use the connector.

10. **Dyna Fn Sql Gen Code**: Dyna Fns are db agnostic abstractions over sql fns. We will implement some commons ones here. For full list, look at enum DynaFunctionRegistry in this [file](https://github.com/finos/legend-engine/blob/master/legend-engine-xt-relationalStore-pure/src/main/resources/core_relational/relational/sqlQueryToString/dbExtension.pure).
Populate the empty list in getDynaFnToSqlForSqlServer in sqlServerExtension.pure with the following

    ~~~java
    [
        dynaFnToSql('and',                    $allStates,            ^ToSql(format='%s', transform={p:String[*]|$p->makeString(' and ')})),
        dynaFnToSql('count',                  $allStates,            ^ToSql(format='count(%s)', transform={p:String[*]|if($p->isEmpty(),|'*',|$p)})),
        dynaFnToSql('equal',                  $allStates,            ^ToSql(format='%s = %s')),
        dynaFnToSql('greaterThan',            $allStates,            ^ToSql(format='%s > %s')),
        dynaFnToSql('greaterThanEqual',       $allStates,            ^ToSql(format='%s >= %s')),
        dynaFnToSql('if',                     $allStates,            ^ToSql(format='case when %s then %s else %s end', parametersWithinWhenClause = [true, false, false])),
        dynaFnToSql('in',                     $allStates,            ^ToSql(format='%s in %s', transform={p:String[2] | if($p->at(1)->startsWith('(') && $p->at(1)->endsWith(')'), | $p, | [$p->at(0), ('(' + $p->at(1) + ')')])})),
        dynaFnToSql('isEmpty',                $allStates,            ^ToSql(format='%s is null')),
        dynaFnToSql('isNotEmpty',             $allStates,            ^ToSql(format='%s is not null')),
        dynaFnToSql('isNotNull',              $allStates,            ^ToSql(format='%s is not null')),
        dynaFnToSql('isNull',                 $allStates,            ^ToSql(format='%s is null')),
        dynaFnToSql('lessThan',               $allStates,            ^ToSql(format='%s < %s')),
        dynaFnToSql('lessThanEqual',          $allStates,            ^ToSql(format='%s <= %s')),
        dynaFnToSql('notEqual',               $allStates,            ^ToSql(format='%s != %s')),
        dynaFnToSql('or',                     $allStates,            ^ToSql(format='%s', transform={p:String[*]|$p->makeString(' or ')})),
        dynaFnToSql('sqlNull',                $allStates,            ^ToSql(format='null'))
    ]
    ~~~

    Now again, if you run tests from pure ide, more tests will go green.

11. **DDL Sql Gen Code**: Some of tests need to create schemas/tables to run, so we also need to implement DDL code to do so. We can add the following in sqlServerExtension.pure

    ~~~java
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::getDDLCommandsTranslatorForSqlServer(): RelationalDDLCommandsTranslator[1]
    {
      ^RelationalDDLCommandsTranslator(
                    createSchema = translateCreateSchemaStatementForSqlServer_CreateSchemaSQL_1__String_MANY_,
                    dropSchema =  translateDropSchemaStatementForSqlServer_DropSchemaSQL_1__String_MANY_,
                    createTable =  translateCreateTableStatementForSqlServer_CreateTableSQL_1__DbConfig_1__String_MANY_,
                    dropTable = translateDropTableStatementForSqlServer_DropTableSQL_1__String_MANY_,
                    loadTable =  loadValuesToDbTableForSqlServer_LoadTableSQL_1__DbConfig_1__String_MANY_
                  );
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::translateCreateSchemaStatementForSqlServer(createSchemaSQL:CreateSchemaSQL[1]) : String[*]
    {
       // sql is enclosed in [] to signify that its failure due to schema already existing should not stop us from running other DDL commands
       if($createSchemaSQL.schema.name == 'default', |[], |'[Create Schema ' + $createSchemaSQL.schema.name + ';]');
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::translateDropSchemaStatementForSqlServer(dropSchemaSQL:DropSchemaSQL[1]) : String[*]
    {
       // dropping the schema is not needed for test setup
       [];
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::translateDropTableStatementForSqlServer(dropTableSQL:DropTableSQL[1]) : String[*]
    {
      let t = $dropTableSQL.table;
      'Drop table if exists '+if($t.schema.name == 'default',|'',|$t.schema.name+'.')+$t.name+';';
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::translateCreateTableStatementForSqlServer(createTableSQL:CreateTableSQL[1], dbConfig:DbConfig[1]) : String[*]
    {
      let t = $createTableSQL.table;
      let applyConstraints = $createTableSQL.applyConstraints;
      'Create Table '+if($t.schema.name == 'default',|'',|$t.schema.name+'.')+$t.name+
          + '('
          + $t.columns->cast(@meta::relational::metamodel::Column)
             ->map(c | $c.name->processColumnName($dbConfig) + ' ' +  getColumnTypeSqlTextForSqlServer($c.type) + if($c.nullable->isEmpty() || $applyConstraints == false, | '', | if($c.nullable == true , | ' NULL', | ' NOT NULL' )))
            ->joinStrings(',')
          + if ($t.primaryKey->isEmpty() || $applyConstraints == false, | '', | ', PRIMARY KEY(' + $t.primaryKey->map(c | $c.name)->joinStrings(',') + ')')
          +');';
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::getColumnTypeSqlTextForSqlServer(columnType:meta::relational::metamodel::datatype::DataType[1]):String[1]
    {
       $columnType->match([
          s : meta::relational::metamodel::datatype::Timestamp[1] | 'datetime',
          a : Any[*] | meta::relational::metamodel::datatype::dataTypeToSqlText($columnType)
       ])
    }
     
    function <<access.private>> meta::relational::functions::sqlQueryToString::sqlServer::loadValuesToDbTableForSqlServer(loadTableSQL:LoadTableSQL[1] , dbConfig: DbConfig[1]) : String[*]
    {
        'insert into ' + if($loadTableSQL.table.schema.name=='default', |'' ,|$loadTableSQL.table.schema.name + '.') + $loadTableSQL.table.name + ' ('
            + $loadTableSQL.columnsToLoad.name->map(colName | $colName->processColumnName($dbConfig))->joinStrings(',')
            + ') values '
            + $loadTableSQL.parsedData.values->map(row | '('
                + $row.values->meta::relational::functions::database::testDataSQLgeneration::convertValuesToCsv($loadTableSQL.columnsToLoad.type)
                + ')')->makeString(',') + ';';
    }
    ~~~

    Now we can enable the DDL functionality by registering it in createDbExtensionForSqlServer() of sqlServerExtension.pure like this

    ~~~
    ...
    ^DbExtension(
        ...
        dynaFuncDispatch = $dynaFuncDispatch,
        ddlCommandsTranslator = getDDLCommandsTranslatorForSqlServer()
    );
    ~~~

    We can now do a clean+install on sqlserver-pure module, and run Test_Pure_Relational_DbSpecific_SqlServer to make sure that our generation code behaves the same way from java side, and pure to java compilation works as expected. You will see that some tests will go green now, instead of everything getting ignored. 

12. **Sql Execution Test**: This is the last step of verifying that our connector works.
We will now execute the generated sql against a real database instance, and make sure that it gives expected result.

    We can use the same test db instance as the one we used in Connection Acquisition Test.
    In the userTestConfig_withSqlServerTestConnection.json of sqlserver-execution-tests module, we can add the test connection detail under either staticTestConnections field, or add the creator class in dynamicTestConnectionCreators field.
    
    Since, we earlier used the dynamic connection based on docker, we will continue with same here, and add

    ~~~
    "dynamicTestConnectionCreators" : {
        "sqlServer" : "org.finos.legend.engine.dynamicTestConnections.SqlServerUsingTestContainer"
    }
    ~~~
    
    Apart from this, you may need to add the following test dependency in sqlserver- execution-tests module if not already added (we need the relational binding for the target platform at runtime, which is java in this case).

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-javaPlatformBinding-pure</artifactId>
        <scope>test</scope>
    </dependency>
    ~~~
    
    Now you can run the Test_Relational_DbSpecific_SqlServer_UsingPureClientTestSuite with docker running in background. If no tests fails, you can be sure that we have successfully, 1) established the connection 2) generated the sql 3) executed the sql, and asserted expected results.
    
    **Congratulations on completing the connector!**

### Executing against database from Pure Ide

In the tutorial, we test sql generation in pure ide, but for execution against db, we have to clean+install the sqlserver-pure module and then run the test from java.
To speed up the development cycle, you can alternatively launch a test instance of engine server from java and link it with Pure Ide.
Then, when you run the tests from Pure Ide, they will run both generation and execution against database. Here are the steps to do that:

* Run SqlServerRelationalTestServerInvoker from sqlserver-execution-tests module. Say it is running at 6060.
* Add following vm options to PureIDELight: -Dlegend.test.server.host=127.0.0.1 -Dlegend.test.server.port=6060 -Dlegend.test.clientVersion=vX_X_X -Dlegend.test.serverVersion=v1
* Run PureIDELight

### Adding a new Datasource or AuthenticationStrategy Specification

Let's say we want to support a new data source specification specific to SqlServer called SqlServerTutorialDatasourceSpecification, which behaves similar to StaticDatasourceSpecification.
Similarly, we will add a new authentication strategy specification specific to SqlServer called SqlServerTutorialAuthenticationStrategy, which behaves similar to UserNamePasswordAuthenticationStrategy.

1. **Protocol Classes**: Define the following classes in sqlserver-protocol module:

    ~~~java
    // Copyright 2020 Goldman Sachs
    //
    // Licensed under the Apache License, Version 2.0 (the "License");
    // you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at
    //
    //      http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing, software
    // distributed under the License is distributed on an "AS IS" BASIS,
    // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    // See the License for the specific language governing permissions and
    // limitations under the License.
    
    package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;
    
    public class SqlServerTutorialDatasourceSpecification extends DatasourceSpecification
    {
        public String host;
        public int port;
        public String databaseName;
    
        @Override
        public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
        {
            return datasourceSpecificationVisitor.visit(this);
        }
    }
    ~~~

    ~~~java
    // Copyright 2021 Goldman Sachs
    //
    // Licensed under the Apache License, Version 2.0 (the "License");
    // you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at
    //
    //      http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing, software
    // distributed under the License is distributed on an "AS IS" BASIS,
    // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    // See the License for the specific language governing permissions and
    // limitations under the License.
    
    package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;
    
    public class SqlServerTutorialAuthenticationStrategy extends AuthenticationStrategy
    {
        public String baseVaultReference;
        public String userNameVaultReference;
        public String passwordVaultReference;
    
        @Override
        public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
        {
            return authenticationStrategyVisitor.visit(this);
        }
    }
    ~~~

    Next register these classes in SqlServerProtocolExtension

    ~~~java
    return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                //DatasourceSpecification
                ProtocolSubTypeInfo.newBuilder(DatasourceSpecification.class)
                    .withSubtype(SqlServerTutorialDatasourceSpecification.class, "sqlServerTutorial")
                    .build(),
                // AuthenticationStrategy
                ProtocolSubTypeInfo.newBuilder(AuthenticationStrategy.class)
                    .withSubtype(SqlServerTutorialAuthenticationStrategy.class, "sqlServerTutorialAuth")
                    .build()
            ));
    ~~~

    If in future, you want to add more DataSource/AuthStratgey specs, you can add them by calling withSubType() again.
    Also, note that we are adding these specs in sqlserver-protocol because they are associated with SqlServer.
    For generic specs that can be used across dbTypes, legend-engine-xt-relationalStore-protocol would be the right place.

    Clean + install the sqlserver-protocol module before moving on.

2. **Pure Metamodel and Protocol**: Create a pure file core_relational_sqlserver/relational/connection/metamodel.pure with following content

    ~~~java
    Class meta::pure::alloy::connections::alloy::specification::SqlServerTutorialDatasourceSpecification extends meta::pure::alloy::connections::alloy::specification::DatasourceSpecification
    {
       host: String[1];
       port: Integer[1];
       databaseName: String[1];
    }
    
    Class meta::pure::alloy::connections::alloy::authentication::SqlServerTutorialAuthenticationStrategy extends meta::pure::alloy::connections::alloy::authentication::AuthenticationStrategy
    {
        baseVaultReference: String[0..1];
        userNameVaultReference: String[1];
        passwordVaultReference: String[1];
    }
    ~~~

    Next, we will convert these metamodel classes to semantically versioned protocol classes. First define the latest version (vX_X_X) of protocol classes in core_relational_sqlserver/relational/protocols/pure/vX_X_X/model/metamodel_connection.pure

    ~~~java
    Class meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::specification::SqlServerTutorialDatasourceSpecification extends meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::specification::DatasourceSpecification
    {
       host: String[1];
       port: Integer[1];
       databaseName: String[1];
    }
    
    Class meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::authentication::SqlServerTutorialAuthenticationStrategy extends meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::authentication::AuthenticationStrategy
    {
        baseVaultReference: String[0..1];
        userNameVaultReference: String[1];
        passwordVaultReference: String[1];
    }
    ~~~

    Then define the conversion logic in sqlServerSerializerExtension() function within core_relational_sqlserver/relational/protocols/pure/vX_X_X/extension/extension_sqlServer.pure

    ~~~java
    ^meta::protocols::pure::vX_X_X::extension::RelationalModuleSerializerExtension(
        module = 'sqlServer',
        transfers_connection_transformAuthenticationStrategy = [
            u:meta::pure::alloy::connections::alloy::authentication::SqlServerTutorialAuthenticationStrategy[1] |
                ^meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::authentication::SqlServerTutorialAuthenticationStrategy(
                         _type = 'sqlServerTutorialAuth',
                         baseVaultReference = $u.baseVaultReference,
                         userNameVaultReference = $u.userNameVaultReference,
                         passwordVaultReference = $u.passwordVaultReference
                      )
        ],
        transfers_connection_transformDatasourceSpecification = [
            s:meta::pure::alloy::connections::alloy::specification::SqlServerTutorialDatasourceSpecification[1] |
                     ^meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::specification::SqlServerTutorialDatasourceSpecification(
                        _type = 'sqlServerTutorial',
                        host = $s.host,
                        port = $s.port,
                        databaseName = $s.databaseName
                     )
        ],
        reverse_transfers_typeLookups = [
            pair('sqlServerTutorial', 'SqlServerTutorialDatasourceSpecification'),
            pair('sqlServerTutorialAuth', 'SqlServerTutorialAuthenticationStrategy')
        ],
        reverse_transfers_connection_transformAuthenticationStrategy = [
            u:meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::authentication::SqlServerTutorialAuthenticationStrategy[1] |
                      ^meta::pure::alloy::connections::alloy::authentication::SqlServerTutorialAuthenticationStrategy(
                          baseVaultReference = $u.baseVaultReference,
                          userNameVaultReference = $u.userNameVaultReference,
                          passwordVaultReference = $u.passwordVaultReference
                        )
        ],
        reverse_transfers_connection_transformDatasourceSpecification = [
            s:meta::protocols::pure::vX_X_X::metamodel::store::relational::connection::alloy::specification::SqlServerTutorialDatasourceSpecification[1] |
                     ^meta::pure::alloy::connections::alloy::specification::SqlServerTutorialDatasourceSpecification(
                        host = $s.host,
                        port = $s.port,
                        databaseName = $s.databaseName
                     )
        ]
      )
    ~~~

    Clean + install the sqlserver-pure module.

3. **DSLs for Specifications**: We will work within sqlserver-grammar module to implement the DSLs, using [ANTLR](https://www.antlr.org). End users write DSLs in Studio instead of working with protocol classes. Write the following in SqlServerLexerGrammar.g4 to parse tokens from DSL text.

    ~~~antlrv4
    SQL_SERVER_TUTORIAL_DSP:                    'SqlServerTutorial';
    HOST:                                       'host';
    PORT:                                       'port';
    NAME:                                       'name';
    
    SQL_SERVER_TUTORIAL_AUTH:                   'SqlServerTutorialAuth';
    BASE_VAULT_REF:                             'baseVaultReference';
    USERNAME_VAULT_REF:                         'userNameVaultReference';
    PASSWORD_VAULT_REF:                         'passwordVaultReference';
    ~~~

    Then write the following in SqlServerParserGrammar.g4 to create parse trees out of tokens obtained from lexer.

    ~~~antlrv4
    sqlServerTutorialDsp:                       SQL_SERVER_TUTORIAL_DSP
                                                    BRACE_OPEN
                                                        (
                                                            dbName
                                                            | dbHost
                                                            | dbPort
                                                        )*
                                                    BRACE_CLOSE
    ;
    dbPort:                                     PORT COLON INTEGER SEMI_COLON
    ;
    dbHost:                                     HOST COLON STRING SEMI_COLON
    ;
    dbName:                                     NAME COLON STRING SEMI_COLON
    ;
    
    sqlServerTutorialAuth:                  SQL_SERVER_TUTORIAL_AUTH
                                                BRACE_OPEN
                                                    (
                                                        baseVaultRef
                                                        | userNameVaultRef
                                                        | passwordVaultRef
                                                    )*
                                                BRACE_CLOSE
    ;
    baseVaultRef:                           BASE_VAULT_REF COLON STRING SEMI_COLON
    ;
    userNameVaultRef:                       USERNAME_VAULT_REF COLON STRING SEMI_COLON
    ;
    passwordVaultRef:                       PASSWORD_VAULT_REF COLON STRING SEMI_COLON
    ;
    ~~~
   
    Clean + install the module so that java classes for the parser are generated by ANTLR.

    Now, we will implement SqlServerGrammarParserExtension, which converts DSLs to protocol classes using the parser generated by ANTLR.

    ~~~java
    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("SqlServerTutorialAuth".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, SqlServerLexerGrammar::new, SqlServerParserGrammar::new,
                        p -> visitSqlServerTutorialAuth(code, p.sqlServerTutorialAuth()));
            }
            return null;
        });
    }
    
    private SqlServerTutorialAuthenticationStrategy visitSqlServerTutorialAuth(AuthenticationStrategySourceCode code, SqlServerParserGrammar.SqlServerTutorialAuthContext authCtx)
    {
        SqlServerTutorialAuthenticationStrategy authStrategy = new SqlServerTutorialAuthenticationStrategy();
        authStrategy.sourceInformation = code.getSourceInformation();
        SqlServerParserGrammar.BaseVaultRefContext baseVaultRef = PureGrammarParserUtility.validateAndExtractOptionalField(authCtx.baseVaultRef(), "baseVaultReference", authStrategy.sourceInformation);
        authStrategy.baseVaultReference = baseVaultRef == null ? null : PureGrammarParserUtility.fromGrammarString(baseVaultRef.STRING().getText(), true);
        SqlServerParserGrammar.UserNameVaultRefContext userNameVaultRef = PureGrammarParserUtility.validateAndExtractRequiredField(authCtx.userNameVaultRef(), "userNameVaultReference", authStrategy.sourceInformation);
        authStrategy.userNameVaultReference = PureGrammarParserUtility.fromGrammarString(userNameVaultRef.STRING().getText(), true);
        SqlServerParserGrammar.PasswordVaultRefContext passwordVaultRef = PureGrammarParserUtility.validateAndExtractRequiredField(authCtx.passwordVaultRef(), "passwordVaultReference", authStrategy.sourceInformation);
        authStrategy.passwordVaultReference = PureGrammarParserUtility.fromGrammarString(passwordVaultRef.STRING().getText(), true);
        return authStrategy;
    }
    
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("SqlServerTutorial".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, SqlServerLexerGrammar::new, SqlServerParserGrammar::new,
                        p -> visitSqlServerTutorialDsp(code, p.sqlServerTutorialDsp()));
            }
            return null;
        });
    }
    
    private SqlServerTutorialDatasourceSpecification visitSqlServerTutorialDsp(DataSourceSpecificationSourceCode code, SqlServerParserGrammar.SqlServerTutorialDspContext dbSpecCtx)
    {
        SqlServerTutorialDatasourceSpecification dsSpec = new SqlServerTutorialDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // host
        SqlServerParserGrammar.DbHostContext hostCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbHost(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true);
        // port
        SqlServerParserGrammar.DbPortContext portCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbPort(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.parseInt(portCtx.INTEGER().getText());
        // database name
        SqlServerParserGrammar.DbNameContext nameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbName(), "name", dsSpec.sourceInformation);
        dsSpec.databaseName = PureGrammarParserUtility.fromGrammarString(nameCtx.STRING().getText(), true);
        return dsSpec;
    }
    ~~~
   
    As the above code references protocol classes, we will also need to add the following dependency in pom.xml of sqlserver-grammar

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-sqlserver-protocol</artifactId>
    </dependency>
    ~~~
   
    What we did till now allows us to parse DSLs into protocol classes, but we need to implement code for conversion in other direction too. In SqlServerGrammarComposerExtension, we can write

    ~~~java
    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((_strategy, context) ->
        {
            if (_strategy instanceof SqlServerTutorialAuthenticationStrategy)
            {
                SqlServerTutorialAuthenticationStrategy auth = (SqlServerTutorialAuthenticationStrategy) _strategy;
                int baseIndentation = 1;
                return "SqlServerTutorialAuth" +
                        "\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (auth.baseVaultReference == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "baseVaultReference: " + convertString(auth.baseVaultReference, true) + ";\n") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "userNameVaultReference: " + convertString(auth.userNameVaultReference, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "passwordVaultReference: " + convertString(auth.passwordVaultReference, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
    
    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof SqlServerTutorialDatasourceSpecification)
            {
                SqlServerTutorialDatasourceSpecification spec = (SqlServerTutorialDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "SqlServerTutorial\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.databaseName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
    ~~~

    We can now complete TestSqlServerConnectionGrammarRoundtrip.testConnectionGrammar() to test both of the above conversions (DSLs to protocol classes and vice-versa) via a round-trip test.

    ~~~java
    @Test
    public void testConnectionGrammar()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::StaticConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: SqlServer;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "}\n");
    }
    ~~~
   
    If you run the test, it should pass.

4. **Java Protocol to Pure Bridge**: Write the following code in SqlServerCompilerExtension of sqlserver-grammar module to implement the bridge. We will convert java protocol classes to pure metamodel classes.

    ~~~java
    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((authenticationStrategy, context) ->
        {
            if (authenticationStrategy instanceof SqlServerTutorialAuthenticationStrategy)
            {
                SqlServerTutorialAuthenticationStrategy tutorialAuthenticationStrategy = (SqlServerTutorialAuthenticationStrategy) authenticationStrategy;
                return new Root_meta_pure_alloy_connections_alloy_authentication_SqlServerTutorialAuthenticationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::authentication::SqlServerTutorialAuthenticationStrategy"))
                        ._baseVaultReference(tutorialAuthenticationStrategy.baseVaultReference)
                        ._userNameVaultReference(tutorialAuthenticationStrategy.userNameVaultReference)
                        ._passwordVaultReference(tutorialAuthenticationStrategy.passwordVaultReference);
            }
            return null;
        });
    }
    
    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof SqlServerTutorialDatasourceSpecification)
            {
                SqlServerTutorialDatasourceSpecification tutorialDatasourceSpecification = (SqlServerTutorialDatasourceSpecification) datasourceSpecification;
                return new Root_meta_pure_alloy_connections_alloy_specification_SqlServerTutorialDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::SqlServerTutorialDatasourceSpecification"))
                        ._host(tutorialDatasourceSpecification.host)
                        ._port(tutorialDatasourceSpecification.port)
                        ._databaseName(tutorialDatasourceSpecification.databaseName);
            }
            return null;
        });
    }
    ~~~
   
    Since above code references Pure metamodel classes, we need the following dependency in pom.xml of sqlserver-grammar module.

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-sqlserver-pure</artifactId>
    </dependency>
    ~~~
   
    Clean + install the sqlserver-grammar module to complete the step.

5. **Authentication Flow**: In sqlserver-execution module, we can create a new class to specify how to authenticate SqlServer with SqlServerTutorialDatasourceSpecification and SqlServerTutorialAuthenticationStrategy.
It is similar to how we added flow for authenticating using pre-existing StaticDatasourceSpecification and UserNamePasswordAuthenticationStrategy, in earlier part of the tutorial.
Flows can potentially be created using all possible combinations of DatasourceSpecification impls and AuthenticationStrategy impls. The flow we are implementing below is just one such possible flow.

    ~~~java
    // Copyright 2021 Goldman Sachs
    //
    // Licensed under the Apache License, Version 2.0 (the "License");
    // you may not use this file except in compliance with the License.
    // You may obtain a copy of the License at
    //
    //      http://www.apache.org/licenses/LICENSE-2.0
    //
    // Unless required by applicable law or agreed to in writing, software
    // distributed under the License is distributed on an "AS IS" BASIS,
    // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    // See the License for the specific language governing permissions and
    // limitations under the License.
    
    package org.finos.legend.engine.authentication.flows;
    
    import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SqlServerTutorialAuthenticationStrategy;
    import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SqlServerTutorialDatasourceSpecification;
    import org.finos.legend.engine.shared.core.identity.Credential;
    import org.finos.legend.engine.shared.core.identity.Identity;
    import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
    import org.finos.legend.engine.shared.core.vault.Vault;
    
    public class SqlServerTutorialAuthFlow implements DatabaseAuthenticationFlow<SqlServerTutorialDatasourceSpecification, SqlServerTutorialAuthenticationStrategy>
    {
        @Override
        public Class<SqlServerTutorialDatasourceSpecification> getDatasourceClass()
        {
            return SqlServerTutorialDatasourceSpecification.class;
        }
    
        @Override
        public Class<SqlServerTutorialAuthenticationStrategy> getAuthenticationStrategyClass()
        {
            return SqlServerTutorialAuthenticationStrategy.class;
        }
    
        @Override
        public DatabaseType getDatabaseType()
        {
            return DatabaseType.SqlServer;
        }
    
        @Override
        public Credential makeCredential(Identity identity, SqlServerTutorialDatasourceSpecification datasourceSpecification, SqlServerTutorialAuthenticationStrategy authStrategy) throws Exception
        {
            String userNameVaultKey = authStrategy.baseVaultReference == null ? authStrategy.userNameVaultReference : authStrategy.baseVaultReference + authStrategy.userNameVaultReference;
            String passwordVaultKey = authStrategy.baseVaultReference == null ? authStrategy.passwordVaultReference : authStrategy.baseVaultReference + authStrategy.passwordVaultReference;
            String userName = Vault.INSTANCE.getValue(userNameVaultKey);
            String password = Vault.INSTANCE.getValue(passwordVaultKey);
            return new PlaintextUserPasswordCredential(userName, password);
        }
    }
    ~~~
   
    Since, above code references protocol classes, we would need to add the following dependency, for it to compile.

    ~~~xml
    <dependency>
        <groupId>org.finos.legend.engine</groupId>
        <artifactId>legend-engine-xt-relationalStore-sqlserver-protocol</artifactId>
    </dependency>
    ~~~
   
6. **Keys for Connection Pooling**: TODO

7. **Connection Runtime**: TODO.
Finally, you can follow steps 5, 6, 7 of the main tutorial to test connectivity to a test db instance with newly implemented data source and auth strategy specifications.
