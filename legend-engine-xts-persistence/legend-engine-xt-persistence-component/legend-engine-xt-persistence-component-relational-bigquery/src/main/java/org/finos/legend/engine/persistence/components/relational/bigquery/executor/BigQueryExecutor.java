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

import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.finos.legend.engine.persistence.components.util.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BigQueryExecutor implements Executor<SqlGen, TabularData, SqlPlan>
{
    private final BigQuerySink bigQuerySink;
    private final BigQueryHelper bigQueryHelper;
    private SqlLogging sqlLogging = SqlLogging.DISABLED;

    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryExecutor.class);

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
    public void executePhysicalPlan(SqlPlan physicalPlan, Map<String, PlaceholderValue> placeholderKeyValues)
    {
        boolean containsDDLStatements = physicalPlan.ops().stream().anyMatch(DDLStatement.class::isInstance);
        List<String> sqlList = physicalPlan.getSqlList();

        if (containsDDLStatements)
        {
            for (String sql : sqlList)
            {
                String enrichedSql = SqlUtils.getEnrichedSql(placeholderKeyValues, sql);
                SqlUtils.logSql(LOGGER, sqlLogging, sql, enrichedSql, placeholderKeyValues);
                bigQueryHelper.executeQuery(enrichedSql);
            }
        }
        else
        {
            for (String sql : sqlList)
            {
                String enrichedSql = SqlUtils.getEnrichedSql(placeholderKeyValues, sql);
                SqlUtils.logSql(LOGGER, sqlLogging, sql, enrichedSql, placeholderKeyValues);
                bigQueryHelper.executeStatement(enrichedSql);
            }
        }
    }

    public Map<StatisticName, Object> executeLoadPhysicalPlanAndGetStats(SqlPlan physicalPlan, Map<String, PlaceholderValue> placeholderKeyValues)
    {
        String enrichedSql = SqlUtils.getEnrichedSql(placeholderKeyValues, physicalPlan.getSqlList().get(0));
        SqlUtils.logSql(LOGGER, sqlLogging, physicalPlan.getSqlList().get(0), enrichedSql, placeholderKeyValues);
        return bigQueryHelper.executeLoadStatement(enrichedSql);
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan)
    {
        return executePhysicalPlanAndGetResults(physicalPlan, new HashMap<>());
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan, int rows)
    {
        throw new UnsupportedOperationException("Not implemented for Big Query");
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan, Map<String, PlaceholderValue> placeholderKeyValues)
    {
        List<TabularData> resultSetList = new ArrayList<>();
        for (String sql : physicalPlan.getSqlList())
        {
            String enrichedSql = SqlUtils.getEnrichedSql(placeholderKeyValues, sql);
            SqlUtils.logSql(LOGGER, sqlLogging, sql, enrichedSql, placeholderKeyValues);
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
    public Dataset constructDatasetFromDatabase(Dataset dataset)
    {
        return bigQuerySink.constructDatasetFromDatabaseFn().execute(this, bigQueryHelper, dataset);
    }

    @Override
    public void setSqlLogging(SqlLogging sqlLogging)
    {
        this.sqlLogging = sqlLogging;
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

    @Override
    public RelationalExecutionHelper getRelationalExecutionHelper()
    {
        return this.bigQueryHelper;
    }
}
