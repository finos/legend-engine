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

package org.finos.legend.engine.persistence.components.util;

import java.util.UUID;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.In;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotEquals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotIn;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.Array;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.HashFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

public class LogicalPlanUtils
{
    public static final long INFINITE_BATCH_ID = 999999999L;
    public static final String INFINITE_BATCH_TIME = "9999-12-31 23:59:59";
    public static final String DEFAULT_META_TABLE = "batch_metadata";
    public static final String DATA_SPLIT_LOWER_BOUND_PLACEHOLDER = "{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}";
    public static final String DATA_SPLIT_UPPER_BOUND_PLACEHOLDER = "{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}";

    private static final String UNDERSCORE = "_";

    private LogicalPlanUtils()
    {
    }

    public static String generateTableNameWithSuffix(String tableName, String suffix)
    {
        UUID uuid = UUID.randomUUID();
        return tableName + UNDERSCORE + suffix + UNDERSCORE + uuid;
    }

    public static NumericalValue INFINITE_BATCH_ID()
    {
        return NumericalValue.of(LogicalPlanUtils.INFINITE_BATCH_ID);
    }

    public static StringValue INFINITE_BATCH_TIME()
    {
        return StringValue.of(LogicalPlanUtils.INFINITE_BATCH_TIME);
    }

    public static List<Value> ALL_COLUMNS()
    {
        return Collections.singletonList(All.INSTANCE);
    }

    public static Condition getDataSplitInRangeCondition(Dataset dataset, String dataSplitField)
    {
        FieldValue dataSplit = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(dataSplitField).build();
        return And.builder()
            .addConditions(
                GreaterThanEqualTo.of(dataSplit, StringValue.of(DATA_SPLIT_LOWER_BOUND_PLACEHOLDER)),
                LessThanEqualTo.of(dataSplit, StringValue.of(DATA_SPLIT_UPPER_BOUND_PLACEHOLDER)))
            .build();
    }

