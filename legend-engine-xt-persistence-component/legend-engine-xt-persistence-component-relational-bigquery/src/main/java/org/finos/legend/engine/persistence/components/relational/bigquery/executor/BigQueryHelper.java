// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.bigquery.executor;

import com.google.cloud.bigquery.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BigQueryHelper implements RelationalExecutionHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryHelper.class);

    private final BigQuery bigQuery;
    private BigQueryTransactionManager transactionManager;

    public static BigQueryHelper of(BigQuery bigQuery)
    {
        return new BigQueryHelper(bigQuery);
    }

    private BigQueryHelper(BigQuery bigQuery)
    {
        this.bigQuery = bigQuery;
    }

    public void beginTransaction()
    {
        try
        {
            this.transactionManager = new BigQueryTransactionManager(bigQuery);
            this.transactionManager.beginTransaction();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void commitTransaction()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.commitTransaction();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void revertTransaction()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.revertTransaction();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void closeTransactionManager()
    {
        if (this.transactionManager != null)
        {
            try
            {
                this.transactionManager.close();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                this.transactionManager = null;
            }
        }
    }

    public boolean doesTableExist(Dataset dataset)
    {
        String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schema = dataset.datasetReference().group().orElse(null);
        Table table = this.bigQuery.getTable(TableId.of(schema, name));
        return table.exists();
    }

    public void validateDatasetSchema(Dataset dataset, DataTypeMapping datatypeMapping)
    {
        // TODO # 10: Fetch and validate primary keys, unique keys and indices
        String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schema = dataset.datasetReference().group().orElse(null);

        Table table = this.bigQuery.getTable(TableId.of(schema, name));
        FieldList dbFields = table.getDefinition().getSchema().getFields();
        List<Field> userFields = new ArrayList<>(dataset.schema().fields());
        List<Column> userColumns = convertUserProvidedFieldsToColumns(userFields, datatypeMapping);
        List<Column> dbColumns = new ArrayList<>();

        for (com.google.cloud.bigquery.Field dbField : dbFields)
        {
            String columnName = dbField.getName();

            // Get the datatype
            String typeName = dbField.getType().getStandardType().name();
            Integer columnSize = Objects.nonNull(dbField.getMaxLength()) ? Integer.valueOf(dbField.getMaxLength().intValue()) : Objects.nonNull(dbField.getPrecision()) ? Integer.valueOf(dbField.getPrecision().intValue()) : null;
            Integer scale = Objects.nonNull(dbField.getScale()) ? Integer.valueOf(dbField.getScale().intValue()) : null;

            org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType matchedDataType = org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.valueOf(typeName.toUpperCase());

            FieldType fieldType = FieldType.of(matchedDataType, columnSize, scale);
            DataType physicalDataType = datatypeMapping.getDataType(fieldType);

            // Check the constraints
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (com.google.cloud.bigquery.Field.Mode.REQUIRED.equals(dbField.getMode()))
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }

            Column column = new Column(columnName, physicalDataType, columnConstraints, null);
            dbColumns.add(column);
        }

        // Compare the schemas
        validateColumns(userColumns, dbColumns);
    }

    public Dataset constructDatasetFromDatabase(String tableName, String schemaName, String databaseName, JdbcPropertiesToLogicalDataTypeMapping mapping)
    {
        // TODO # 9: Fetch and construct primary keys, unique keys and indices
        Table table = this.bigQuery.getTable(TableId.of(schemaName, tableName));

        // Get all columns
        List<Field> fields = new ArrayList<>();
        for (com.google.cloud.bigquery.Field dbField : table.getDefinition().getSchema().getFields())
        {
            String columnName = dbField.getName();
            String typeName = dbField.getType().getStandardType().name();
            String dataType = dbField.getType().name();
            Integer columnSize = Objects.nonNull(dbField.getMaxLength()) ? Integer.valueOf(dbField.getMaxLength().intValue()) : Objects.nonNull(dbField.getPrecision()) ? Integer.valueOf(dbField.getPrecision().intValue()) : null;
            Integer decimalDigits = Objects.nonNull(dbField.getScale()) ? Integer.valueOf(dbField.getScale().intValue()) : null;

            // Construct type
            FieldType fieldType = mapping.getDataType(typeName.toUpperCase(), dataType.toUpperCase(), columnSize, decimalDigits);

            // Construct constraints
            boolean nullable = !com.google.cloud.bigquery.Field.Mode.REQUIRED.equals(dbField.getMode());

            Field field = Field.builder()
                    .name(columnName)
                    .type(fieldType)
                    .nullable(nullable)
                    .build();

            fields.add(field);
        }

        SchemaDefinition schemaDefinition = SchemaDefinition.builder()
                .addAllFields(fields)
                .build();
        return DatasetDefinition.builder().name(tableName).database(databaseName).group(schemaName).schema(schemaDefinition).build();
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
        // TODO # 11: Handle primary keys, unique keys and indices
        List<Column> columnList = new ArrayList<>();

        for (Field f : userFields)
        {
            DataType dataType = datatypeMapping.getDataType(f.type());
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (!f.nullable() || f.primaryKey())
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }
            Column column = new Column(f.name(), dataType, columnConstraints, null);
            columnList.add(column);
        }

        return columnList;
    }


    public void executeStatement(String sql)
    {
        List<String> sqls = Collections.singletonList(sql);
        executeStatements(sqls);
    }

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
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            BigQueryTransactionManager txManager = null;
            try
            {
                txManager = new BigQueryTransactionManager(bigQuery);
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
                    catch (InterruptedException e2)
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
                    catch (InterruptedException e)
                    {
                        LOGGER.error("Error closing transaction manager.", e);
                    }
                }
            }
        }
    }

    public List<Map<String, Object>> executeQuery(String sql)
    {
        if (this.transactionManager != null)
        {
            return this.transactionManager.convertResultSetToList(sql);
        }
        else
        {
            BigQueryTransactionManager txManager = null;
            try
            {
                txManager = new BigQueryTransactionManager(bigQuery);
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
                    catch (InterruptedException e)
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
    }
}
