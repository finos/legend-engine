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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.exception.JsonReadOrWriteException;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.*;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.*;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.In;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.IsNull;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotEquals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotIn;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.Array;
import org.finos.legend.engine.persistence.components.logicalplan.values.DatetimeValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.InfiniteBatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.BIGINT;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.DATE;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.DECIMAL;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.DOUBLE;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.FLOAT;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.INT;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.INTEGER;
import static org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType.VARCHAR;
import static org.finos.legend.engine.persistence.components.util.MetadataUtils.BATCH_SOURCE_INFO_FILE_PATHS;
import static org.finos.legend.engine.persistence.components.util.MetadataUtils.BATCH_SOURCE_INFO_FILE_PATTERNS;
import static org.finos.legend.engine.persistence.components.util.MetadataUtils.BATCH_SOURCE_INFO_STAGING_FILTERS;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_STAGING_DATASET_ALIAS;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_STAGING_DATASET_QUALIFIER;


public class LogicalPlanUtils
{
    public static final String INFINITE_BATCH_TIME = "9999-12-31 23:59:59";
    public static final String DEFAULT_META_TABLE = "batch_metadata";
    public static final String DEFAULT_SINK_CLEAN_UP_AUDIT_TABLE = "sink_cleanup_audit";
    public static final String DATA_SPLIT_LOWER_BOUND_PLACEHOLDER = "{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}";
    public static final String DATA_SPLIT_UPPER_BOUND_PLACEHOLDER = "{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}";
    public static final String UNDERSCORE = "_";
    public static final String TEMP_DATASET_BASE_NAME = "legend_persistence_temp";
    public static final String TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME = "legend_persistence_tempWithDeleteIndicator";

    private LogicalPlanUtils()
    {
    }

    public static Value INFINITE_BATCH_ID()
    {
        return InfiniteBatchIdValue.builder().build();
    }

