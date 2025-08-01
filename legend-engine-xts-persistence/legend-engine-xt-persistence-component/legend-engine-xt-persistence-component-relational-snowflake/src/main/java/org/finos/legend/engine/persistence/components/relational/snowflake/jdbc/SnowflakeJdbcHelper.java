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

package org.finos.legend.engine.persistence.components.relational.snowflake.jdbc;

import org.finos.legend.engine.persistence.components.executor.RelationalTransactionManager;
import org.finos.legend.engine.persistence.components.executor.TypeMapping;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.ArrayList;

public class SnowflakeJdbcHelper extends JdbcHelper
{
    private SnowflakeJdbcHelper(Connection connection)
    {
        super(connection);
    }

    public static SnowflakeJdbcHelper of(Connection connection)
    {
        return new SnowflakeJdbcHelper(connection);
    }

    @Override
    protected RelationalTransactionManager intializeTransactionManager(Connection connection) throws SQLException
    {
        return new SnowflakeJdbcTransactionManager(connection);
    }

    @Override
    public Dataset constructDatasetFromDatabase(Dataset dataset, TypeMapping typeMapping, boolean escape)
    {
        Dataset datasetConstructedFromDatabase = super.constructDatasetFromDatabase(dataset, typeMapping, escape);
        return datasetConstructedFromDatabase.withSchema(datasetConstructedFromDatabase.schema().withClusterKeys(fetchClusterKey(dataset)));
    }

    private List<ClusterKey> fetchClusterKey(Dataset dataset)
    {
        try
        {
            String databaseName = dataset.datasetReference().database().orElseThrow(IllegalStateException::new).toUpperCase();
            String schemaName = dataset.datasetReference().group().orElseThrow(IllegalStateException::new).toUpperCase();
            String tableName = dataset.datasetReference().name().orElseThrow(IllegalStateException::new).toUpperCase();
            String sql = String.format("SELECT CLUSTERING_KEY FROM %S.INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s';", databaseName, schemaName, tableName);
            Object clusteringKey = executeQuery(sql, 1).get(0).get("CLUSTERING_KEY");
            if (clusteringKey == null)
            {
                return new ArrayList<>();
            }
            else
            {
                // Clustering Key Format = LINEAR(c1, c2)
                String clusteringKeyString = clusteringKey.toString();
                String prefix = "LINEAR(";
                String suffix = ")";
                if (clusteringKeyString.startsWith(prefix) && clusteringKeyString.endsWith(suffix))
                {
                    String keysPart = clusteringKeyString.substring(prefix.length(), clusteringKeyString.length() - suffix.length());
                    String[] clusterKeys = Arrays.stream(keysPart.split(","))
                                                .map(String::trim)
                                                .toArray(String[]::new);
                    // Use clusterKeys safely
                    return Arrays.stream(clusterKeys)
                        .map(column -> ClusterKey.of(FieldValue.builder().fieldName(column).build()))
                        .collect(Collectors.toList());
                }
                else
                {
                    throw new IllegalArgumentException("Invalid clustering key format: " + clusteringKeyString);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error fetching cluster keys for dataset: " + dataset.datasetReference().name().get(), e);
        }
    }
}