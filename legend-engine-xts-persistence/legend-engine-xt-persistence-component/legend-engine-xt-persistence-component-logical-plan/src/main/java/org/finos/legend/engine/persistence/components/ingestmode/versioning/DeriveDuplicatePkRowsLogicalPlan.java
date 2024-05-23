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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.List;
import java.util.stream.Collectors;

public class DeriveDuplicatePkRowsLogicalPlan implements VersioningStrategyVisitor<LogicalPlan>
{
    private List<String> primaryKeys;
    private Dataset tempStagingDataset;
    private int sampleRowCount;
    private boolean useAliasInHaving;

    public static final String DUPLICATE_PK_COUNT = "legend_persistence_pk_count";

    public DeriveDuplicatePkRowsLogicalPlan(List<String> primaryKeys, Dataset tempStagingDataset, int sampleRowCount, boolean useAliasInHaving)
    {
        this.primaryKeys = primaryKeys;
        this.tempStagingDataset = tempStagingDataset;
        this.sampleRowCount = sampleRowCount;
        this.useAliasInHaving = useAliasInHaving;
    }

    @Override
    public LogicalPlan visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        if (noVersioningStrategy.failOnDuplicatePrimaryKeys())
        {
            List<Value> pks = primaryKeys.stream().map(pkName -> FieldValue.builder().fieldName(pkName).build()).collect(Collectors.toList());

            FunctionImpl count = FunctionImpl.builder()
                .functionName(FunctionName.COUNT)
                .addValue(All.INSTANCE)
                .alias(DUPLICATE_PK_COUNT)
                .build();

            Condition havingCondition = GreaterThan.of(useAliasInHaving ? FieldValue.builder().fieldName(DUPLICATE_PK_COUNT).build() : count, ObjectValue.of(1));

            Selection selectDuplicatePks = Selection.builder()
                .source(tempStagingDataset)
                .groupByFields(pks)
                .addAllFields(pks)
                .addFields(count)
                .havingCondition(havingCondition)
                .limit(sampleRowCount)
                .build();

            return LogicalPlan.builder().addOps(selectDuplicatePks).build();
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