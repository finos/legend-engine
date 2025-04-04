// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api.utils;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PartitionExtractorUtils
{
    public static List<Map<String, Object>> extractPartitions(Executor<SqlGen, TabularData, SqlPlan> executor,
                                                        Transformer<SqlGen, SqlPlan> transformer,
                                                        Dataset srcDataset,
                                                        List<String> partitionFields,
                                                        Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        // Select distinct pk1, pk2, ... from staging_table with staging_filters
        List<Map<String, Object>> partitionSpecList = new ArrayList<>();
        LogicalPlan logicalPlanForPartitionSpec = LogicalPlanFactory.getLogicalPlanForDistinctValues(srcDataset, partitionFields);
        List<TabularData> partitionSpecResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForPartitionSpec), placeHolderKeyValues);

        if (!partitionSpecResult.isEmpty())
        {
            List<Map<String, Object>> partitionSpecRows = partitionSpecResult.get(0).data();
            for (Map<String, Object> partitionSpec: partitionSpecRows)
            {
                partitionSpecList.add(partitionSpec);
            }
        }

        return partitionSpecList;
    }
}
