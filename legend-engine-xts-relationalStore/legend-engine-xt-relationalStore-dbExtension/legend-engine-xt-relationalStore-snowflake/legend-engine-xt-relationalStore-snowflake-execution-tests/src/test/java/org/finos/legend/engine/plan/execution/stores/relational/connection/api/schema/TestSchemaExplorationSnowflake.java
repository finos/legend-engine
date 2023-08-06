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


package org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema.model.DatabaseBuilderInput;
import org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema.model.DatabasePattern;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.BigInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.DataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Date;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Decimal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class TestSchemaExplorationSnowflake
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private ConnectionManagerSelector connectionManager;

    @Before
    public void setup()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
    }

    // deliberately ignored. This test required credentials. Search for XXXXX
    @Ignore
    public void test1() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;

        ImmutableMap<String, String> expectedObjects = Maps.immutable.of(
                "DEMO_SCHEMA1", "FIRM,PERSON",
                "INFORMATION_SCHEMA", "APPLICABLE_ROLES,COLUMNS,DATABASES,ENABLED_ROLES,EVENT_TABLES,EXTERNAL_TABLES,FILE_FORMATS,FUNCTIONS,INFORMATION_SCHEMA_CATALOG_NAME,LOAD_HISTORY,OBJECT_PRIVILEGES,PACKAGES,PIPES,PROCEDURES,REFERENTIAL_CONSTRAINTS,REPLICATION_DATABASES,REPLICATION_GROUPS,SCHEMATA,SEQUENCES,SERVICES,STAGES,TABLES,TABLE_CONSTRAINTS,TABLE_PRIVILEGES,TABLE_STORAGE_METRICS,USAGE_PRIVILEGES,VIEWS"
        );
        testImpl(true, databaseBuilderInput, expectedObjects);
        testImpl(false, databaseBuilderInput, expectedObjects);
    }

    // deliberately ignored. This test required credentials. Search for XXXXX
    @Ignore
    public void test2() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = false;

        ImmutableMap<String, String> expectedObjects = Maps.immutable.of(
                "DEMO_SCHEMA1", "",
                "INFORMATION_SCHEMA", ""
        );
        testImpl(true, databaseBuilderInput, expectedObjects);
        testImpl(false, databaseBuilderInput, expectedObjects);
    }

    // deliberately ignored. This test required credentials. Search for XXXXX
    @Ignore
    public void test3() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern("INFORMATION_SCHEMA", "FILE_FORMATS", true, true));

        ImmutableMap<String, String> expectedObjects = Maps.immutable.of(
                "INFORMATION_SCHEMA", "FILE_FORMATS"
        );
        testImpl(true, databaseBuilderInput, expectedObjects);
        testImpl(false, databaseBuilderInput, expectedObjects);
    }

    // deliberately ignored. This test required credentials. Search for XXXXX
    @Ignore
    public void test4() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern("DEMO_SCHEMA1", "PERSON", true, true));

        ImmutableMap<String, String> expectedObjects = Maps.immutable.of(
                "DEMO_SCHEMA1", "PERSON"
        );
        testImpl(true, databaseBuilderInput, expectedObjects);
        testImpl(false, databaseBuilderInput, expectedObjects);
    }

    public void testImpl(boolean localMode, DatabaseBuilderInput databaseBuilderInput, ImmutableMap<String, String> objects) throws Exception
    {
        databaseBuilderInput.connection = localMode ? this.createSnowflakeLocalConnection() : this.createSnowflakeFullConnection();
        Database expected =  buildExpectedDatabase(objects);
        test(databaseBuilderInput, expected);
    }

    public Database buildExpectedDatabase(ImmutableMap<String, String> expectedObjects)
    {
        Database database = new Database();
        database.name = "db";
        database._package = "my::package";
        database.schemas = FastList.newList();

        for (String schemaName : expectedObjects.keysView())
        {
            Schema schema = new Schema();
            schema.name = schemaName;

            String tablesNamesFullString = expectedObjects.get(schemaName);
            if (!tablesNamesFullString.isEmpty())
            {
                FastList<String> tableNames = FastList.newListWith(tablesNamesFullString.split(","));
                schema.tables = FastList.newList();
                ListIterate.forEach(tableNames, name ->
                {
                    Table table = new Table();
                    table.name = name;
                    schema.tables.add(table);
                });
            }

            database.schemas.add(schema);
        }
        return database;
    }

    private RelationalDatabaseConnection createSnowflakeLocalConnection()
    {

        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-accountName", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-region", "prod3.us-west-2");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-warehouseName", "demo_wh1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-databaseName", "demo_db1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-cloudType", "aws");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-role", "demo_role1");

        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-privateKeyVaultReference", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-passphraseVaultReference", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-publicuserName", "demo_user1");

        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(snowflakeLocalDataSourceSpecFileProperties));

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();

        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "legend-local-snowflake-accountName";
        snowflakeDatasourceSpecification.databaseName = "legend-local-snowflake-databaseName";
        snowflakeDatasourceSpecification.role = "legend-local-snowflake-role";
        snowflakeDatasourceSpecification.warehouseName = "legend-local-snowflake-warehouseName";
        snowflakeDatasourceSpecification.region = "legend-local-snowflake-region";
        snowflakeDatasourceSpecification.cloudType = "legend-local-snowflake-cloudType";

        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        authenticationStrategy.privateKeyVaultReference = "legend-local-snowflake-privateKeyVaultReference";
        authenticationStrategy.passPhraseVaultReference = "legend-local-snowflake-passphraseVaultReference";
        authenticationStrategy.publicUserName = "legend-local-snowflake-publicuserName";

        connection.authenticationStrategy = authenticationStrategy;
        connection.datasourceSpecification = snowflakeDatasourceSpecification;
        connection.type = DatabaseType.Snowflake;

        return connection;
    }

    private RelationalDatabaseConnection createSnowflakeFullConnection()
    {
        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        snowflakeLocalDataSourceSpecFileProperties.setProperty("privateKeyVaultReference", "XXXX");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("passphraseVaultReference", "XXXX");

        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(snowflakeLocalDataSourceSpecFileProperties));

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();

        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "sfdataengsandboxd01";
        snowflakeDatasourceSpecification.databaseName = "demo_db1";
        snowflakeDatasourceSpecification.role = "demo_role1";
        snowflakeDatasourceSpecification.warehouseName = "demo_wh1";
        snowflakeDatasourceSpecification.region = "prod3.us-west-2";
        snowflakeDatasourceSpecification.cloudType = "aws";

        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        authenticationStrategy.privateKeyVaultReference = "privateKeyVaultReference";
        authenticationStrategy.passPhraseVaultReference = "passphraseVaultReference";
        authenticationStrategy.publicUserName = "demo_user1";

        connection.authenticationStrategy = authenticationStrategy;
        connection.datasourceSpecification = snowflakeDatasourceSpecification;
        connection.type = DatabaseType.Snowflake;

        return connection;
    }

    private void test(DatabaseBuilderInput input, Database expected) throws Exception
    {
        SchemaExportation builder = SchemaExportation.newBuilder(input);
        Database store = builder.build(this.connectionManager, null);
        sort(store);
        sort(expected);
        Assert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(store));
    }

    private void sort(Database database)
    {
        database.schemas.sort(Comparator.comparing(a -> a.name));
        database.schemas.forEach(schema ->
        {
            schema.tables.sort(Comparator.comparing(a -> a.name));
            if (schema.tables != null)
            {
                schema.tables.forEach(table ->
                {
                    if (table.columns != null)
                    {
                        table.columns.sort(Comparator.comparing(a -> a.name));
                    }
                });
            }
        });
    }


    private Schema createCommonSchema(String name)
    {
        Schema schema = new Schema();
        schema.name = name;
        schema.tables = FastList.newListWith(
                createCommonTable("TABLE_1"),
                createCommonTable("TABLE11")
        );

        return schema;
    }

    private Table createCommonTable(String name)
    {
        VarChar varchar = new VarChar();
        varchar.size = 100;

        Table table = new Table();
        table.name = name;
        table.primaryKey = FastList.newListWith("INT", "TINYINT");
        table.columns = FastList.newListWith(
                createColumn("INT", new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer(), false),
                createColumn("BOOLEAN", new Bit(), false),
                createColumn("TINYINT", new TinyInt(), false),
                createColumn("SMALLINT", new SmallInt(), false),
                createColumn("BIGINT", new BigInt(), false),
                createColumn("DECIMAL", new Decimal(), false),
                createColumn("DOUBLE", new Double(), false),
                createColumn("DATE", new Date(), false),
                createColumn("TIMESTAMP", new Timestamp(), false),
                createColumn("VARCHAR", varchar, true)
        );

        return table;
    }

    private Column createColumn(String name, DataType type, boolean nullable)
    {
        Column column = new Column();
        column.name = name;
        column.type = type;
        column.nullable = nullable;

        return column;
    }

    private RelationalDatabaseConnection createCommonConnection()
    {
        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
        AuthenticationStrategy authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        LocalH2DatasourceSpecification datasourceSpecification = new LocalH2DatasourceSpecification();
        datasourceSpecification.testDataSetupSqls = createTestSQL();
        datasourceSpecification.testDataSetupCsv = "----";

        connection.authenticationStrategy = authenticationStrategy;
        connection.datasourceSpecification = datasourceSpecification;
        connection.type = DatabaseType.H2;

        return connection;
    }

    private List<String> createTestSQL()
    {
        return ListIterate.flatCollect(FastList.newListWith(
                FastList.newListWith("DROP ALL OBJECTS"),
                createCommonSchemaStatements("SCHEMA_1"),
                createCommonSchemaStatements("SCHEMA11"),
                createCommonTableStatements("SCHEMA_1", "TABLE_1"),
                createCommonTableStatements("SCHEMA_1", "TABLE11"),
                createCommonTableStatements("SCHEMA11", "TABLE_1"),
                createCommonTableStatements("SCHEMA11", "TABLE11")
        ), s -> s);
    }

    private List<String> createCommonTableStatements(String schema, String table)
    {
        return FastList.newListWith(
                dropTable(schema, table),
                createTable(schema, table,
                        FastList.newListWith(
                                "INT INT NOT NULL", "BOOLEAN BOOLEAN NOT NULL", "TINYINT TINYINT NOT NULL", "SMALLINT SMALLINT NOT NULL",
                                "BIGINT BIGINT NOT NULL", "DECIMAL DECIMAL NOT NULL", "DOUBLE DOUBLE NOT NULL",
                                "DATE DATE NOT NULL", "TIMESTAMP TIMESTAMP NOT NULL", "VARCHAR VARCHAR(100)"
                        ),
                        FastList.newListWith("INT", "TINYINT"))
        );
    }

    private String dropTable(String schema, String table)
    {
        return "Drop Table if exists " + schema + "." + table + ";";
    }

    private String createTable(String schema, String table, MutableList<String> columns, MutableList<String> pks)
    {
        return "Create Table " + schema + "." + table + "(" + columns.makeString(", ") + ", PRIMARY KEY (" + pks.makeString(", ") + "));";
    }

    private List<String> createCommonSchemaStatements(String name)
    {
        return FastList.newListWith(
                "Drop Schema if exists " + name + ";",
                "Create Schema " + name + ";"
        );
    }

}

