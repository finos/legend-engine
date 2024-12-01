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

package org.finos.legend.engine.persistence.components.relational.duckdb.jdbc;

import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.executor.TypeMapping;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuckDBJdbcHelper extends JdbcHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DuckDBJdbcHelper.class);

    public static DuckDBJdbcHelper of(Connection connection)
    {
        return new DuckDBJdbcHelper(connection);
    }

    private DuckDBJdbcHelper(Connection connection)
    {
        super(connection);
    }

    @Override
    public boolean doesTableExist(Dataset dataset)
    {
        try
        {
            String name = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
            String database = dataset.datasetReference().database().orElse(null);
            String schema = dataset.datasetReference().group().orElse(null);
            try (ResultSet result = this.connection.getMetaData().getTables(database, schema, name, null))
            {
                return result.next(); // This method returns true if ResultSet is not empty
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dataset constructDatasetFromDatabase(Dataset dataset, TypeMapping typeMapping, boolean escape)
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

                Field field = Field.builder().name(columnName).type(fieldType).nullable(nullable).identity(identity).primaryKey(primaryKey).build();

                fields.add(field);
            }

            SchemaDefinition schemaDefinition = SchemaDefinition.builder().addAllFields(fields).build();
            return DatasetDefinition.builder().name(tableName).database(databaseName).group(schemaName).schema(schemaDefinition).datasetAdditionalProperties(dataset.datasetAdditionalProperties()).build();
        }
        catch (SQLException e)
        {
            LOGGER.error("Exception in Constructing dataset Schema from Database", e);
            throw new RuntimeException(e);
        }
    }
}
