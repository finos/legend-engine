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

package org.finos.legend.engine.persistence.components.ingestmode.deduplication;

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DatasetFilterAndDeduplicator implements VersioningStrategyVisitor<Dataset>
{

    Dataset stagingDataset;
    List<String> primaryKeys;
    Optional<Condition> stagingDatasetFilter;

    private static final String ROW_NUMBER = "legend_persistence_row_num";

    public DatasetFilterAndDeduplicator(Dataset stagingDataset, List<String> primaryKeys)
    {
        this.stagingDataset = stagingDataset;
        this.primaryKeys = primaryKeys;
        this.stagingDatasetFilter = LogicalPlanUtils.getDatasetFilterCondition(stagingDataset);
    }

    @Override
    public Dataset visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        Dataset enrichedStagingDataset = this.stagingDataset;
        if (this.stagingDatasetFilter.isPresent())
        {
            enrichedStagingDataset = filterDataset();
        }
        return enrichedStagingDataset;
    }

    @Override
    public Dataset visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        Dataset enrichedStagingDataset = this.stagingDataset;
        if (maxVersionStrategy.performDeduplication())
        {
            OrderedField orderByField = OrderedField.builder()
                    .fieldName(maxVersionStrategy.versioningField())
                    .datasetRef(stagingDataset.datasetReference())
                    .order(Order.DESC).build();
            List<Value> allColumns = new ArrayList<>(stagingDataset.schemaReference().fieldValues());
            List<Value> allColumnsWithRowNumber = new ArrayList<>(stagingDataset.schemaReference().fieldValues());
            List<FieldValue> partitionFields = primaryKeys.stream()
                    .map(field -> FieldValue.builder().fieldName(field).datasetRef(stagingDataset.datasetReference()).build())
                    .collect(Collectors.toList());
            Value rowNumber = WindowFunction.builder()
                    .windowFunction(FunctionImpl.builder().functionName(FunctionName.ROW_NUMBER).build())
                    .addAllPartitionByFields(partitionFields)
                    .addOrderByFields(orderByField)
                    .alias(ROW_NUMBER)
                    .build();
            allColumnsWithRowNumber.add(rowNumber);
            Selection selectionWithRowNumber = Selection.builder()
                    .source(stagingDataset)
                    .addAllFields(allColumnsWithRowNumber)
                    .condition(stagingDatasetFilter)
                    .alias(stagingDataset.datasetReference().alias())
                    .build();

            Condition rowNumberFilterCondition = Equals.of(FieldValue.builder().fieldName(ROW_NUMBER).datasetRefAlias(stagingDataset.datasetReference().alias()).build(), ObjectValue.of(1));

            enrichedStagingDataset = Selection.builder()
                    .source(selectionWithRowNumber)
                    .addAllFields(allColumns)
                    .condition(rowNumberFilterCondition)
                    .alias(stagingDataset.datasetReference().alias())
                    .build();
        }
        else if (this.stagingDatasetFilter.isPresent())
        {
            enrichedStagingDataset = filterDataset();
        }
        return enrichedStagingDataset;
    }

    private Dataset filterDataset()
    {
        List<Value> allColumns = new ArrayList<>(stagingDataset.schemaReference().fieldValues());
        Selection selection = Selection.builder()
                .source(this.stagingDataset)
                .addAllFields(allColumns)
                .condition(this.stagingDatasetFilter.get())
                .alias(stagingDataset.datasetReference().alias())
                .build();
        return selection;
    }
}
