// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.exploration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.exploration.model.DatabaseBuilderConfig;
import org.finos.legend.engine.plan.execution.stores.relational.exploration.model.DatabaseBuilderInput;
import org.finos.legend.engine.plan.execution.stores.relational.exploration.model.DatabasePattern;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.TabularFunction;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.DataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Other;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;

public class SchemaExportation
{
    private final ConnectionManagerSelector connectionManager;
    private static final String ESCAPE_CHARS = " :";
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<Integer, Function<Long, DataType>> TYPE_MAP = UnifiedMap.newMapWith(
            Tuples.pair(Types.VARCHAR, size -> createDataType("Varchar", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar.class)),
            Tuples.pair(Types.LONGNVARCHAR, size -> createDataType("Varchar", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar.class)),
            Tuples.pair(Types.DATE, size -> createDataType("Date", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Date.class)),
            Tuples.pair(Types.FLOAT, size -> createDataType("Float", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Float.class)),
            Tuples.pair(Types.DOUBLE, size -> createDataType("Double", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double.class)),
            Tuples.pair(Types.NUMERIC, size -> createDataType("Numeric", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Numeric.class)),
            Tuples.pair(Types.DECIMAL, size -> createDataType("Decimal", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Decimal.class)),
            Tuples.pair(Types.BIT, size -> createDataType("Bit", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit.class)),
            Tuples.pair(Types.BOOLEAN, size -> createDataType("Bit", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit.class)),
            Tuples.pair(Types.INTEGER, size -> createDataType("Integer", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer.class)),
            Tuples.pair(Types.BIGINT, size -> createDataType("BigInt", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.BigInt.class)),
            Tuples.pair(Types.SMALLINT, size -> createDataType("SmallInt", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt.class)),
            Tuples.pair(Types.TINYINT, size -> createDataType("TinyInt", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt.class)),
            Tuples.pair(Types.TIMESTAMP, size -> createDataType("Timestamp", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair(Types.TIME_WITH_TIMEZONE, size -> createDataType("Timestamp", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair(Types.TIMESTAMP_WITH_TIMEZONE, size -> createDataType("Timestamp", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair(Types.CHAR, size -> createDataType("Char", size,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Char.class))
    );

    //must hardcode precision since SF does not currently store it for Tabular Functions
    private static final Map<String, DataType> STRING_TYPE_MAP = UnifiedMap.newMapWith(
            Tuples.pair("VARCHAR", createDataType("Varchar", 16777216L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar.class)),
            Tuples.pair("LONGNVARCHAR", createDataType("Varchar", 16777216L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar.class)),
            Tuples.pair("DATE", createDataType("Date", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Date.class)),
            Tuples.pair("FLOAT", createDataType("Float", 38L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Float.class)),
            Tuples.pair("DOUBLE", createDataType("Double", 38L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double.class)),
            Tuples.pair("NUMERIC", createDataType("Numeric", 38L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Numeric.class)),
            Tuples.pair("DECIMAL", createDataType("Decimal", 38L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Decimal.class)),
            Tuples.pair("BIT", createDataType("Bit", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit.class)),
            Tuples.pair("BOOLEAN", createDataType("Bit", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit.class)),
            Tuples.pair("INTEGER", createDataType("Integer", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer.class)),
            Tuples.pair("BIGINT", createDataType("BigInt", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.BigInt.class)),
            Tuples.pair("SMALLINT", createDataType("SmallInt", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt.class)),
            Tuples.pair("TINYINT", createDataType("TinyInt", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt.class)),
            Tuples.pair("TIMESTAMP", createDataType("Timestamp", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair("TIME_WITH_TIMEZONE", createDataType("Timestamp", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair("TIMESTAMP_WITH_TIMEZONE", createDataType("Timestamp", 0L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp.class)),
            Tuples.pair("CHAR", createDataType("Char", 1L,
                    org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Char.class))
    );

    private static final String[] TABLES_TYPES = new String[] {"TABLE", "VIEW", "BASE TABLE"};
    private static final String DEFAULT_SCHEMA = "default";

    SchemaExportation(ConnectionManagerSelector connectionManager)
    {
        this.connectionManager = connectionManager;
    }


    public static SchemaExportation newBuilder(ConnectionManagerSelector connectionManager)
    {
        return new SchemaExportation(connectionManager);
    }

    public Database build(DatabaseBuilderInput databaseBuilderInput, Identity identity) throws SQLException
    {
        List<Predicate<String>> tableNameFilters = FastList.newList();

        List<Function<String, String>> tableNameMappers = FastList.newList();
        List<Function<String, String>> schemaNameMappers = FastList.newList();
        List<Function<String, String>> columnNameMappers = FastList.newList();
        List<Function<String, String>> functionNameMappers = FastList.newList();

        try (Connection connection = connectionManager.getDatabaseConnection(identity, databaseBuilderInput.connection))
        {
            DatabaseMetaData metadata = connection.getMetaData();
            DatabaseBuilderConfig config = databaseBuilderInput.config;
            Database database = new Database();
            database._package = databaseBuilderInput.targetDatabase._package;
            database.name = databaseBuilderInput.targetDatabase.name;
            if (config.patterns == null || config.patterns.isEmpty())
            {
                return database;
            }
            this.preProcessInput(databaseBuilderInput);
            database.schemas = FastList.newList();

            for (DatabasePattern pattern : config.patterns)
            {
                buildDatabaseSchemas(databaseBuilderInput, database, metadata, pattern, tableNameFilters);
            }

            schemaNameMappers.add(SchemaExportation::escapeString);
            tableNameMappers.add(SchemaExportation::escapeString);
            columnNameMappers.add(SchemaExportation::escapeString);
            functionNameMappers.add(SchemaExportation::escapeString);

            //post process the names.
            database.schemas.forEach(schema ->
            {
                schema.name = applyNameMapper(schemaNameMappers, schema.name);
                schema.tables.forEach(table ->
                {
                    table.name = applyNameMapper(tableNameMappers, table.name);
                    table.columns.forEach(column ->
                    {
                        column.name = applyNameMapper(columnNameMappers, column.name);
                    });
                });
                schema.tabularFunctions.forEach(function -> function.name = applyNameMapper(tableNameMappers, function.name));
                schema.tabularFunctions.forEach(function ->
                {
                    function.name = applyNameMapper(functionNameMappers, function.name);
                    function.columns.forEach(funcColumn ->
                    {
                        funcColumn.name = applyNameMapper(columnNameMappers, funcColumn.name);
                    });
                });
            });

            return database;
        }
    }

    private String applyNameMapper(List<Function<String, String>> mappers, String name)
    {
        return mappers.stream().reduce(Function::andThen).orElse(Function.identity()).apply(name);
    }

    private void buildDatabaseSchemas(DatabaseBuilderInput databaseBuilderInput, Database db, DatabaseMetaData metadata, DatabasePattern pattern, List<Predicate<String>> tableNameFilters) throws SQLException
    {
        DatabaseBuilderConfig config = databaseBuilderInput.config;
        Map<String, List<CatalogFunction>> functionsBySchema = Maps.mutable.empty();
        // build schemas

        Map<String, List<CatalogTable>> tablesBySchema = this.buildSchemasAndCollectTables(db, metadata, pattern, tableNameFilters);

        if (config.enrichTableFunctions)
        {
            functionsBySchema = this.buildSchemasAndCollectFunctions(db, metadata, pattern, tableNameFilters);
        }

        if (config.enrichTables && config.maxTables != null && tablesBySchema.values().stream().mapToLong(List::size).sum() > config.maxTables)
        {
            throw new IllegalStateException(String.format("Maximum number of tables %d has been reached, " +
                    "please restrict the input you are generating for or set maxTables property. " +
                    "The current input has requested %d tables", config.maxTables, tablesBySchema.values().stream().mapToLong(List::size).sum()));
        }
        tablesBySchema.keySet().forEach(schema -> getOrCreateAndAddSchema(db, schema));
        functionsBySchema.keySet().forEach(schema -> getOrCreateAndAddSchema(db, schema));
      
        // build tables
        if (config.enrichTables)
        {

            for (Map.Entry<String, List<CatalogTable>> entry : tablesBySchema.entrySet())
            {
                String catalogTablesSchema = entry.getKey();
                List<CatalogTable> catalogTables = entry.getValue();
                Schema schema = getOrCreateAndAddSchema(db, catalogTablesSchema);
                for (CatalogTable t : catalogTables)
                {
                    buildSchemaTable(databaseBuilderInput, t.getCatalog(), schema, t.getTable(), metadata);
                }
            }
        }   

        // build functions
        if (config.enrichTableFunctions)
        {
            for (Map.Entry<String, List<CatalogFunction>> entry : functionsBySchema.entrySet())
            {
                String catalogFunctionSchema = entry.getKey();
                List<CatalogFunction> catalogFunctions = entry.getValue();
                Schema schema = getOrCreateAndAddSchema(db, catalogFunctionSchema);
                for (CatalogFunction f : catalogFunctions)
                {
                    buildSchemaFunction(databaseBuilderInput, f.getCatalog(), schema, f.getFunction(), metadata);
                }
            }
        }
    }

    private Map<String, List<CatalogTable>> buildSchemasAndCollectTables(Database db, DatabaseMetaData metadata, DatabasePattern pattern, List<Predicate<String>> tableNameFilters) throws SQLException
    {
        String escapeStringCharacter = metadata.getSearchStringEscape();

        String escapedCatalog = correctCasePattern(pattern.getCatalog(), metadata);

        String escapedSchema = correctCasePattern(
                escapePattern(pattern.getSchemaPattern(), pattern.isEscapeSchemaPattern(), escapeStringCharacter),
                metadata);

        String escapedTable = correctCasePattern(escapePattern(pattern.getTablePattern(), pattern.isEscapeTablePattern(), escapeStringCharacter), metadata);

        try (ResultSet tablesRs = metadata.getTables(escapedCatalog, escapedSchema, escapedTable, TABLES_TYPES))
        {
            MutableMap<String, List<CatalogTable>> tablesBySchema = Maps.mutable.empty();
            while (tablesRs.next())
            {
                String CATALOG_LABEL = "TABLE_CAT";
                String TABLE_LABEL = "TABLE_NAME";
                String SCHEMA_LABEL = "TABLE_SCHEM";
                String catalog = tablesRs.getString(CATALOG_LABEL);
                String table = tablesRs.getString(TABLE_LABEL);
                String schema = tablesRs.getString(SCHEMA_LABEL);
                schema = schema == null ? DEFAULT_SCHEMA : schema;
                if (ListIterate.anySatisfy(tableNameFilters, f -> f.test(table)))
                {
                    continue;
                }
                CatalogTable catalogTable = new CatalogTable(catalog, schema, table);
                tablesBySchema.getIfAbsentPut(schema, Lists.mutable.empty()).add(catalogTable);
            }
            return tablesBySchema;
        }
    }

    private Map<String, List<CatalogFunction>> buildSchemasAndCollectFunctions(Database db, DatabaseMetaData metadata, DatabasePattern pattern, List<Predicate<String>> functionNameFilters) throws SQLException
    {
        String escapeStringCharacter = metadata.getSearchStringEscape();

        String escapedCatalog = correctCasePattern(pattern.getCatalog(), metadata);

        String escapedSchema = correctCasePattern(
                escapePattern(pattern.getSchemaPattern(), pattern.isEscapeSchemaPattern(), escapeStringCharacter),
                metadata);
        
        String escapedTableFunction = correctCasePattern(
                escapePattern(pattern.getFunctionPattern(), pattern.isEscapeFunctionPattern(), escapeStringCharacter),
                metadata);

        try (ResultSet functionRs = metadata.getFunctions(escapedCatalog, escapedSchema, escapedTableFunction))
        {
            MutableMap<String, List<CatalogFunction>> functionsBySchema = Maps.mutable.empty();
            while (functionRs.next())
            {
                String CATALOG_LABEL = "FUNCTION_CAT";
                String FUNCTION_LABEL = "FUNCTION_NAME";
                String SCHEMA_LABEL = "FUNCTION_SCHEM";
                String FUNCTION_TYPE = "FUNCTION_TYPE";
                String catalog = functionRs.getString(CATALOG_LABEL);
                String function = functionRs.getString(FUNCTION_LABEL);
                String schema = functionRs.getString(SCHEMA_LABEL);
                int functionType = functionRs.getInt(FUNCTION_TYPE);
                schema = schema == null ? DEFAULT_SCHEMA : schema;
                if (ListIterate.anySatisfy(functionNameFilters, f -> f.test(function)))
                {
                    continue;
                }

                if (functionType == metadata.functionReturnsTable)
                {
                    CatalogFunction catalogFunction = new CatalogFunction(catalog, schema, function);
                    functionsBySchema.getIfAbsentPut(schema, Lists.mutable.empty()).add(catalogFunction);
                }
            }
            return functionsBySchema;
        }
    }

    private String escapePattern(String pattern, boolean doEscape, String escapeStringCharacter)
    {
        return pattern != null && doEscape && escapeStringCharacter != null
                ? pattern.replace("_", escapeStringCharacter + "_")
                .replace("%", escapeStringCharacter + "%")
                : pattern;
    }

    private String correctCasePattern(String pattern, DatabaseMetaData metaData) throws SQLException
    {
        if (pattern == null)
        {
            return null;
        }
        if (metaData.storesUpperCaseIdentifiers())
        {
            return pattern.toUpperCase();
        }
        if (metaData.storesLowerCaseIdentifiers())
        {
            return pattern.toLowerCase();
        }

        return pattern;
    }

    private void buildSchemaTable(DatabaseBuilderInput databaseBuilderInput, String catalog, Schema schema, String tableName, DatabaseMetaData metaData) throws SQLException
    {
        if (ListIterate.noneSatisfy(schema.tables, t -> t.name.equals(tableName)))
        {
            Table table = new Table();
            table.name = tableName;
            DatabaseBuilderConfig config = databaseBuilderInput.config;
            if (config.enrichColumns)
            {
                buildTableColumns(databaseBuilderInput, catalog, schema, table, metaData);
            }
            schema.tables.add(table);
        }
    }

    private void buildSchemaFunction(DatabaseBuilderInput databaseBuilderInput, String catalog, Schema schema, String functionName, DatabaseMetaData metaData) throws SQLException
    {
        if (ListIterate.noneSatisfy(schema.tabularFunctions, t -> t.name.equals(functionName)))
        {
            TabularFunction function = new TabularFunction();
            function.name = functionName;
            DatabaseBuilderConfig config = databaseBuilderInput.config;
            if (config.enrichColumns)
            {
                buildFunctionColumns(catalog, schema.name, function, metaData);
            }
            schema.tabularFunctions.add(function);
        }
    }

    private void buildTableColumns(DatabaseBuilderInput databaseBuilderInput, String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException
    {
        if (databaseBuilderInput.config.enrichPrimaryKeys)
        {
            table.primaryKey = buildPrimaryKeys(catalog, schema, table, metaData);
        }
        table.columns = buildColumns(catalog, schema, table, metaData);
    }

    private void buildFunctionColumns(String catalog, String schema, TabularFunction function, DatabaseMetaData metadata) throws SQLException
    {
        function.columns = functionColumns(catalog, schema, function, metadata);
    }

    private List<Column> functionColumns(String catalog, String schema, TabularFunction function, DatabaseMetaData metadata) throws SQLException
    {
        String searchStringEscape = metadata.getSearchStringEscape();
        String escapedSchemaName = escapePattern(schema, true, searchStringEscape);
        String escapedFunctionName = escapePattern(function.name, true, searchStringEscape);
        final Pattern getColumnInfo = Pattern.compile("\\((.*?)\\)");
        final Pattern columnInfo = Pattern.compile("^(.*)\\s(\\w+)$");
        List<Column> columns = FastList.newList();

        try (ResultSet functionRs = metadata.getFunctionColumns(catalog, escapedSchemaName, escapedFunctionName, "%"))
        {
            while (functionRs.next())
            {
                //must fetch column name and type using TYPE_NAME. Currently, COLUMN_NAME, COLUMN_TYPE, and PRECISION is returning null for SF UDTF.
                String functionType = functionRs.getString("TYPE_NAME");

                Matcher matchColumnsInfo = getColumnInfo.matcher(functionType);
                if (matchColumnsInfo.find())
                {
                    String columnNameAndType = matchColumnsInfo.group(1);
                    String[] allColumns = columnNameAndType.split(", ");
                    Arrays.stream(allColumns).forEach(c ->
                    {
                        try
                        {
                            Matcher matchColumnNameAndType = columnInfo.matcher(c);
                            if (matchColumnNameAndType.find())
                            {
                                Column column = new Column();
                                column.name = matchColumnNameAndType.group(1);
                                column.nullable = "YES".equals(functionRs.getString("IS_NULLABLE"));
                                column.type = buildFunctionDataTypeNode(matchColumnNameAndType.group(2));
                                columns.add(column);
                            } 
                        }
                        catch (Exception e)
                        {
                             new Exception(e);
                        }
                        
                    });
                }
            }
        }
        return columns;
    }
    
    private List<Column> buildColumns(String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException
    {
        String searchStringEscape = metaData.getSearchStringEscape();

        String escapedSchemaName = escapePattern(schema.name, true, searchStringEscape);
        String escapedTableName = escapePattern(table.name, true, searchStringEscape);

        try (ResultSet columnsRs = metaData.getColumns(catalog, escapedSchemaName, escapedTableName, "%"))
        {
            List<Column> columns = FastList.newList();
            while (columnsRs.next())
            {
                Column column = new Column();
                column.name = columnsRs.getString("COLUMN_NAME");
                column.nullable = "YES".equals(columnsRs.getString("IS_NULLABLE"));
                column.type = buildDataTypeNode(columnsRs);
                columns.add(column);
            }
            return columns;
        }
    }

    private List<String> buildPrimaryKeys(String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException
    {
        String searchStringEscape = metaData.getSearchStringEscape();

        String escapedSchemaName = escapePattern(schema.name, true, searchStringEscape);

        try (ResultSet primaryKeysRs = metaData.getPrimaryKeys(catalog, escapedSchemaName, table.name))
        {
            List<String> primaryKeys = FastList.newList();
            while (primaryKeysRs.next())
            {
                primaryKeys.add(primaryKeysRs.getString("COLUMN_NAME"));
            }
            return primaryKeys;
        }
    }

    private Schema getOrCreateAndAddSchema(Database db, String name)
    {
        Schema schema = ListIterate.select(db.schemas, s -> s.name.equals(name)).getFirst();
        if (schema == null)
        {
            schema = new Schema();
            schema.name = name;
            schema.tables = FastList.newList();
            schema.tabularFunctions = FastList.newList();
            db.schemas.add(schema);
        }
        return schema;
    }

    private DataType buildDataTypeNode(ResultSet columns) throws SQLException
    {
        int type = columns.getInt("DATA_TYPE");
        long size = columns.getInt("COLUMN_SIZE");

        Function<Long, DataType> func = TYPE_MAP.get(type);

        if (func == null)
        {
            Other o = new Other();
            return o;
        }

        return func.apply(size);
    }

    private DataType buildFunctionDataTypeNode(String columnType)
    {
        DataType type = STRING_TYPE_MAP.get(columnType);
        if (type == null)
        {
            return new Other(); 
        }
        return type;
    }

    private static DataType createDataType(String type, Long size, Class<? extends DataType> clazz)
    {
        ObjectNode node = JsonNodeFactory.instance.objectNode().put("_type", type);
        if (size != 0L)
        {
            node.put("size", size);
        }
        try
        {
            return objectMapper.treeToValue(node, clazz);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException("Error converting datatype", e);
        }
    }

    private void preProcessInput(DatabaseBuilderInput storeInput)
    {
        /*
        // TODO - is this a feature flag to allow this feature only for Snowflake ??
        if (storeInput.connection.datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) storeInput.connection.datasourceSpecification;
            storeInput.config.setPatterns(ListIterate.collect(storeInput.config.getPatterns(), p -> p.withNewCatalog(snowflakeDatasourceSpecification.databaseName)));
        }
         */
    }

    public static String escapeString(String s)
    {
        return StringUtils.containsAny(s, ESCAPE_CHARS) ? "\"" + s + "\"" : s;
    }

    private class CatalogTable
    {
        private final String catalog;
        private final String schema;
        private final String table;

        public CatalogTable(String catalog, String schema, String table)
        {
            this.catalog = catalog;
            this.schema = schema;
            this.table = table;
        }

        public String getCatalog()
        {
            return catalog;
        }

        public String getSchema()
        {
            return schema;
        }

        public String getTable()
        {
            return table;
        }
    }

    private class CatalogFunction
    {
        private final String catalog;
        private final String schema;
        private final String function;

        public CatalogFunction(String catalog, String schema, String function)
        {
            this.catalog = catalog;
            this.schema = schema;
            this.function = function;
        }

        public String getCatalog()
        {
            return catalog;
        }

        public String getSchema()
        {
            return schema;
        }

        public String getFunction()
        {
            return function;
        }
    }


}
