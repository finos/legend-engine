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

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetVersioningHandler implements VersioningStrategyVisitor<Dataset>
{

    Dataset dataset;
    List<String> primaryKeys;

    private static final String RANK = "legend_persistence_rank";

    public DatasetVersioningHandler(Dataset dataset, List<String> primaryKeys)
    {
        this.dataset = dataset;
        this.primaryKeys = primaryKeys;
    }

    @Override
    public Dataset visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        return this.dataset;
    }

    @Override
    public Dataset visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        if (!maxVersionStrategy.performStageVersioning())
        {
            return this.dataset;
        }
        OrderedField orderByField = OrderedField.builder()
                .fieldName(maxVersionStrategy.versioningField())
                .datasetRef(dataset.datasetReference())
                .order(Order.DESC).build();
        List<Value> allColumns = new ArrayList<>(dataset.schemaReference().fieldValues());
        List<Value> allColumnsWithRank = new ArrayList<>(dataset.schemaReference().fieldValues());
        List<FieldValue> partitionFields = primaryKeys.stream()
                .map(field -> FieldValue.builder().fieldName(field).datasetRef(dataset.datasetReference()).build())
                .collect(Collectors.toList());
        Value rank = WindowFunction.builder()
                .windowFunction(FunctionImpl.builder().functionName(FunctionName.DENSE_RANK).build())
                .addAllPartitionByFields(partitionFields)
                .addOrderByFields(orderByField)
                .alias(RANK)
                .build();
        allColumnsWithRank.add(rank);
        Selection selectionWithRank = Selection.builder()
                .source(dataset)
                .addAllFields(allColumnsWithRank)
                .alias(dataset.datasetReference().alias())
                .build();

        Condition rankFilterCondition = Equals.of(FieldValue.builder().fieldName(RANK).datasetRefAlias(dataset.datasetReference().alias()).build(), ObjectValue.of(1));

        Dataset enrichedStagingDataset = Selection.builder()
                .source(selectionWithRank)
                .addAllFields(allColumns)
                .condition(rankFilterCondition)
                .build();

        return enrichedStagingDataset;
    }

    @Override
    public Dataset visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
    {
        if (!allVersionsStrategyAbstract.performStageVersioning())
        {
            return this.dataset;
        }
        OrderedField orderByField = OrderedField.builder()
                .fieldName(allVersionsStrategyAbstract.versioningField())
                .datasetRef(dataset.datasetReference())
                .order(Order.ASC).build();
        List<FieldValue> partitionFields = primaryKeys.stream()
                .map(field -> FieldValue.builder().fieldName(field).datasetRef(dataset.datasetReference()).build())
                .collect(Collectors.toList());
        Value rank = WindowFunction.builder()
                .windowFunction(FunctionImpl.builder().functionName(FunctionName.DENSE_RANK).build())
                .addAllPartitionByFields(partitionFields)
                .addOrderByFields(orderByField)
                .alias(allVersionsStrategyAbstract.dataSplitFieldName())
                .build();
        List<Value> allColumnsWithRank = new ArrayList<>(dataset.schemaReference().fieldValues());

        allColumnsWithRank.add(rank);
        Selection selectionWithRank = Selection.builder()
                .source(dataset)
                .addAllFields(allColumnsWithRank)
                .build();
        return selectionWithRank;
    }
}
