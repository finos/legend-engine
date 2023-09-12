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

package org.finos.legend.engine.persistence.components.relational.executor;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RelationalExecutor implements Executor<SqlGen, TabularData, SqlPlan>
{
    private final RelationalSink relationalSink;
    private final RelationalExecutionHelper relationalExecutionHelper;

    public RelationalExecutor(RelationalSink relationalSink, RelationalExecutionHelper relationalExecutionHelper)
    {
        this.relationalSink = relationalSink;
        this.relationalExecutionHelper = relationalExecutionHelper;
    }

    @Override
    public void executePhysicalPlan(SqlPlan physicalPlan)
    {
        List<String> sqlList = physicalPlan.getSqlList();
        relationalExecutionHelper.executeStatements(sqlList);
    }

    @Override
    public void executePhysicalPlan(SqlPlan physicalPlan, Map<String, String> placeholderKeyValues)
    {
        List<String> sqlList = physicalPlan.getSqlList();
        for (String sql : sqlList)
        {
            String enrichedSql = getEnrichedSql(placeholderKeyValues, sql);
            relationalExecutionHelper.executeStatement(enrichedSql);
        }
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan)
    {
        List<TabularData> resultSetList = new ArrayList<>();
        for (String sql : physicalPlan.getSqlList())
        {
            List<Map<String, Object>> queryResult = relationalExecutionHelper.executeQuery(sql);
            if (!queryResult.isEmpty())
            {
                resultSetList.add(new TabularData(queryResult));
            }
        }
        return resultSetList;
    }

    @Override
    public List<TabularData> executePhysicalPlanAndGetResults(SqlPlan physicalPlan, Map<String, String> placeholderKeyValues)
    {
        List<TabularData> resultSetList = new ArrayList<>();
        for (String sql : physicalPlan.getSqlList())
        {
            String enrichedSql = getEnrichedSql(placeholderKeyValues, sql);
            List<Map<String, Object>> queryResult = relationalExecutionHelper.executeQuery(enrichedSql);
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
        return relationalSink.datasetExistsFn().apply(this, relationalExecutionHelper, dataset);
    }

    @Override
    public void validateMainDatasetSchema(Dataset dataset)
    {
        relationalSink.validateMainDatasetSchemaFn().execute(this, relationalExecutionHelper, dataset);
    }

    @Override
    public Dataset constructDatasetFromDatabase(Dataset dataset)
    {
        return relationalSink.constructDatasetFromDatabaseFn().execute(this, relationalExecutionHelper, dataset);
    }

    @Override
    public void begin()
    {
        relationalExecutionHelper.beginTransaction();
    }

    @Override
    public void commit()
    {
        relationalExecutionHelper.commitTransaction();
    }

    @Override
    public void revert()
    {
        relationalExecutionHelper.revertTransaction();
    }

    @Override
    public void close()
    {
        relationalExecutionHelper.closeTransactionManager();
    }

    @Override
    public RelationalExecutionHelper getRelationalExecutionHelper()
    {
        return this.relationalExecutionHelper;
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
