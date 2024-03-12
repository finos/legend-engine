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

package org.finos.legend.engine.persistence.components.logicalplan;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.LoadCsv;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Function;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.TabularValues;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;

import java.util.List;

public class LogicalPlanFactory
{
    public static final String IS_TABLE_NON_EMPTY = "isTableNonEmpty";
    public static final String TABLE_IS_NON_EMPTY = "1";
    public static final String TABLE_ALIAS = "X";
    public static final String MAX_OF_FIELD = "MAX";
    public static final String MIN_OF_FIELD = "MIN";

    public static LogicalPlan getLogicalPlanForIsDatasetEmpty(Dataset dataset)
    {
        Function countAllRows = FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(IS_TABLE_NON_EMPTY).build();
        Selection selectWithLimit = Selection.builder().source(dataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).limit(1).alias(TABLE_ALIAS).build();
        return LogicalPlan.builder()
            .addOps(Selection.builder().addFields(countAllRows).source(selectWithLimit).build())
            .build();
    }

    public static LogicalPlan getDatasetCreationPlan(Dataset dataset, boolean createIfNotExists)
    {
        return LogicalPlan.builder().addOps(Create.of(createIfNotExists, dataset)).build();
    }

    public static LogicalPlan getInsertPlan(Dataset targetDataset, List<Value> fieldsToInsert, List<List<Value>> valuesToInsert, int numFields)
    {
        Dataset values = TabularValues.builder().addAllValues(valuesToInsert).columnCount(numFields).build();
        return LogicalPlan.builder().addOps(Insert.of(targetDataset, values, fieldsToInsert)).build();
    }

    public static LogicalPlan getLoadCsvPlan(CsvExternalDatasetReference csvExternalDatasetReference)
    {
        return LogicalPlan.builder()
            .addOps(LoadCsv.of(csvExternalDatasetReference.getDatasetDefinition(), csvExternalDatasetReference))
            .build();
    }

    public static LogicalPlan getLogicalPlanForDoesDatasetExist(Dataset dataset)
    {
        return LogicalPlan.builder().addOps(Show.of(Show.ShowType.TABLES, dataset)).build();
    }

    public static LogicalPlan getLogicalPlanForValidateDatasetSchema(Dataset dataset)
    {
        return LogicalPlan.builder().addOps(Show.of(Show.ShowType.COLUMNS, dataset)).build();
    }

    public static LogicalPlan getLogicalPlanForConstantStats(String stats, Long value)
    {
        return LogicalPlan.builder().addOps(
            Selection.builder().addFields(NumericalValue.builder().value(value).alias(stats).build()).build())
            .build();
    }

    public static LogicalPlan getLogicalPlanForNextBatchId(Datasets datasets)
    {
        StringValue mainTable = StringValue.of(datasets.mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        MetadataDataset metadataDataset = datasets.metadataDataset().orElse(MetadataDataset.builder().build());
        MetadataUtils metadataUtils = new MetadataUtils(metadataDataset);
        Selection selection = metadataUtils.getBatchId(mainTable).selection();
        return LogicalPlan.builder().addOps(selection).build();
    }

    public static LogicalPlan getLogicalPlanForMinAndMaxForField(Dataset dataset, String fieldName)
    {
        FieldValue field = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(fieldName).build();
        Selection selection = Selection.builder()
            .addFields(FunctionImpl.builder().functionName(FunctionName.MIN).addValue(field).alias(MIN_OF_FIELD).build())
            .addFields(FunctionImpl.builder().functionName(FunctionName.MAX).addValue(field).alias(MAX_OF_FIELD).build())
            .source(dataset).build();
        return LogicalPlan.builder().addOps(selection).build();
    }

    public static LogicalPlan getLogicalPlanForMaxOfField(Dataset dataset, String fieldName)
    {
        FieldValue field = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(fieldName).build();
        Selection selection = Selection.builder()
                .addFields(FunctionImpl.builder().functionName(FunctionName.MAX).addValue(field).alias(MAX_OF_FIELD).build())
                .source(dataset).build();
        return LogicalPlan.builder().addOps(selection).build();
    }
}
