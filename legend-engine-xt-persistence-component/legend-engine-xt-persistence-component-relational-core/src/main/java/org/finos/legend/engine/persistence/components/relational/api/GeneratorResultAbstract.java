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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.SqlPlanAbstract;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class GeneratorResultAbstract
{
    public static final String SINGLE_QUOTE = "'";

    public abstract SqlPlan preActionsSqlPlan();

    public abstract Optional<SqlPlan> schemaEvolutionSqlPlan();

    public abstract Optional<Dataset> schemaEvolutionDataset();

    public abstract SqlPlan ingestSqlPlan();

    public abstract Optional<DataSplitRange> ingestDataSplitRange();

    public abstract Optional<SqlPlan> metadataIngestSqlPlan();

    public abstract SqlPlan postActionsSqlPlan();

    public abstract Map<StatisticName, SqlPlan> preIngestStatisticsSqlPlan();

    public abstract Map<StatisticName, SqlPlan> postIngestStatisticsSqlPlan();

    public List<String> preActionsSql()
    {
        return preActionsSqlPlan().getSqlList();
    }

    public List<String> schemaEvolutionSql()
    {
        return schemaEvolutionSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> ingestSql()
    {
        return ingestDataSplitRange()
            .map(dataSplitRange -> ingestSqlPlan().getSqlList()
                .stream()
                .map(sql -> sql
                    .replace(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_LOWER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.lowerBound()))
                    .replace(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_UPPER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.upperBound())))
                .collect(Collectors.toList()))
            .orElseGet(ingestSqlPlan()::getSqlList);
    }

    public List<String> metadataIngestSql()
    {
        return metadataIngestSqlPlan().map(SqlPlanAbstract::getSqlList).orElse(Collections.emptyList());
    }

    public List<String> postActionsSql()
    {
        return postActionsSqlPlan().getSqlList();
    }

    public Map<StatisticName, String> preIngestStatisticsSql()
    {
        return preIngestStatisticsSqlPlan().keySet().stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> preIngestStatisticsSqlPlan().get(k).getSql()));
    }

    public Map<StatisticName, String> postIngestStatisticsSql()
    {
        return postIngestStatisticsSqlPlan().keySet().stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> postIngestStatisticsSqlPlan().get(k).getSql()));
    }
}