    public static DatetimeValue INFINITE_BATCH_TIME()
    {
        return DatetimeValue.of(LogicalPlanUtils.INFINITE_BATCH_TIME);
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

    public static Condition getPartitionColumnValueMatchInCondition(Dataset dataSet, Map<String, Set<String>> partitionFilter)
    {
        return getColumnValueMatchInCondition(dataSet, partitionFilter);
    }

    // (key1 = val11 AND key2 = val21) OR (key1 = val12 AND key2 = val22) OR ...
    public static Condition getPartitionSpecMatchCondition(Dataset dataSet, List<Map<String, Object>> partitionSpecList)
    {
        return Or.of(partitionSpecList.stream()
                 .map(partitionSpec -> And.of(
                         partitionSpec.entrySet().stream()
                                 .map(columnValuePair ->
                                         Equals.of(
                                                 FieldValue.builder().datasetRef(dataSet.datasetReference()).fieldName(columnValuePair.getKey()).build(),
                                                 columnValuePair.getValue() instanceof Number ? ObjectValue.of(columnValuePair.getValue()) : StringValue.of((String) columnValuePair.getValue()))
                                 )
                        .collect(Collectors.toList()))
        ).collect(Collectors.toList()));
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

    public static Condition getDeleteIndicatorIsSetCondition(Dataset stagingDataset, String deleteIndicatorField, List<Object> deleteIndicatorValues)
    {
        Field deleteIndicator = stagingDataset.schema().fields().stream()
            .filter(field -> field.name().equalsIgnoreCase(deleteIndicatorField))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Delete indicator [" + deleteIndicatorField + "] not found in staging dataset"));
        boolean isStringDatatype = DataType.isStringDatatype(deleteIndicator.type().dataType());

        // if there is a single deleteIndicatorValue Use EqualityCondition with String/Object value as needed
        if (deleteIndicatorValues.size() == 1)
        {
            return Equals.of(
                FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(deleteIndicatorField).build(),
                getDeleteIndicatorValue(deleteIndicatorValues.get(0), isStringDatatype));
        }

        // if there are multiple deleteIndicatorValues Use InCondition with String/Object value as needed
        return In.of(
            FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(deleteIndicatorField).build(),
            Array.of(deleteIndicatorValues.stream().map(val -> getDeleteIndicatorValue(val, isStringDatatype)).collect(Collectors.toList())));
    }

    public static Condition getDeleteIndicatorIsNotSetCondition(Dataset stagingDataset, String deleteIndicatorField, List<Object> deleteIndicatorValues)
    {
        Field deleteIndicator = stagingDataset.schema().fields().stream()
            .filter(field -> field.name().equalsIgnoreCase(deleteIndicatorField))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Delete indicator [\" + deleteIndicatorField + \"] not found in staging dataset"));
        boolean isStringDatatype = DataType.isStringDatatype(deleteIndicator.type().dataType());

        // if there is a single deleteIndicatorValue Use NonEqualityCondition with String/Object value as needed
        if (deleteIndicatorValues.size() == 1)
        {
            return NotEquals.of(
                FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(deleteIndicatorField).build(),
                getDeleteIndicatorValue(deleteIndicatorValues.get(0), isStringDatatype));
        }

        // if there is multiple deleteIndicatorValues Use NotInCondition with String/Object value as needed
        return NotIn.of(
            FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(deleteIndicatorField).build(),
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

    public static void replaceField(List<Value> fieldsList, String oldFieldName, String newFieldName)
    {
        fieldsList.forEach(field ->
        {
            if (field instanceof FieldValue && ((FieldValue) field).fieldName().equals(oldFieldName))
            {
                fieldsList.set(fieldsList.indexOf(field), ((FieldValue) field).withFieldName(newFieldName));
            }
        });
    }

    public static Condition getDatasetFilterCondition(DerivedDataset derivedDataset)
    {
        List<DatasetFilter> datasetFilters = derivedDataset.datasetFilters();
        List<Condition> conditions = new ArrayList<>();
        for (DatasetFilter datasetFilter: datasetFilters)
        {
            conditions.add(datasetFilter.mapFilterToCondition(derivedDataset.datasetReference()));
        }
        return And.of(conditions);
    }

    public static Map<String, Object> jsonifyStagingFilters(List<DatasetFilter> filters)
    {
        Map<String, Object> batchSourceInfoMap = new HashMap<>();

        Map<String, Map<String, Object>> stagingFiltersMap = new HashMap<>();
        for (DatasetFilter filter : filters)
        {
            String key = filter.fieldName();
            Object value = filter.getValue();
            String filterType = filter.filterType().getType();
            Map<String, Object> mapValue = stagingFiltersMap.getOrDefault(key, new HashMap<>());
            mapValue.put(filterType, value);
            stagingFiltersMap.put(key, mapValue);
        }
        batchSourceInfoMap.put(BATCH_SOURCE_INFO_STAGING_FILTERS, stagingFiltersMap);

        return batchSourceInfoMap;
    }

    public static Map<String, Object> jsonifyBulkLoadSourceInfo(StagedFilesDatasetProperties stagedFilesDatasetProperties)
    {
        Map<String, Object> batchSourceInfoMap = new HashMap<>();

        List<String> filePaths = stagedFilesDatasetProperties.filePaths();
        List<String> filePatterns = stagedFilesDatasetProperties.filePatterns();
        if (filePaths != null && !filePaths.isEmpty())
        {
            batchSourceInfoMap.put(BATCH_SOURCE_INFO_FILE_PATHS, filePaths);
        }
        if (filePatterns != null && !filePatterns.isEmpty())
        {
            batchSourceInfoMap.put(BATCH_SOURCE_INFO_FILE_PATTERNS, filePatterns);
        }

        return batchSourceInfoMap;
    }

    public static Optional<StringValue> getStringValueFromMap(Map<String, Object> map)
    {
        if (!map.isEmpty())
        {
            ObjectMapper objectMapper = new ObjectMapper();
            try
            {
                return Optional.of(StringValue.of(objectMapper.writeValueAsString(map)));
            }
            catch (JsonProcessingException e)
            {
                throw new JsonReadOrWriteException(e.getMessage(), e);
            }
        }

        return Optional.empty();
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
                .source(dataset)
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

    public static List<Condition> getOptimizationFilterConditions(Dataset dataset, List<OptimizationFilter> optimizationFilters)
    {
        List<Condition> optimizationConditions = new ArrayList<>();
        for (OptimizationFilter filter: optimizationFilters)
        {
            Condition lowerBoundCondition =
                GreaterThanEqualTo.of(
                    FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(filter.fieldName()).build(),
                    StringValue.of(filter.lowerBoundPattern()));
            Condition upperBoundCondition =
                LessThanEqualTo.of(
                    FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(filter.fieldName()).build(),
                    StringValue.of(filter.upperBoundPattern()));
            Condition optimizationCondition = And.builder().addConditions(lowerBoundCondition, upperBoundCondition).build();
            if (filter.includesNullValues())
            {
                Condition nullCondition = IsNull.of(FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(filter.fieldName()).build());
                optimizationCondition = Or.builder().addConditions(optimizationCondition, nullCondition).build();
            }
            optimizationConditions.add(optimizationCondition);
        }
        return optimizationConditions;
    }

    public static List<Field> findCommonPrimaryFieldsBetweenMainAndStaging(Dataset mainDataset, Dataset stagingDataset)
    {
        Set<String> primaryKeysFromMain = mainDataset.schema().fields().stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        return stagingDataset.schema().fields().stream().filter(field -> field.primaryKey() && primaryKeysFromMain.contains(field.name())).collect(Collectors.toList());
    }

    public static List<Value> extractStagedFilesFieldValues(Dataset dataset)
    {
        List<Value> stagedFilesFields = new ArrayList<>();
        boolean columnNumbersPresent = dataset.schema().fields().stream().allMatch(field -> field.columnNumber().isPresent());
        int iter = 1;
        for (Field field : dataset.schema().fields())
        {
            stagedFilesFields.add(getStagedFilesFieldValueWithType(dataset, field, field.type(), columnNumbersPresent, iter++));
        }
        return stagedFilesFields;
    }

    public static List<Value> extractStagedFilesFieldValuesWithVarCharType(Dataset dataset)
    {
        List<Value> stagedFilesFields = new ArrayList<>();
        boolean columnNumbersPresent = dataset.schema().fields().stream().allMatch(field -> field.columnNumber().isPresent());
        int iter = 1;
        for (Field field : dataset.schema().fields())
        {
            stagedFilesFields.add(getStagedFilesFieldValueWithType(dataset, field, FieldType.builder().dataType(VARCHAR).build(), columnNumbersPresent, iter++));
        }
        return stagedFilesFields;
    }

    public static StagedFilesFieldValue getStagedFilesFieldValueWithType(Dataset dataset, Field field, FieldType fieldType, boolean columnNumbersPresent, int counter)
    {
        return StagedFilesFieldValue.builder()
            .columnNumber(columnNumbersPresent ? field.columnNumber().get() : counter)
            .datasetRefAlias(dataset.datasetReference().alias())
            .alias(field.fieldAlias().isPresent() ? field.fieldAlias().get() : field.name())
            .elementPath(field.elementPath())
            .fieldType(fieldType)
            .fieldName(field.name())
            .build();
    }

    public static Dataset getTempDataset(Datasets datasets)
    {
        String mainDatasetName = datasets.mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new));
        return datasets.tempDataset().orElse(DatasetDefinition.builder()
                .schema(datasets.mainDataset().schema())
                .database(datasets.mainDataset().datasetReference().database())
                .group(datasets.mainDataset().datasetReference().group())
                .name(mainDatasetName + UNDERSCORE + TEMP_DATASET_BASE_NAME)
                .alias(TEMP_DATASET_BASE_NAME)
                .build());
    }

    public static Dataset getTempDatasetWithDeleteIndicator(Datasets datasets, String deleteIndicatorField)
    {
        if (datasets.tempDatasetWithDeleteIndicator().isPresent())
        {
            return datasets.tempDatasetWithDeleteIndicator().get();
        }
        else
        {
            String mainDatasetName = datasets.mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new));
            Field deleteIndicator = Field.builder().name(deleteIndicatorField).type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();
            List<Field> mainFieldsPlusDeleteIndicator = new ArrayList<>(datasets.mainDataset().schema().fields());
            mainFieldsPlusDeleteIndicator.add(deleteIndicator);
            return DatasetDefinition.builder()
                    .schema(datasets.mainDataset().schema().withFields(mainFieldsPlusDeleteIndicator))
                    .database(datasets.mainDataset().datasetReference().database())
                    .group(datasets.mainDataset().datasetReference().group())
                    .name(mainDatasetName + UNDERSCORE + TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME)
                    .alias(TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME)
                    .build();
        }
    }

