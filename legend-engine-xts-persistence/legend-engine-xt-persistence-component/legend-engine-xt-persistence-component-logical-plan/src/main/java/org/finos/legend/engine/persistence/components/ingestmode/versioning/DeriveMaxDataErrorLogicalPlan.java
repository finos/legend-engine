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

import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.DistinctFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.ArrayList;
import java.util.List;

public class DeriveMaxDataErrorLogicalPlan implements VersioningStrategyVisitor<LogicalPlan>
{

    List<String> primaryKeys;
    List<String> remainingColumns;
    Dataset tempStagingDataset;

    public DeriveMaxDataErrorLogicalPlan(List<String> primaryKeys, List<String> remainingColumns, Dataset tempStagingDataset)
    {
        this.primaryKeys = primaryKeys;
        this.remainingColumns = remainingColumns;
        this.tempStagingDataset = tempStagingDataset;
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
            return getLogicalPlanForDataErrorCheck(maxVersionStrategy.versioningField());
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
            return getLogicalPlanForDataErrorCheck(allVersionsStrategyAbstract.versioningField());
        }
        else
        {
            return null;
        }
    }

    private LogicalPlan getLogicalPlanForDataErrorCheck(String versionField)
    {
        String maxDataErrorAlias = DedupAndVersionErrorSqlType.MAX_DATA_ERRORS.name();
        String distinctRowCount = "legend_persistence_distinct_rows";
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
                .alias(distinctRowCount)
                .build();

        Selection selectCountDataError = Selection.builder()
                .source(tempStagingDataset)
                .groupByFields(pKsAndVersion)
                .addFields(countDistinct)
                .alias(tempStagingDataset.datasetReference().alias())
                .build();
        FunctionImpl maxCount = FunctionImpl.builder()
                .functionName(FunctionName.MAX)
                .addValue(FieldValue.builder().fieldName(distinctRowCount).build())
                .alias(maxDataErrorAlias)
                .build();
        Selection maxDataErrorCount = Selection.builder()
                .source(selectCountDataError)
                .addFields(maxCount)
                .build();
        return LogicalPlan.builder().addOps(maxDataErrorCount).build();
    }

}