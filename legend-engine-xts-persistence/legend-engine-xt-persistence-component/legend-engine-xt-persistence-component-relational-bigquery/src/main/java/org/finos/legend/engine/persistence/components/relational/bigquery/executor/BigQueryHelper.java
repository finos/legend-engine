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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.TableId;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.TypeMapping;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.constraints.columns.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BigQueryHelper implements RelationalExecutionHelper
{
    public static final String PRIMARY_KEY_INFO_TABLE_NAME = "INFORMATION_SCHEMA.KEY_COLUMN_USAGE";
    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryHelper.class);
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String TABLE_SCHEMA = "TABLE_SCHEMA";
    private static final String CONSTRAINT_NAME = "CONSTRAINT_NAME";
    private static final String CONSTRAINT_NAME_QUANTIFIER_PK = ".pk$";
    private static final Function<String, String> CONSTRAINT_NAME_PROVIDER_PK = tableName -> tableName + CONSTRAINT_NAME_QUANTIFIER_PK;

    private final BigQuery bigQuery;
    private BigQueryTransactionManager transactionManager;

    public static BigQueryHelper of(BigQuery bigQuery)
    {
        if (bigQuery != null)
        {
            return new BigQueryHelper(bigQuery);
        }
        throw new IllegalStateException("Sink initialized without connection can only be used for SQL generation APIs, but used with ingestion API");
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
        String projectName = dataset.datasetReference().database().orElse(null);
        String datasetName = dataset.datasetReference().group().orElseThrow(IllegalStateException::new);
        String tableName = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);

        TableId tableId = projectName == null ?
                TableId.of(datasetName, tableName) :
                TableId.of(projectName, datasetName, tableName);

        com.google.cloud.bigquery.Table table = this.bigQuery.getTable(tableId);
        boolean tableExists = table != null && table.exists();
        return tableExists;
    }

    public void validateDatasetSchema(Dataset dataset, TypeMapping typeMapping)
    {
        if (!(typeMapping instanceof DataTypeMapping))
        {
            throw new IllegalStateException("Only DataTypeMapping allowed in validateDatasetSchema");
        }
        DataTypeMapping datatypeMapping = (DataTypeMapping) typeMapping;
        String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schema = dataset.datasetReference().group().orElse(null);

        com.google.cloud.bigquery.Table table = this.bigQuery.getTable(TableId.of(schema, name));
        List<String> primaryKeysInDb = this.fetchPrimaryKeys(name, schema, dataset.datasetReference().database().orElse(null));
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
                matchedDataType = org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.valueOf(dbField.getType().name());
            }

            FieldType fieldType = FieldType.of(matchedDataType, columnSize, scale);
            DataType physicalDataType = datatypeMapping.getDataType(fieldType);

            // Check the constraints
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (com.google.cloud.bigquery.Field.Mode.REQUIRED.equals(dbField.getMode()))
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }
            if (primaryKeysInDb.contains(columnName))
            {
                columnConstraints.add(new PKColumnConstraint());
            }

            Column column = new Column(columnName, physicalDataType, columnConstraints, null);
            dbColumns.add(column);
        }

        // Compare the schemas
        validateColumns(userColumns, dbColumns);
    }

    public Dataset constructDatasetFromDatabase(Dataset dataset, TypeMapping typeMapping, boolean escape)
    {
        String tableName = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schemaName = dataset.datasetReference().group().orElse(null);
        String databaseName = dataset.datasetReference().database().orElse(null);
        if (!(typeMapping instanceof JdbcPropertiesToLogicalDataTypeMapping))
        {
            throw new IllegalStateException("Only JdbcPropertiesToLogicalDataTypeMapping allowed in constructDatasetFromDatabase");
        }
        JdbcPropertiesToLogicalDataTypeMapping mapping = (JdbcPropertiesToLogicalDataTypeMapping) typeMapping;
        List<String> primaryKeysInDb = this.fetchPrimaryKeys(tableName, schemaName, databaseName);
        com.google.cloud.bigquery.Table table = this.bigQuery.getTable(TableId.of(schemaName, tableName));

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
            boolean primaryKey = primaryKeysInDb.contains(columnName);

            Field field = Field.builder()
                    .name(columnName)
                    .type(fieldType)
                    .nullable(nullable)
                    .primaryKey(primaryKey)
                    .build();

            fields.add(field);
        }

        SchemaDefinition schemaDefinition = SchemaDefinition.builder()
                .addAllFields(fields)
                .build();
        return DatasetDefinition.builder().name(tableName).database(databaseName).group(schemaName).schema(schemaDefinition).datasetAdditionalProperties(dataset.datasetAdditionalProperties()).build();
    }

    private List<String> fetchPrimaryKeys(String tableName, String schemaName, String databaseName)
    {
        RelationalTransformer relationalTransformer = new RelationalTransformer(BigQuerySink.get());
        LogicalPlan logicalPlanFetchPrimaryKeys = getLogicalPlanFetchPrimaryKeys(tableName, schemaName, databaseName);
        SqlPlan sqlPlan = relationalTransformer.generatePhysicalPlan(logicalPlanFetchPrimaryKeys);
        List<Map<String, Object>> resultSet = executeQuery(sqlPlan.getSql());
        return resultSet.stream().map(resultEntry -> (String) resultEntry.get(COLUMN_NAME)).collect(Collectors.toList());
    }

    private static LogicalPlan getLogicalPlanFetchPrimaryKeys(String tableName, String schemaName, String databaseName)
    {
        return LogicalPlan.builder().addOps(Selection.builder()
                .addFields(FieldValue.builder().fieldName(COLUMN_NAME).build())
                .source(DatasetReferenceImpl.builder()
                        .database(databaseName)
                        .group(schemaName)
                        .name(PRIMARY_KEY_INFO_TABLE_NAME).build())
                .condition(And.of(Arrays.asList(
                        Equals.of(FieldValue.builder().fieldName(TABLE_SCHEMA).build(),
                                StringValue.of(schemaName)),
                        Equals.of(FieldValue.builder().fieldName(TABLE_NAME).build(),
                                StringValue.of(tableName)),
                        Equals.of(FieldValue.builder().fieldName(CONSTRAINT_NAME).build(),
                                StringValue.of(CONSTRAINT_NAME_PROVIDER_PK.apply(tableName))))))
                .build()).build();
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

    public void executeStatementInANewTransaction(String sql)
    {
        List<String> sqls = Collections.singletonList(sql);
        executeStatementsInANewTransaction(sqls);
    }

    // Execute statements in a transaction - either use an existing one or use a new one
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
            executeStatementsInANewTransaction(sqls);
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, int rows)
    {
        throw new UnsupportedOperationException("Not implemented for Big Query");
    }

    public void executeStatementsInANewTransaction(List<String> sqls)
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

    // Execute statement in a transaction - either use an existing one or use a new one
    public Map<StatisticName, Object> executeLoadStatement(String sql)
    {
        if (this.transactionManager != null)
        {
            try
            {
                return this.transactionManager.executeLoadStatement(sql);
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
                Map<StatisticName, Object> stats = txManager.executeLoadStatement(sql);
                txManager.commitTransaction();
                return stats;
            }
            catch (Exception e)
            {
                LOGGER.error("Error executing SQL statements: " + sql, e);
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

    @Override
    public void close()
    {
        closeTransactionManager();
    }
}
