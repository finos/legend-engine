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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.List;
import java.util.stream.Collectors;

public class DeriveMaxDuplicatePkCountLogicalPlan implements VersioningStrategyVisitor<LogicalPlan>
{

    List<String> primaryKeys;
    Dataset tempStagingDataset;

    public DeriveMaxDuplicatePkCountLogicalPlan(List<String> primaryKeys, Dataset tempStagingDataset)
    {
        this.primaryKeys = primaryKeys;
        this.tempStagingDataset = tempStagingDataset;
    }

    @Override
    public LogicalPlan visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        if (noVersioningStrategy.failOnDuplicatePrimaryKeys())
        {
            String maxPkCountAlias = DedupAndVersionErrorSqlType.MAX_PK_DUPLICATES.name();
            String pkCountAlias = "legend_persistence_pk_count";

            List<Value> pks = primaryKeys.stream().map(pkName -> FieldValue.builder().fieldName(pkName).build()).collect(Collectors.toList());

            FunctionImpl count = FunctionImpl.builder()
                .functionName(FunctionName.COUNT)
                .addValue(All.INSTANCE)
                .alias(pkCountAlias)
                .build();

            Selection selectPkCount = Selection.builder()
                .source(tempStagingDataset)
                .groupByFields(pks)
                .addFields(count)
                .alias(tempStagingDataset.datasetReference().alias())
                .build();

            FunctionImpl maxCount = FunctionImpl.builder()
                .functionName(FunctionName.MAX)
                .addValue(FieldValue.builder().fieldName(pkCountAlias).build())
                .alias(maxPkCountAlias)
                .build();

            Selection selectMaxPkCountCount = Selection.builder()
                .source(selectPkCount)
                .addFields(maxCount)
                .build();

            return LogicalPlan.builder().addOps(selectMaxPkCountCount).build();
        }
        else
        {
            return null;
        }
    }

    @Override
    public LogicalPlan visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        return null;
    }

    @Override
    public LogicalPlan visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
    {
        return null;
    }
}