    public static Condition getPrimaryKeyMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String[] pkFields)
    {
        return getColumnsMatchCondition(mainDataSet, stagingDataSet, pkFields);
    }

    public static Condition getPartitionColumnsMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String[] partitionColumns)
    {
        return getColumnsMatchCondition(mainDataSet, stagingDataSet, partitionColumns);
    }

    public static Condition getPartitionColumnsDoNotMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String[] partitionColumns)
    {
        return getColumnsDoNotMatchCondition(mainDataSet, stagingDataSet, partitionColumns);
    }

    public static Condition getPartitionColumnValueMatchInCondition(Dataset dataSet, Map<String, Set<String>> partitionFilter)
    {
        return getColumnValueMatchInCondition(dataSet, partitionFilter);
    }

    private static Condition getColumnValueMatchInCondition(Dataset dataSet, Map<String, Set<String>> keyValuePair)
    {
        return And.of(
            keyValuePair.entrySet()
                .stream()
                .map(columnValuePair -> In.of(
                    FieldValue.builder().datasetRef(dataSet.datasetReference()).fieldName(columnValuePair.getKey()).build(),
                    Array.of(columnValuePair.getValue().stream().map(StringValue::of).collect(Collectors.toList()))))
                .collect(Collectors.toList()));
    }

    private static Condition getColumnsMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String[] columns)
    {
        return And.of(
            Arrays.stream(columns)
                .map(fieldName -> Equals.of(
                    FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(fieldName).build(),
                    FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(fieldName).build()))
                .collect(Collectors.toList()));
    }

    private static Condition getColumnsDoNotMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String[] columns)
    {
        return Or.of(
            Arrays.stream(columns)
                .map(fieldName -> NotEquals.of(
                    FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(fieldName).build(),
                    FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(fieldName).build()))
                .collect(Collectors.toList()));
    }

    public static Condition getDigestMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String digestField)
    {
        return Equals.of(
            FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(digestField).build(),
            FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(digestField).build());
    }

    public static Condition getDigestDoesNotMatchCondition(Dataset mainDataSet, Dataset stagingDataSet, String digestField)
    {
        return NotEquals.of(
            FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(digestField).build(),
            FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(digestField).build());
    }

    public static Condition getDeleteIndicatorIsSetCondition(Dataset stagingDataSet, String deleteIndicatorField, List<Object> deleteIndicatorValues)
    {
        Field deleteIndicator = stagingDataSet.schema().fields().stream()
            .filter(field -> field.name().equalsIgnoreCase(deleteIndicatorField))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Delete indicator [" + deleteIndicatorField + "] not found in staging dataset"));
        boolean isStringDatatype = DataType.isStringDatatype(deleteIndicator.type().dataType());

        // if there is a single deleteIndicatorValue Use EqualityCondition with String/Object value as needed
        if (deleteIndicatorValues.size() == 1)
        {
            return Equals.of(
                FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(deleteIndicatorField).build(),
                getDeleteIndicatorValue(deleteIndicatorValues.get(0), isStringDatatype));
        }

        // if there are multiple deleteIndicatorValues Use InCondition with String/Object value as needed
        return In.of(
            FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(deleteIndicatorField).build(),
            Array.of(deleteIndicatorValues.stream().map(val -> getDeleteIndicatorValue(val, isStringDatatype)).collect(Collectors.toList())));
    }

    public static Condition getDeleteIndicatorIsNotSetCondition(Dataset stagingDataSet, String deleteIndicatorField, List<Object> deleteIndicatorValues)
    {
        Field deleteIndicator = stagingDataSet.schema().fields().stream()
            .filter(field -> field.name().equalsIgnoreCase(deleteIndicatorField))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Delete indicator [\" + deleteIndicatorField + \"] not found in staging dataset"));
        boolean isStringDatatype = DataType.isStringDatatype(deleteIndicator.type().dataType());

        // if there is a single deleteIndicatorValue Use NonEqualityCondition with String/Object value as needed
        if (deleteIndicatorValues.size() == 1)
        {
            return NotEquals.of(
                FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(deleteIndicatorField).build(),
                getDeleteIndicatorValue(deleteIndicatorValues.get(0), isStringDatatype));
        }

        // if there is multiple deleteIndicatorValues Use NotInCondition with String/Object value as needed
        return NotIn.of(
            FieldValue.builder().datasetRef(stagingDataSet.datasetReference()).fieldName(deleteIndicatorField).build(),
            Array.of(deleteIndicatorValues.stream().map(val -> getDeleteIndicatorValue(val, isStringDatatype)).collect(Collectors.toList())));
    }

    private static Value getDeleteIndicatorValue(Object value, boolean isStringDatatype)
    {
        if (isStringDatatype)
        {
            return StringValue.of(value.toString());
        }
        return ObjectValue.of(value);
    }

    public static void removeField(List<Value> fieldsList, String fieldName)
    {
        fieldsList.removeIf(field ->
            field instanceof FieldValue && ((FieldValue) field).fieldName().equalsIgnoreCase(fieldName)
        );
    }

    public static Condition getBatchIdEqualsInfiniteCondition(Dataset mainDataSet, String batchIdOutField)
    {
        return Equals.of(
            FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(batchIdOutField).build(),
            LogicalPlanUtils.INFINITE_BATCH_ID());
    }

    public static Condition getBatchTimeEqualsInfiniteCondition(Dataset mainDataSet, String batchTimeOutField)
    {
        return Equals.of(
            FieldValue.builder().datasetRef(mainDataSet.datasetReference()).fieldName(batchTimeOutField).build(),
            LogicalPlanUtils.INFINITE_BATCH_TIME());
    }

    public static Selection getRecordCount(Dataset dataset, String alias)
    {
        return Selection.builder()
            .source(dataset.datasetReference())
            .addFields(FunctionImpl.builder().functionName(FunctionName.COUNT).alias(alias).addValue(All.INSTANCE).build())
            .build();
    }

    public static Selection getRecordCount(Dataset dataset, String alias, Optional<Condition> condition)
    {
        return Selection.builder()
                .source(dataset.datasetReference())
                .addFields(FunctionImpl.builder().functionName(FunctionName.COUNT).alias(alias).addValue(All.INSTANCE).build())
                .condition(condition)
                .build();
    }

    public static Condition getBatchIdEqualityCondition(Dataset dataset, Value batchId, String batchIdField)
    {
        return Equals.of(
            FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(batchIdField).build(),
            batchId);
    }

    public static Condition getBatchTimeEqualityCondition(Dataset dataset, Value batchTime, String batchTimeField)
    {
        return Equals.of(
            FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(batchTimeField).build(),
            batchTime);
    }

    // Used in Incremental
    public static Selection getRowsBasedOnLatestTimestamp(Dataset dataset, String field, String alias)
    {
        FieldValue fieldValue = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(field).build();

        FunctionImpl maxFunction = FunctionImpl.builder().functionName(FunctionName.MAX).addValue(fieldValue).build();
        SelectValue maxTs = SelectValue.of(Selection.builder().source(dataset.datasetReference()).addFields(maxFunction).build());
        Equals<FieldValue, SelectValue> condition = Equals.of(fieldValue, maxTs);
        FunctionImpl countFunction = FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(alias).build();

        return Selection.builder().source(dataset.datasetReference()).condition(condition).addFields(countFunction).build();
    }
}
