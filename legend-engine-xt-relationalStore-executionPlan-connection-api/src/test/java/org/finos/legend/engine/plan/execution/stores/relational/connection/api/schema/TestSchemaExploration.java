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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

public class TestSchemaExploration
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    private ConnectionManagerSelector connectionManager;

    @Before
    public void setup()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
    }

    @Test
    public void testDefaults() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newList(), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testEnrichTables() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1", "TABLE11"), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testEnrichTablesWithColumns() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.enrichColumns = true;

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1", "TABLE11"), true, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testEnrichTablesWithColumnsAndPKS() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.enrichColumns = true;
        databaseBuilderInput.config.enrichPrimaryKeys = true;

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1", "TABLE11"), true, true);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testFilterSchemasExact() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern("SCHEMA_1", null, true, true));

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1"), FastList.newListWith("TABLE_1", "TABLE11"), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testFilterSchemas() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern("SCHEMA_1", null));

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1", "TABLE11"), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testFilterTablesExact() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern(null, "TABLE_1", false, true));

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1"), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testFilterTables() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern(null, "TABLE_1", false, false));

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1", "SCHEMA11"), FastList.newListWith("TABLE_1", "TABLE11"), false, false);
        test(databaseBuilderInput, expected);
    }

    @Test
    public void testFilterTablesAndSchemasExact() throws Exception
    {
        DatabaseBuilderInput databaseBuilderInput = new DatabaseBuilderInput();
        databaseBuilderInput.connection = createCommonConnection();
        databaseBuilderInput.targetDatabase._package = "my::package";
        databaseBuilderInput.targetDatabase.name = "db";
        databaseBuilderInput.config.enrichTables = true;
        databaseBuilderInput.config.patterns = FastList.newListWith(new DatabasePattern("SCHEMA_1", "TABLE_1", true, true));

        Database expected = filterCommonDatabase(FastList.newListWith("SCHEMA_1"), FastList.newListWith("TABLE_1"), false, false);
        test(databaseBuilderInput, expected);
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

    private Database filterCommonDatabase(List<String> schemas, List<String> tables, boolean enrichColumns, boolean enrichPks)
    {
        Database database = createCommonDatabase();
        database.schemas = ListIterate.select(database.schemas, s -> schemas.contains(s.name));
        database.schemas.forEach(schema ->
        {
            schema.tables = ListIterate.select(schema.tables, t -> tables.contains(t.name));
            schema.tables.forEach(table ->
            {
                table.columns = enrichColumns ? table.columns : null;
                table.primaryKey = enrichPks ? table.primaryKey : null;
            });
        });

        return database;
    }

    private Database createCommonDatabase()
    {
        Table table = new Table();
        table.name = "db";

        Database database = new Database();
        database.name = "db";
        database._package = "my::package";
        database.schemas = FastList.newListWith(
                createCommonSchema("SCHEMA_1"),
                createCommonSchema("SCHEMA11")
        );

        return database;
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

