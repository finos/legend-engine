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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;

import java.util.ArrayList;
import java.util.List;

public class DeriveDataErrorsLogicalPlan implements VersioningStrategyVisitor<LogicalPlan>
{
    private List<String> primaryKeys;
    private List<String> remainingColumns;
    private Dataset tempStagingDataset;
    private int sampleRowCount;

    public DeriveDataErrorsLogicalPlan(List<String> primaryKeys, List<String> remainingColumns, Dataset tempStagingDataset, int sampleRowCount)
    {
        this.primaryKeys = primaryKeys;
        this.remainingColumns = remainingColumns;
        this.tempStagingDataset = tempStagingDataset;
        this.sampleRowCount = sampleRowCount;
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
            return getLogicalPlanForDataErrors(maxVersionStrategy.versioningField());
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
            return getLogicalPlanForDataErrors(allVersionsStrategyAbstract.versioningField());
        }
        else
        {
            return null;
        }
    }

    private LogicalPlan getLogicalPlanForDataErrors(String versionField)
    {
        String distinctRowCount = "legend_persistence_error_count";
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
                .addValue(FunctionImpl.builder().functionName(FunctionName.DISTINCT).addAllValue(distinctValueFields).build())
                .alias(distinctRowCount)
                .build();

        Selection selectDataError = Selection.builder()
                .source(tempStagingDataset)
                .groupByFields(pKsAndVersion)
                .addAllFields(pKsAndVersion)
                .addFields(countDistinct)
                .havingCondition(GreaterThan.of(FieldValue.builder().fieldName(distinctRowCount).build(), ObjectValue.of(1)))
                .limit(sampleRowCount)
                .build();

        return LogicalPlan.builder().addOps(selectDataError).build();
    }

}