    public static Dataset getTempStagingDatasetDefinition(Dataset stagingDataset, IngestMode ingestMode, String ingestRunId)
    {
        String alias = stagingDataset.datasetReference().alias().orElse(TEMP_STAGING_DATASET_ALIAS);
        String datasetName = TableNameGenUtils.generateTableName(stagingDataset.datasetReference().name().orElseThrow(IllegalStateException::new), TEMP_STAGING_DATASET_QUALIFIER, ingestRunId);
        SchemaDefinition tempStagingSchema = ingestMode.versioningStrategy().accept(new DeriveTempStagingSchemaDefinition(stagingDataset.schema(), ingestMode.deduplicationStrategy()));
        return DatasetDefinition.builder()
                .schema(tempStagingSchema)
                .database(stagingDataset.datasetReference().database())
                .group(stagingDataset.datasetReference().group())
                .name(datasetName)
                .alias(alias)
                .build();
    }

    public static Dataset getTempStagingDatasetWithoutPks(Dataset tempStagingDataset)
    {
        List<Field> fieldsWithoutPk = tempStagingDataset.schema().fields().stream()
                .map(field -> field.withPrimaryKey(false)).collect(Collectors.toList());
        return tempStagingDataset.withSchema(tempStagingDataset.schema().withFields(fieldsWithoutPk));
    }

