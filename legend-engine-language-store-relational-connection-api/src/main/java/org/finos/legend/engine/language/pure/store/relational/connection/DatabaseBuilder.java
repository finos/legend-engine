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

package org.finos.legend.engine.language.pure.store.relational.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.store.relational.connection.model.DatabaseBuilderConfig;
import org.finos.legend.engine.language.pure.store.relational.connection.model.DatabaseBuilderInput;
import org.finos.legend.engine.language.pure.store.relational.connection.model.StorePattern;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.DataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Other;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.pac4j.core.profile.CommonProfile;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class DatabaseBuilder {

    private DatabaseBuilderInput databaseBuilderInput;
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
    private static final String[] TABLES_TYPES = new String[]{"TABLE", "VIEW"};
    private static final String DEFAULT_SCHEMA = "default";


    DatabaseBuilder(DatabaseBuilderInput storeBuilderInput) {
        this.databaseBuilderInput = storeBuilderInput;
    }


    public static DatabaseBuilder newBuilder(DatabaseBuilderInput storeBuilderInput) {
        return new DatabaseBuilder(storeBuilderInput);
    }

    public Database build(ConnectionManagerSelector connectionManager, MutableList<CommonProfile> profiles) throws SQLException {
        List<Predicate<String>> tableNameFilters = FastList.newList();

        List<Function<String, String>> tableNameMappers = FastList.newList();
        List<Function<String, String>> schemaNameMappers = FastList.newList();
        List<Function<String, String>> columnNameMappers = FastList.newList();

        try (Connection connection = connectionManager.getDatabaseConnection(profiles, databaseBuilderInput.connection)) {
            DatabaseMetaData metadata = connection.getMetaData();
            DatabaseBuilderConfig config = this.databaseBuilderInput.config;
            Database database = new Database();
            database._package = this.databaseBuilderInput.targetDatabase._package;
            database.name = this.databaseBuilderInput.targetDatabase.name;
            if (config.patterns == null || config.patterns.isEmpty()) {
                config.setPatterns(FastList.newListWith(new StorePattern(null, null)));
            }
            this.preProcessInput(this.databaseBuilderInput);
            database.schemas = FastList.newList();

            for (StorePattern pattern : config.patterns) {
                buildDatabaseSchemas(database, metadata, pattern, tableNameFilters);
            }

            schemaNameMappers.add(DatabaseBuilder::escapeString);
            tableNameMappers.add(DatabaseBuilder::escapeString);
            columnNameMappers.add(DatabaseBuilder::escapeString);

            //post process the names.
            database.schemas.forEach(schema -> {
                schema.name = applyNameMapper(schemaNameMappers, schema.name);
                schema.tables.forEach(table -> {
                    table.name = applyNameMapper(tableNameMappers, table.name);
                    table.columns.forEach(column -> {
                        column.name = applyNameMapper(columnNameMappers, column.name);
                    });
                });
            });

            return database;
        }
    }

    private String applyNameMapper(List<Function<String, String>> mappers, String name) {
        return mappers.stream().reduce(Function::andThen).orElse(Function.identity()).apply(name);
    }

    private void buildDatabaseSchemas(Database db, DatabaseMetaData metadata, StorePattern pattern, List<Predicate<String>> tableNameFilters) throws SQLException {
        DatabaseBuilderConfig config = this.databaseBuilderInput.config;
        // build schemas

        Map<String, List<CatalogTable>> tablesBySchema = this.buildSchemasAndCollectTables(db, metadata, pattern, tableNameFilters);
        if (config.enrichTables && config.maxTables != null && tablesBySchema.values().stream().mapToLong(List::size).sum() > config.maxTables) {
            throw new IllegalStateException(String.format("Maximum number of tables %d has been reached, " +
                    "please restrict the input you are generating for or set maxTables property. " +
                    "The current input has requested %d tables", config.maxTables, tablesBySchema.values().stream().mapToLong(List::size).sum()));
        }
        tablesBySchema.keySet().forEach(schema -> getOrCreateAndAddSchema(db, schema));
        // build tables
        if (config.enrichTables) {

            for (Map.Entry<String, List<CatalogTable>> entry : tablesBySchema.entrySet()) {
                String catalogTablesSchema = entry.getKey();
                List<CatalogTable> catalogTables = entry.getValue();
                Schema schema = getOrCreateAndAddSchema(db, catalogTablesSchema);
                for (CatalogTable t : catalogTables) {
                    buildSchemaTable(t.getCatalog(), schema, t.getTable(), metadata);
                }
            }
        }
    }

    private Map<String, List<CatalogTable>> buildSchemasAndCollectTables(Database db, DatabaseMetaData metadata, StorePattern pattern, List<Predicate<String>> tableNameFilters) throws SQLException {
        String escapeStringCharacter = metadata.getSearchStringEscape();

        String escapedCatalog = correctCasePattern(pattern.getCatalog(), metadata);

        String escapedSchema = correctCasePattern(
                escapePattern(pattern.getSchemaPattern(), pattern.isEscapeSchemaPattern(), escapeStringCharacter),
                metadata);

        String escapedTable = correctCasePattern(escapePattern(pattern.getTablePattern(), pattern.isEscapeTablePattern(), escapeStringCharacter), metadata);

        try (ResultSet tablesRs = metadata.getTables(escapedCatalog, escapedSchema, escapedTable, TABLES_TYPES)) {
            MutableMap<String, List<CatalogTable>> tablesBySchema = Maps.mutable.empty();
            while (tablesRs.next()) {
                String CATALOG_LABEL = "TABLE_CAT";
                String TABLE_LABEL = "TABLE_NAME";
                String SCHEMA_LABEL = "TABLE_SCHEM";
                String catalog = tablesRs.getString(CATALOG_LABEL);
                String table = tablesRs.getString(TABLE_LABEL);
                String schema = tablesRs.getString(SCHEMA_LABEL);
                schema = schema == null ? DEFAULT_SCHEMA : schema;
                if (ListIterate.anySatisfy(tableNameFilters, f -> f.test(table))) {
                    continue;
                }
                CatalogTable catalogTable = new CatalogTable(catalog, schema, table);
                tablesBySchema.getIfAbsentPut(schema, Lists.mutable.empty()).add(catalogTable);
            }
            return tablesBySchema;
        }
    }

    private String escapePattern(String pattern, boolean doEscape, String escapeStringCharacter) {
        return pattern != null && doEscape
                ? pattern.replace("_", escapeStringCharacter + "_")
                .replace("%", escapeStringCharacter + "%")
                : pattern;
    }

    private String correctCasePattern(String pattern, DatabaseMetaData metaData) throws SQLException {
        if (pattern == null) {
            return null;
        }
        if (metaData.storesUpperCaseIdentifiers()) {
            return pattern.toUpperCase();
        }
        if (metaData.storesLowerCaseIdentifiers()) {
            return pattern.toLowerCase();
        }

        return pattern;
    }

    private void buildSchemaTable(String catalog, Schema schema, String tableName, DatabaseMetaData metaData) throws SQLException {
        if (ListIterate.noneSatisfy(schema.tables, t -> t.name.equals(tableName))) {
            Table table = new Table();
            table.name = tableName;
            DatabaseBuilderConfig config = this.databaseBuilderInput.config;
            if (config.enrichColumns) {
                buildTableColumns(catalog, schema, table, metaData);
            }
            schema.tables.add(table);
        }
    }

    private void buildTableColumns(String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException {
        if (this.databaseBuilderInput.config.enrichPrimaryKeys) {
            table.primaryKey = buildPrimaryKeys(catalog, schema, table, metaData);
        }
        table.columns = buildColumns(catalog, schema, table, metaData);
    }

    private List<Column> buildColumns(String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException {
        String searchStringEscape = metaData.getSearchStringEscape();

        String escapedSchemaName = escapePattern(schema.name, true, searchStringEscape);
        String escapedTableName = escapePattern(table.name, true, searchStringEscape);

        try (ResultSet columnsRs = metaData.getColumns(catalog, escapedSchemaName, escapedTableName, "%")) {
            List<Column> columns = FastList.newList();
            while (columnsRs.next()) {
                Column column = new Column();
                column.name = columnsRs.getString("COLUMN_NAME");
                column.nullable = "YES".equals(columnsRs.getString("IS_NULLABLE"));
                column.type = buildDataTypeNode(columnsRs);
                columns.add(column);
            }
            return columns;
        }
    }

    private List<String> buildPrimaryKeys(String catalog, Schema schema, Table table, DatabaseMetaData metaData) throws SQLException {
        String searchStringEscape = metaData.getSearchStringEscape();

        String escapedSchemaName = escapePattern(schema.name, true, searchStringEscape);

        try (ResultSet primaryKeysRs = metaData.getPrimaryKeys(catalog, escapedSchemaName, table.name)) {
            List<String> primaryKeys = FastList.newList();
            while (primaryKeysRs.next()) {
                primaryKeys.add(primaryKeysRs.getString("COLUMN_NAME"));
            }
            return primaryKeys;
        }
    }

    private Schema getOrCreateAndAddSchema(Database db, String name) {
        Schema schema = ListIterate.select(db.schemas, s -> s.name.equals(name)).getFirst();
        if (schema == null) {
            schema = new Schema();
            schema.name = name;
            schema.tables = FastList.newList();
            db.schemas.add(schema);
        }
        return schema;
    }

    private DataType buildDataTypeNode(ResultSet columns) throws SQLException {
        int type = columns.getInt("DATA_TYPE");
        long size = columns.getInt("COLUMN_SIZE");

        Function<Long, DataType> func = TYPE_MAP.get(type);

        if (func == null) {
            Other o = new Other();
            return o;
        }

        return func.apply(size);
    }

    private static DataType createDataType(String type, Long size, Class<? extends DataType> clazz) {
        ObjectNode node = JsonNodeFactory.instance.objectNode().put("_type", type);
        if (size != 0L) {
            node.put("size", size);
        }
        try {
            return objectMapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting datatype", e);
        }
    }

    private void preProcessInput(DatabaseBuilderInput storeInput) {
        if (storeInput.connection.datasourceSpecification instanceof SnowflakeDatasourceSpecification) {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) storeInput.connection.datasourceSpecification;
            storeInput.config.setPatterns(ListIterate.collect(storeInput.config.getPatterns(), p -> p.withNewCatalog(snowflakeDatasourceSpecification.databaseName)));
        }
    }

    public static String escapeString(String s) {
        return StringUtils.containsAny(s, ESCAPE_CHARS) ? "\"" + s + "\"" : s;
    }

    private class CatalogTable {
        private final String catalog;
        private final String schema;
        private final String table;

        public CatalogTable(String catalog, String schema, String table) {
            this.catalog = catalog;
            this.schema = schema;
            this.table = table;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }

        public String getTable() {
            return table;
        }
    }




}
