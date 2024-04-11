// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.jdbc;

import org.finos.legend.engine.persistence.components.executor.TypeMapping;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Index;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.UniqueColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class JdbcHelper implements RelationalExecutionHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcHelper.class);

    private final Connection connection;
    private JdbcTransactionManager transactionManager;

    public static final String TYPE_NAME = "TYPE_NAME";
    public static final String DATA_TYPE = "DATA_TYPE";
    public static final String COLUMN_SIZE = "COLUMN_SIZE";
    public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
    public static final String IS_NULLABLE = "IS_NULLABLE";
    public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
    public static final String BOOL_TRUE_STRING_VALUE = "YES";
    public static final String INDEX_NAME = "INDEX_NAME";
    public static final String NON_UNIQUE = "NON_UNIQUE";

    public static JdbcHelper of(Connection connection)
    {
        return new JdbcHelper(connection);
    }

    private JdbcHelper(Connection connection)
    {
        this.connection = connection;
    }

    public Connection connection()
    {
        return connection;
    }

    @Override
    public void beginTransaction()
    {
        try
        {
            this.transactionManager = new JdbcTransactionManager(connection);
            this.transactionManager.beginTransaction();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commitTransaction()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.commitTransaction();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void revertTransaction()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.revertTransaction();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void closeTransactionManager()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                this.transactionManager = null;
            }
        }
    }

    @Override
    public boolean doesTableExist(Dataset dataset)
    {
        try
        {
            String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
            String database = dataset.datasetReference().database().orElse(null);
            String schema = dataset.datasetReference().group().orElse(null);
            ResultSet result = this.connection.getMetaData().getTables(database, schema, name, new String[] {Clause.TABLE.get()});
            return result.isBeforeFirst(); // This method returns true if ResultSet is not empty
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateDatasetSchema(Dataset dataset, TypeMapping typeMapping)
    {
        try
        {
            if (!(typeMapping instanceof  DataTypeMapping))
            {
                throw new IllegalStateException("Only DataTypeMapping allowed in validateDatasetSchema");
            }
            DataTypeMapping datatypeMapping = (DataTypeMapping) typeMapping;
            String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
            String database = dataset.datasetReference().database().orElse(null);
            String schema = dataset.datasetReference().group().orElse(null);

            DatabaseMetaData dbMetaData = this.connection.getMetaData();
            ResultSet result = dbMetaData.getColumns(database, schema, name, null);
            List<Field> userFields = new ArrayList<>(dataset.schema().fields());
            List<Column> userColumns = convertUserProvidedFieldsToColumns(userFields, datatypeMapping);
            List<Column> dbColumns = new ArrayList<>();

            // Get primary keys
            Set<String> primaryKeys = new HashSet<>();
            ResultSet primaryKeyResult = dbMetaData.getPrimaryKeys(database, schema, name);
            while (primaryKeyResult.next())
            {
                primaryKeys.add(primaryKeyResult.getString(RelationalExecutionHelper.COLUMN_NAME));
            }

            // Get unique keys
            Set<String> uniqueKeys = new HashSet<>();
            ResultSet uniqueKeyResult = dbMetaData.getIndexInfo(database, schema, name, true, false);
            while (uniqueKeyResult.next())
            {
                uniqueKeys.add(uniqueKeyResult.getString(RelationalExecutionHelper.COLUMN_NAME));
            }

            while (result.next())
            {
                String columnName = result.getString(RelationalExecutionHelper.COLUMN_NAME);

                // Get the datatype
                String typeName = result.getString(TYPE_NAME);
                int columnSize = result.getInt(COLUMN_SIZE);
                int scale = result.getInt(DECIMAL_DIGITS);

                org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType matchedDataType = null;
                for (org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType dataType : org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.values())
                {
                    if (dataType.name().equalsIgnoreCase(typeName))
                    {
                        matchedDataType = dataType;
                        break;
                    }
                }
                if (matchedDataType == null)
                {
                    matchedDataType = org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.valueOf(JDBCType.valueOf(result.getInt("DATA_TYPE")).name());
                }

                FieldType fieldType = FieldType.of(matchedDataType, columnSize, scale);
                DataType physicalDataType = datatypeMapping.getDataType(fieldType);

                // Check the constraints
                List<ColumnConstraint> columnConstraints = new ArrayList<>();
                boolean nullable = result.getString(IS_NULLABLE).equals(BOOL_TRUE_STRING_VALUE);
                if (!nullable)
                {
                    columnConstraints.add(new NotNullColumnConstraint());
                }

                boolean primaryKey = primaryKeys.contains(columnName);
                if (primaryKey)
                {
                    columnConstraints.add(new PKColumnConstraint());
                }

                boolean unique = uniqueKeys.contains(columnName);
                if (unique)
                {
                    columnConstraints.add(new UniqueColumnConstraint());
                }

                Column column = new Column(columnName, physicalDataType, columnConstraints, null);
                dbColumns.add(column);
            }

            // Compare the schemas
            validateColumns(userColumns, dbColumns);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dataset constructDatasetFromDatabase(Dataset dataset, TypeMapping typeMapping)
    {
        String tableName = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schemaName = dataset.datasetReference().group().orElse(null);
        String databaseName = dataset.datasetReference().database().orElse(null);
        try
        {
            if (!(typeMapping instanceof JdbcPropertiesToLogicalDataTypeMapping))
            {
                throw new IllegalStateException("Only JdbcPropertiesToLogicalDataTypeMapping allowed in constructDatasetFromDatabase");
            }
            JdbcPropertiesToLogicalDataTypeMapping mapping = (JdbcPropertiesToLogicalDataTypeMapping) typeMapping;
            DatabaseMetaData dbMetaData = this.connection.getMetaData();

            // Get primary keys
            Set<String> primaryKeys = new HashSet<>();
            ResultSet primaryKeyResult = dbMetaData.getPrimaryKeys(databaseName, schemaName, tableName);
            while (primaryKeyResult.next())
            {
                primaryKeys.add(primaryKeyResult.getString(RelationalExecutionHelper.COLUMN_NAME));
            }

            // Get all unique constraints and indices
            Set<String> uniqueKeys = new HashSet<>();
            Map<String, List<String>> indexMap = new HashMap<>();
            Map<String, Boolean> indexNonUniqueMap = new HashMap<>();
            ResultSet indexResult = dbMetaData.getIndexInfo(databaseName, schemaName, tableName, false, false);
            while (indexResult.next())
            {
                String indexName = indexResult.getString(INDEX_NAME);
                String columnName = indexResult.getString(RelationalExecutionHelper.COLUMN_NAME);
                boolean isIndexNonUnique = indexResult.getBoolean(NON_UNIQUE);

                if (!indexName.matches(Pattern.compile("PRIMARY_KEY_[a-zA-Z0-9]+").pattern()))
                {
                    if (indexName.matches(Pattern.compile("CONSTRAINT_INDEX_[a-zA-Z0-9]+").pattern()) && !isIndexNonUnique)
                    {
                        // Unique constraint index
                        uniqueKeys.add(columnName);
                    }
                    else
                    {
                        // Custom index
                        if (!indexMap.containsKey(indexName))
                        {
                            indexMap.put(indexName, new ArrayList<>());
                            indexNonUniqueMap.put(indexName, isIndexNonUnique);
                        }
                        indexMap.get(indexName).add(columnName);
                    }
                }
            }
            List<Index> indices = new ArrayList<>();
            for (String indexName : indexMap.keySet())
            {
                Index index = Index.builder()
                        .indexName(indexName)
                        .addAllColumns(indexMap.get(indexName))
                        .unique(!indexNonUniqueMap.get(indexName))
                        .build();
                indices.add(index);
            }

            // Get all columns
            List<Field> fields = new ArrayList<>();
            ResultSet columnResult = dbMetaData.getColumns(databaseName, schemaName, tableName, null);
            while (columnResult.next())
            {
                String columnName = columnResult.getString(RelationalExecutionHelper.COLUMN_NAME);
                String typeName = columnResult.getString(TYPE_NAME);
                String dataType = JDBCType.valueOf(columnResult.getInt(DATA_TYPE)).getName();
                int columnSize = columnResult.getInt(COLUMN_SIZE);
                int decimalDigits = columnResult.getInt(DECIMAL_DIGITS);
                String isNullable = columnResult.getString(IS_NULLABLE);
                String isIdentity = columnResult.getString(IS_AUTOINCREMENT);

                // Construct type
                FieldType fieldType = mapping.getDataType(typeName.toUpperCase(), dataType.toUpperCase(), columnSize, decimalDigits);

                // Construct constraints
                boolean nullable = isNullable.equals(BOOL_TRUE_STRING_VALUE);
                boolean identity = isIdentity.equals(BOOL_TRUE_STRING_VALUE);
                boolean primaryKey = primaryKeys.contains(columnName);
                boolean unique = uniqueKeys.contains(columnName);

                Field field = Field.builder().name(columnName).type(fieldType).nullable(nullable).identity(identity).primaryKey(primaryKey).unique(unique).build();

                fields.add(field);
            }

            SchemaDefinition schemaDefinition = SchemaDefinition.builder().addAllFields(fields).addAllIndexes(indices).build();
            return DatasetDefinition.builder().name(tableName).database(databaseName).group(schemaName).schema(schemaDefinition).datasetAdditionalProperties(dataset.datasetAdditionalProperties()).build();
        }
        catch (SQLException e)
        {
            LOGGER.error("Exception in Constructing dataset Schema from Database", e);
            throw new RuntimeException(e);
        }
    }

    public static void validateColumns(List<Column> userColumns, List<Column> dbColumns)
    {
        if (userColumns.size() != dbColumns.size())
        {
            throw new IllegalStateException("Number of columns in user-provided schema doesn't match with the schema in the database");
        }
        for (Column userColumn : userColumns)
        {
            Column matchedColumn = dbColumns.stream().filter(dbColumn -> dbColumn.getColumnName().equals(userColumn.getColumnName())).findFirst().orElseThrow(() -> new IllegalStateException("Column in user-provided schema doesn't match any column in the schema in the database"));
            if (!userColumn.equals(matchedColumn))
            {
                throw new IllegalStateException("Column in user-provided schema doesn't match the corresponding column in the schema in the database");
            }
        }
    }

    public static List<Column> convertUserProvidedFieldsToColumns(List<Field> userFields, DataTypeMapping datatypeMapping)
    {
        List<Column> columnList = new ArrayList<>();

        for (Field f : userFields)
        {
            DataType dataType = datatypeMapping.getDataType(f.type());
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (!f.nullable() || f.primaryKey())
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }
            if (f.primaryKey())
            {
                columnConstraints.add(new PKColumnConstraint());
            }
            if (f.unique() || f.primaryKey())
            {
                columnConstraints.add(new UniqueColumnConstraint());
            }
            Column column = new Column(f.name(), dataType, columnConstraints, null);
            columnList.add(column);
        }

        return columnList;
    }


    @Override
    public void executeStatement(String sql)
    {
        List<String> sqls = Collections.singletonList(sql);
        executeStatements(sqls);
    }

    @Override
    public void executeStatements(List<String> sqls)
    {
        if (this.transactionManager != null)
        {
            try
            {
                for (String sql : sqls)
                {
                    this.transactionManager.executeInCurrentTransaction(sql);
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            JdbcTransactionManager txManager = null;
            try
            {
                txManager = new JdbcTransactionManager(connection);
                txManager.beginTransaction();
                for (String sql : sqls)
                {
                    txManager.executeInCurrentTransaction(sql);
                }
                txManager.commitTransaction();
            }
            catch (Exception e)
            {
                LOGGER.error("Error executing SQL statements: " + sqls, e);
                if (txManager != null)
                {
                    try
                    {
                        txManager.revertTransaction();
                    }
                    catch (SQLException e2)
                    {
                        throw new RuntimeException(e2);
                    }
                }
                throw new RuntimeException(e);
            }
            finally
            {
                if (txManager != null)
                {
                    try
                    {
                        txManager.close();
                    }
                    catch (SQLException e)
                    {
                        LOGGER.error("Error closing transaction manager.", e);
                    }
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, int rows)
    {
        if (this.transactionManager != null)
        {
            return this.transactionManager.convertResultSetToList(sql, rows);
        }
        else
        {
            JdbcTransactionManager txManager = null;
            try
            {
                txManager = new JdbcTransactionManager(connection);
                return txManager.convertResultSetToList(sql, rows);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error executing SQL query: " + sql, e);
            }
            finally
            {
                if (txManager != null)
                {
                    try
                    {
                        txManager.close();
                    }
                    catch (SQLException e)
                    {
                        LOGGER.error("Error closing transaction manager.", e);
                    }
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql)
    {
        if (this.transactionManager != null)
        {
            return this.transactionManager.convertResultSetToList(sql);
        }
        else
        {
            JdbcTransactionManager txManager = null;
            try
            {
                txManager = new JdbcTransactionManager(connection);
                return txManager.convertResultSetToList(sql);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error executing SQL query: " + sql, e);
            }
            finally
            {
                if (txManager != null)
                {
                    try
                    {
                        txManager.close();
                    }
                    catch (SQLException e)
                    {
                        LOGGER.error("Error closing transaction manager.", e);
                    }
                }
            }
        }
    }

    @Override
    public void close()
    {
        try
        {
            this.connection.close();
        }
        catch (SQLException exception)
        {
            LOGGER.error("Error closing connection", exception);
        }
    }
}
