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

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BigQueryExecutor implements Executor<SqlGen, TabularData, SqlPlan>
{
    private final BigQuerySink bigQuerySink;
    private final BigQueryHelper bigQueryHelper;

    public BigQueryExecutor(BigQuerySink bigQuerySink, BigQueryHelper bigQueryHelper)
    {
        this.bigQuerySink = bigQuerySink;
        this.bigQueryHelper = bigQueryHelper;
    }

    @Override
    public void executePhysicalPlan(SqlPlan physicalPlan)
    {
        executePhysicalPlan(physicalPlan, new HashMap<>());
    }

    @Override
    public void executePhysicalPlan(SqlPlan physicalPlan, Map<String, String> placeholderKeyValues)
    {
        boolean containsDDLStatements = physicalPlan.ops().stream().anyMatch(DDLStatement.class::isInstance);
        List<String> sqlList = physicalPlan.getSqlList();

        if (containsDDLStatements)
        {
            for (String sql : sqlList)
            {
                String enrichedSql = getEnrichedSql(placeholderKeyValues, sql);
                bigQueryHelper.executeQuery(enrichedSql);
            }
        }
        else
        {
            for (String sql : sqlList)
            {
                String enrichedSql = getEnrichedSql(placeholderKeyValues, sql);
                bigQueryHelper.executeStatement(enrichedSql);
            }
        }
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan)
    {
        return executePhysicalPlanAndGetResults(physicalPlan, new HashMap<>());
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan, Map<String, String> placeholderKeyValues)
    {
        List<TabularData> resultSetList = new ArrayList<>();
        for (String sql : physicalPlan.getSqlList())
        {
            String enrichedSql = getEnrichedSql(placeholderKeyValues, sql);
            List<Map<String, Object>> queryResult = bigQueryHelper.executeQuery(enrichedSql);
            if (!queryResult.isEmpty())
            {
                resultSetList.add(new TabularData(queryResult));
            }
        }
        return resultSetList;
    }

    @Override
    public boolean datasetExists(Dataset dataset)
    {
        return bigQuerySink.datasetExistsFn().apply(this, bigQueryHelper, dataset);
    }

    @Override
    public void validateMainDatasetSchema(Dataset dataset)
    {
        bigQuerySink.validateMainDatasetSchemaFn().execute(this, bigQueryHelper, dataset);
    }

    @Override
    public Dataset constructDatasetFromDatabase(String tableName, String schemaName, String databaseName)
    {
        return bigQuerySink.constructDatasetFromDatabaseFn().execute(this, bigQueryHelper, tableName, schemaName, databaseName);
    }

    @Override
    public void begin()
    {
        bigQueryHelper.beginTransaction();
    }

    @Override
    public void commit()
    {
        bigQueryHelper.commitTransaction();
    }

    @Override
    public void revert()
    {
        bigQueryHelper.revertTransaction();
    }

    @Override
    public void close()
    {
        bigQueryHelper.close();
    }

    private String getEnrichedSql(Map<String, String> placeholderKeyValues, String sql)
    {
        String enrichedSql = sql;
        for (Map.Entry<String, String> entry : placeholderKeyValues.entrySet())
        {
            enrichedSql = enrichedSql.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
        }
        return enrichedSql;
    }

}