    public static Dataset getDedupedAndVersionedDataset(DeduplicationStrategy deduplicationStrategy, VersioningStrategy versioningStrategy, Dataset stagingDataset, List<String> primaryKeys)
    {
        Dataset dedupedDataset = deduplicationStrategy.accept(new DatasetDeduplicationHandler(stagingDataset));
        boolean isTempTableNeededForVersioning = versioningStrategy.accept(VersioningVisitors.IS_TEMP_TABLE_NEEDED);
        if (isTempTableNeededForVersioning && dedupedDataset instanceof Selection)
        {
            Selection selection = (Selection) dedupedDataset;
            dedupedDataset = selection.withAlias(stagingDataset.datasetReference().alias());
        }
        Dataset versionedDataset = versioningStrategy.accept(new DatasetVersioningHandler(dedupedDataset, primaryKeys));
        return versionedDataset;
    }

    public static boolean isTempTableNeededForStaging(IngestMode ingestMode)
    {
        boolean isTempTableNeededForVersioning = ingestMode.versioningStrategy().accept(VersioningVisitors.IS_TEMP_TABLE_NEEDED);
        boolean isTempTableNeededForDedup = ingestMode.deduplicationStrategy().accept(DeduplicationVisitors.IS_TEMP_TABLE_NEEDED);
        return isTempTableNeededForVersioning || isTempTableNeededForDedup;
    }

    public static Set<DataType> SUPPORTED_DATA_TYPES_FOR_OPTIMIZATION_COLUMNS =
            new HashSet<>(Arrays.asList(INT, INTEGER, BIGINT, FLOAT, DOUBLE, DECIMAL, DATE));

    public static Set<DataType> SUPPORTED_DATA_TYPES_FOR_VERSIONING_COLUMNS = DataType.getComparableDataTypes();
}
