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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetDeduplicator implements VersioningStrategyVisitor<Dataset>
{

    Dataset stagingDataset;
    List<String> primaryKeys;

    private static final String ROW_NUMBER = "legend_persistence_row_num";

    public DatasetDeduplicator(Dataset stagingDataset, List<String> primaryKeys)
    {
        this.stagingDataset = stagingDataset;
        this.primaryKeys = primaryKeys;
    }

    @Override
    public Dataset visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        return this.stagingDataset;
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
        return enrichedStagingDataset;
    }
}
