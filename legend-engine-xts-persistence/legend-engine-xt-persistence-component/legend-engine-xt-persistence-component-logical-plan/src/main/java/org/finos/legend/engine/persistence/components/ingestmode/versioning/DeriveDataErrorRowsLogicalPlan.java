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
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.DistinctFunction;

import java.util.ArrayList;
import java.util.List;

public class DeriveDataErrorRowsLogicalPlan implements VersioningStrategyVisitor<LogicalPlan>
{
    private List<String> primaryKeys;
    private List<String> remainingColumns;
    private Dataset tempStagingDataset;
    private int sampleRowCount;
    private boolean useAliasInHaving;

    public static final String DATA_VERSION_ERROR_COUNT = "legend_persistence_error_count";

    public DeriveDataErrorRowsLogicalPlan(List<String> primaryKeys, List<String> remainingColumns, Dataset tempStagingDataset, int sampleRowCount, boolean useAliasInHaving)
    {
        this.primaryKeys = primaryKeys;
        this.remainingColumns = remainingColumns;
        this.tempStagingDataset = tempStagingDataset;
        this.sampleRowCount = sampleRowCount;
        this.useAliasInHaving = useAliasInHaving;
    }

    @Override
    public LogicalPlan visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        return null;
    }

    @Override
    public LogicalPlan visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        if (maxVersionStrategy.performStageVersioning())
        {
            return getLogicalPlanForDataErrors(maxVersionStrategy.versioningField(), useAliasInHaving);
        }
        else
        {
            return null;
        }
    }

    @Override
    public LogicalPlan visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
    {
        if (allVersionsStrategyAbstract.performStageVersioning())
        {
            return getLogicalPlanForDataErrors(allVersionsStrategyAbstract.versioningField(), useAliasInHaving);
        }
        else
        {
            return null;
        }
    }

    private LogicalPlan getLogicalPlanForDataErrors(String versionField, boolean useAliasInHaving)
    {
        List<Value> pKsAndVersion = new ArrayList<>();
        for (String pk: primaryKeys)
        {
            pKsAndVersion.add(FieldValue.builder().fieldName(pk).build());
        }
        pKsAndVersion.add(FieldValue.builder().fieldName(versionField).build());

        List<Value> distinctValueFields = new ArrayList<>();
        for (String field: remainingColumns)
        {
            distinctValueFields.add(FieldValue.builder().fieldName(field).build());
        }

        FunctionImpl countDistinct = FunctionImpl.builder()
                .functionName(FunctionName.COUNT)
                .addValue(DistinctFunction.builder().addAllValues(distinctValueFields).build())
                .alias(DATA_VERSION_ERROR_COUNT)
                .build();

        Condition havingCondition = GreaterThan.of(useAliasInHaving ? FieldValue.builder().fieldName(DATA_VERSION_ERROR_COUNT).build() : countDistinct, ObjectValue.of(1));

        Selection selectDataError = Selection.builder()
                .source(tempStagingDataset)
                .groupByFields(pKsAndVersion)
                .addAllFields(pKsAndVersion)
                .addFields(countDistinct)
                .havingCondition(havingCondition)
                .limit(sampleRowCount)
                .build();

        return LogicalPlan.builder().addOps(selectDataError).build();
    }

}