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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TestUtils
{

    public static String testSchemaName = "TEST";
    public static String testDatabaseName = "TEST_DB";
    public static String mainTableName = "main";
    public static String stagingTableName = "staging";
    public static String tempTableName = "temp";
    public static String tempWithDeleteIndicatorTableName = "tempWithDeleteIndicator";
    public static String stagingTableWithoutDuplicatesName = "stagingWihtoutDuplicates";

    // Sample table 1
    public static String idName = "id";
    public static String nameName = "name";
    public static String incomeName = "income";
    public static String startTimeName = "start_time";
    public static String expiryDateName = "expiry_date";

    // Sample table 2
    public static String dateName = "date";
    public static String entityName = "entity";
    public static String priceName = "price";
    public static String volumeName = "volume";

    // Sample table 3
    public static String key1Name = "key1";
    public static String key2Name = "key2";
    public static String valueName = "value1";
    public static String dateInName = "date_in";
    public static String dateOutName = "date_out";

    // Sample table 4
    public static String indexName = "index";
    public static String dateTimeName = "datetime";
    public static String balanceName = "balance";

    // Special columns
    public static String digestName = "digest";
    public static String digestUDF = "LAKEHOUSE_MD5";
    public static String versionName = "version";
    public static String batchUpdateTimeName = "batch_update_time";
    public static String batchIdName = "batch_id";
    public static String batchIdInName = "batch_id_in";
    public static String batchIdOutName = "batch_id_out";
    public static String batchTimeInName = "batch_time_in";
    public static String batchTimeOutName = "batch_time_out";
    public static String deleteIndicatorName = "delete_indicator";
    public static String[] deleteIndicatorValues = new String[]{"yes", "1", "true"};
    public static String[] deleteIndicatorValuesEdgeCase = new String[]{"0"};
    public static String alterColumnName = "alter_column";
    public static String fromName = "from";
    public static String throughName = "through";
    public static String startDateTimeName = "start_datetime";
    public static String endDateTimeName = "end_datetime";
    public static String dataSplitName = "data_split";
    public static String batchName = "batch";
    public static String ratingName = "rating";
    public static String accountNumName = "accountNum";
    public static String dimensionName = "dimension";
    public static String COMMA_DELIMITER = ",";

    public static HashMap<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put(dateName, new HashSet<>(Arrays.asList("2021-12-01", "2021-12-02")));
    }};

    public static Field id = Field.builder().name(idName).type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(idName).build();
    public static Field tinyIntId = Field.builder().name(idName).type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(idName).build();
    public static Field name = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).fieldAlias(nameName).build();
    public static Field nullableName = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 64, null)).fieldAlias(nameName).build();
    public static Field nameWithMoreLength = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 256, null)).nullable(false).fieldAlias(nameName).build();
    public static Field income = Field.builder().name(incomeName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(incomeName).build();
    public static Field notNullableIntIncome = Field.builder().name(incomeName).type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).nullable(false).fieldAlias(incomeName).build();
    public static Field nullableIntIncome = Field.builder().name(incomeName).type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).fieldAlias(incomeName).build();
    public static Field decimalIncome = Field.builder().name(incomeName).type(FieldType.of(DataType.DECIMAL, 10, 2)).fieldAlias(incomeName).build();
    public static Field startTime = Field.builder().name(startTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(startTimeName).build();
    public static Field startTimeTimestamp = Field.builder().name(startTimeName).type(FieldType.of(DataType.TIMESTAMP, 6, null)).primaryKey(true).fieldAlias(startTimeName).build();
    public static Field expiryDate = Field.builder().name(expiryDateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).fieldAlias(expiryDateName).build();
    public static Field expiryDatePk = Field.builder().name(expiryDateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(expiryDateName).build();
    public static Field date = Field.builder().name(dateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dateName).build();
    public static Field entity = Field.builder().name(entityName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(entityName).build();
    public static Field price = Field.builder().name(priceName).type(FieldType.of(DataType.DECIMAL, 20, 2)).fieldAlias(priceName).build();
    public static Field volume = Field.builder().name(volumeName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(volumeName).build();
    public static Field key1 = Field.builder().name(key1Name).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(key1Name).build();
    public static Field key2 = Field.builder().name(key2Name).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(key2Name).build();
    public static Field value = Field.builder().name(valueName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(valueName).build();
    public static Field dateIn = Field.builder().name(dateInName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dateInName).build();
    public static Field dateOut = Field.builder().name(dateOutName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(dateOutName).build();
    public static Field digest = Field.builder().name(digestName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(digestName).build();
    public static Field digestWithLength = Field.builder().name(digestName).type(FieldType.of(DataType.VARCHAR, 1000000000, null)).fieldAlias(digestName).build();
    public static Field version = Field.builder().name(versionName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(versionName).build();
    public static Field versionPk = Field.builder().name(versionName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(versionName).primaryKey(true).build();
    public static Field batchUpdateTimestamp = Field.builder().name(batchUpdateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    public static Field batchId = Field.builder().name(batchIdName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(batchIdName).build();
    public static Field batchIdIn = Field.builder().name(batchIdInName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(batchIdInName).build();
    public static Field batchIdOut = Field.builder().name(batchIdOutName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(batchIdOutName).build();
    public static Field batchTimeIn = Field.builder().name(batchTimeInName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(batchTimeInName).build();
    public static Field batchTimeOut = Field.builder().name(batchTimeOutName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(batchTimeOutName).build();
    public static Field deleteIndicator = Field.builder().name(deleteIndicatorName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(deleteIndicatorName).build();
    public static Field booleanDeleteIndicator = Field.builder().name(deleteIndicatorName).type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).fieldAlias(deleteIndicatorName).build();
    public static Field alterColumn = Field.builder().name(alterColumnName).type(FieldType.of(DataType.VARCHAR, 64, null)).fieldAlias(alterColumnName).build();
    public static Field incomeChanged = Field.builder().name(incomeName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(incomeName).build();
    public static Field from = Field.builder().name(fromName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(fromName).primaryKey(true).build();
    public static Field through = Field.builder().name(throughName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(throughName).build();
    public static Field index = Field.builder().name(indexName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(indexName).build();
    public static Field dateTime = Field.builder().name(dateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(dateTimeName).primaryKey(true).build();
    public static Field balance = Field.builder().name(balanceName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(balanceName).build();
    public static Field startDateTime = Field.builder().name(startDateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(startDateTimeName).primaryKey(true).build();
    public static Field endDateTime = Field.builder().name(endDateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(endDateTimeName).build();
    public static Field dataSplit = Field.builder().name(dataSplitName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dataSplitName).build();
    public static Field batch = Field.builder().name(batchName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(batchName).primaryKey(true).build();
    public static Field rating = Field.builder().name(ratingName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(ratingName).build();
    public static Field accountNum = Field.builder().name(accountNumName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(accountNumName).primaryKey(true).build();
    public static Field dimension = Field.builder().name(dimensionName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(dimensionName).primaryKey(true).build();

    public static DatasetDefinition getBasicMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchId)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getDefaultMainTable()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(mainTableName)
                .schema(SchemaDefinition.builder().build())
                .build();
    }

    public static DatasetDefinition getBasicMainTableWithVersion()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(version)
                .addFields(batchId)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getMainTableWithBatchUpdateTimeField()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchUpdateTimestamp)
                .addFields(batchId)
                .build())
            .build();
    }

    public static SchemaDefinition getStagingSchema()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(digest)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithExpiryDatePk()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDatePk)
            .addFields(digest)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithVersion()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(digest)
            .addFields(versionPk)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithNonPkVersion()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(digest)
            .addFields(version)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithNonPkVersionWithoutDigest()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(version)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithFilterForDB()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(digest)
            .addFields(batch)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithFilterWithVersionForDB()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .addFields(digest)
            .addFields(versionPk)
            .addFields(batch)
            .build();
    }

    public static SchemaDefinition getDedupAndVersioningSchemaWithVersion =
        SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(version)
            .addFields(income)
            .addFields(expiryDate)
            .addFields(digest)
            .build();

    public static SchemaDefinition getDedupAndVersioningSchemaWithVersionAndBatch =
        SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(version)
            .addFields(income)
            .addFields(expiryDate)
            .addFields(digest)
            .addFields(batch)
            .build();

    public static SchemaDefinition getStagingSchemaWithDataSplits()
    {
        return SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(dataSplit)
                .build();
    }

    public static SchemaDefinition getStagingSchemaWithoutDigest()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(expiryDate)
            .build();
    }

    public static SchemaDefinition getStagingSchemaWithLessColumnThanMain()
    {
        return SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTime)
            .addFields(digest)
            .build();
    }

    public static SchemaDefinition getSchemaWithNoPKs()
    {
        return SchemaDefinition.builder()
            .addFields(name)
            .addFields(income)
            .addFields(expiryDate)
            .build();
    }

    public static DatasetDefinition getBasicStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchema())
            .build();
    }

    public static DatasetDefinition getStagingTableWithNoPks()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getSchemaWithNoPKs())
            .build();
    }

    public static FilteredDataset getFilteredStagingTableWithComplexFilter()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getSchemaWithNoPKs())
            .alias(stagingTableName)
            .filter(And.builder()
                .addConditions(GreaterThan.of(FieldValue.builder().fieldName(incomeName).datasetRefAlias(stagingTableName).build(), NumericalValue.of(1000L)))
                .addConditions(Or.builder()
                    .addConditions(GreaterThanEqualTo.of(FieldValue.builder().fieldName(expiryDateName).datasetRefAlias(stagingTableName).build(), StringValue.of("2022-12-03")))
                    .addConditions(LessThanEqualTo.of(FieldValue.builder().fieldName(expiryDateName).datasetRefAlias(stagingTableName).build(), StringValue.of("2022-12-01")))
                    .build())
                .build())
            .build();
    }

    public static DatasetDefinition getBasicStagingTableWithExpiryDatePk()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithExpiryDatePk())
            .build();
    }

    public static DatasetDefinition getStagingTableWithVersion()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithVersion())
            .build();
    }

    public static DatasetDefinition getStagingTableWithNonPkVersion()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithNonPkVersion())
            .build();
    }

    public static DatasetDefinition getStagingTableWithNonPkVersionWithoutDigest()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithNonPkVersionWithoutDigest())
            .build();
    }

    public static DatasetDefinition getStagingTableWithFilterForDB()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithFilterForDB())
            .build();
    }

    public static DatasetDefinition getStagingTableWithFilterWithVersionForDB()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithFilterWithVersionForDB())
            .build();
    }

    public static DerivedDataset getDerivedStagingTableWithFilter()
    {
        return DerivedDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchema())
            .alias(stagingTableName)
            .addDatasetFilters(DatasetFilter.of(batchName, FilterType.GREATER_THAN_EQUAL, 2))
            .build();
    }

    public static FilteredDataset getFilteredStagingTable()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchema())
            .alias(stagingTableName)
            .filter(Equals.of(FieldValue.builder()
                .fieldName(batchName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(1L)))
            .build();
    }

    public static DerivedDataset getStagingTableWithFilterSecondPass()
    {
        return DerivedDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchema())
            .alias(stagingTableName)
            .addDatasetFilters(DatasetFilter.of(batchName, FilterType.GREATER_THAN_EQUAL, 3))
            .addDatasetFilters(DatasetFilter.of(batchName, FilterType.LESS_THAN_EQUAL, 5))
            .build();
    }

    public static FilteredDataset getFilteredStagingTableSecondPass()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchema())
            .alias(stagingTableName)
            .filter(GreaterThan.of(FieldValue.builder()
                .fieldName(batchName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(1L)))
            .build();
    }

    public static DerivedDataset getDerivedStagingTableWithFilterWithVersion()
    {
        return DerivedDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithVersion())
            .alias(stagingTableName)
            .addDatasetFilters(DatasetFilter.of(batchName, FilterType.GREATER_THAN_EQUAL, 2))
            .build();
    }

    public static FilteredDataset getFilteredStagingTableWithVersion()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithVersion())
            .alias(stagingTableName)
            .filter(GreaterThanEqualTo.of(FieldValue.builder()
                .fieldName(batchName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(2L)))
            .build();
    }

    public static DerivedDataset getStagingTableWithFilterWithVersionSecondPass()
    {
        return DerivedDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithVersion())
            .alias(stagingTableName)
            .addDatasetFilters(DatasetFilter.of(batchName, FilterType.GREATER_THAN_EQUAL, 3))
            .build();
    }

    public static FilteredDataset getFilteredStagingTableWithVersionSecondPass()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithVersion())
            .alias(stagingTableName)
            .filter(GreaterThanEqualTo.of(FieldValue.builder()
                .fieldName(batchName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(3L)))
            .build();
    }

    public static JsonExternalDatasetReference getBasicJsonDatasetReferenceTable(String dataPath)
    {
        return JsonExternalDatasetReference.builder()
            .schema(getStagingSchema())
            .data(readFile(dataPath))
            .build();
    }

    public static JsonExternalDatasetReference getJsonDatasetWithoutDigest(String dataPath, String database, String group, String name)
    {
        return JsonExternalDatasetReference.builder()
            .database(database)
            .group(group)
            .name(name)
            .schema(getStagingSchemaWithoutDigest())
            .data(readFile(dataPath))
            .build();
    }

    public static JsonExternalDatasetReference getJsonDatasetWithDigest(String dataPath, String database, String name, String group, String alias)
    {
        return JsonExternalDatasetReference.builder()
            .database(database)
            .name(name)
            .group(group)
            .alias(alias)
            .schema(getStagingSchema())
            .data(readFile(dataPath))
            .build();
    }

    public static JsonExternalDatasetReference getJsonDatasetWithoutDigestReferenceTable(String dataPath)
    {
        return JsonExternalDatasetReference.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithoutDigest())
            .data(readFile(dataPath))
            .build();
    }

    public static CsvExternalDatasetReference getBasicCsvDatasetReferenceTable(String dataPath)
    {
        return CsvExternalDatasetReference.builder()
            .schema(getStagingSchema())
            .csvDataPath(dataPath)
            .build();
    }

    public static CsvExternalDatasetReference getBasicCsvDatasetReferenceTableWithDataSplits(String dataPath)
    {
        return CsvExternalDatasetReference.builder()
                .schema(getStagingSchemaWithDataSplits())
                .csvDataPath(dataPath)
                .build();
    }

    public static CsvExternalDatasetReference getCsvDatasetReferenceTable(String dataPath, String database, String name, String group, String alias)
    {
        return CsvExternalDatasetReference.builder()
            .database(database)
            .name(name)
            .group(group)
            .alias(alias)
            .schema(getStagingSchema())
            .csvDataPath(dataPath)
            .build();
    }

    public static DatasetDefinition getDatasetWithLessColumnsThanMain()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getStagingSchemaWithLessColumnThanMain())
            .build();
    }

    public static Dataset getCsvDatasetRefWithLessColumnsThanMainForBitemp(String dataPath)
    {
        return CsvExternalDatasetReference.builder()
            .schema(getBitemporalStagingSchemaWithLessColumnThanMain())
            .csvDataPath(dataPath)
            .build();
    }

    public static DatasetDefinition getUnitemporalMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getUnitemporalMainTableWithVersion()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(version)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getUnitemporalIdBasedMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getUnitemporalTimeBasedMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getStagingTableWithDeleteIndicator()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(digest)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getEntityPriceMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getEntityPriceIdBasedMainTable()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
        return mainTable;
    }

    public static DatasetDefinition getEntityPriceTimeBasedMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getEntityPriceStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getEntityPriceWithVersionStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(version)
                .build()
            )
            .build();
    }

    public static FilteredDataset getEntityPriceWithVersionFilteredStagingTable()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .alias(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(version)
                .build()
            )
            .filter(GreaterThanEqualTo.of(FieldValue.builder()
                .fieldName(volumeName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(100L)))
            .build();
    }

    public static FilteredDataset getEntityPriceWithVersionFilteredStagingTableSecondPass()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .alias(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(entity)
                .addFields(price)
                .addFields(volume)
                .addFields(digest)
                .addFields(version)
                .build()
            )
            .filter(GreaterThanEqualTo.of(FieldValue.builder()
                .fieldName(volumeName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(500L)))
            .build();
    }

    public static DatasetDefinition getBitemporalMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(key1)
                .addFields(key2)
                .addFields(value)
                .addFields(from)
                .addFields(through)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromTimeOnlyMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(key1)
                .addFields(key2)
                .addFields(value)
                .addFields(dateIn)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(from)
                .addFields(through)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(key1)
                .addFields(key2)
                .addFields(value)
                .addFields(dateIn)
                .addFields(dateOut)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalStagingTableWithDeleteIndicator()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(key1)
                .addFields(key2)
                .addFields(value)
                .addFields(dateIn)
                .addFields(dateOut)
                .addFields(digest)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromTimeOnlyStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(key1)
                .addFields(key2)
                .addFields(value)
                .addFields(dateIn)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static SchemaDefinition getBitemporalStagingSchemaWithLessColumnThanMain()
    {
        return SchemaDefinition.builder()
            .addFields(key1)
            .addFields(key2)
            .addFields(dateIn)
            .addFields(dateOut)
            .addFields(digest)
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyMainTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(balance)
                .addFields(digest)
                .addFields(startDateTime)
                .addFields(endDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyMainTableWithVersionIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(balance)
                .addFields(digest)
                .addFields(version)
                .addFields(startDateTime)
                .addFields(endDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyTempTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(tempTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(balance)
                .addFields(digest)
                .addFields(startDateTime)
                .addFields(endDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyTempTableWithVersionIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(tempTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(balance)
                .addFields(digest)
                .addFields(version)
                .addFields(startDateTime)
                .addFields(endDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyTempTableWithDeleteIndicatorIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(tempWithDeleteIndicatorTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(balance)
                .addFields(digest)
                .addFields(startDateTime)
                .addFields(endDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static FilteredDataset getBitemporalFromOnlyFilteredStagingTableIdBased()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .build()
            )
            .alias(stagingTableName)
            .filter(LessThanEqualTo.of(FieldValue.builder()
                .fieldName(balanceName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(3L)))
            .build();
    }

    public static FilteredDataset getBitemporalFromOnlyFilteredStagingTableIdBasedSecondPass()
    {
        return FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .build()
            )
            .alias(stagingTableName)
            .filter(LessThanEqualTo.of(FieldValue.builder()
                .fieldName(balanceName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(20L)))
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableWithoutDuplicatesIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableWithoutDuplicatesName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableWithVersionWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .addFields(version)
                .addFields(dataSplit)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableWithDeleteIndicatorIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableWithDeleteIndicatorWithVersionWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .addFields(version)
                .addFields(deleteIndicator)
                .addFields(dataSplit)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getBitemporalFromOnlyStagingTableWithoutDuplicatesWithDeleteIndicatorWithVersionWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableWithoutDuplicatesName)
            .schema(SchemaDefinition.builder()
                .addFields(index)
                .addFields(dateTime)
                .addFields(balance)
                .addFields(digest)
                .addFields(version)
                .addFields(deleteIndicator)
                .addFields(dataSplit)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionAddColumnMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchUpdateTimestamp)
                .addFields(batchId)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionAddColumnMainTableUpperCase()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName.toUpperCase())
            .name(mainTableName.toUpperCase())
            .schema(SchemaDefinition.builder()
                .addFields(id.withName(idName.toUpperCase()))
                .addFields(name.withName(nameName.toUpperCase()))
                .addFields(startTime.withName(startTimeName.toUpperCase()))
                .addFields(expiryDate.withName(expiryDateName.toUpperCase()))
                .addFields(digest.withName(digestName.toUpperCase()))
                .addFields(batchUpdateTimestamp.withName(batchUpdateTimeName.toUpperCase()))
                .addFields(batchId.withName(batchIdName.toUpperCase()))
                .build())
            .build();
    }

    public static DatasetDefinition expectedMainTableSchema()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(mainTableName)
                .schema(SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(income)
            .addFields(startTimeTimestamp)
            .addFields(expiryDate)
            .addFields(digestWithLength)
            .build())
                .build();
    }

    public static DatasetDefinition expectedMainTableSchemaWithLengthEvolution()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(mainTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(id)
                        .addFields(nameWithMoreLength)
                        .addFields(income)
                        .addFields(startTimeTimestamp)
                        .addFields(expiryDate)
                        .addFields(digestWithLength)
                        .build())
                .build();
    }

    public static DatasetDefinition expectedMainTableSchemaWithDatatypeChange()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(mainTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(id)
                        .addFields(name)
                        .addFields(decimalIncome)
                        .addFields(startTimeTimestamp)
                        .addFields(expiryDate)
                        .addFields(digestWithLength)
                        .build())
                .build();
    }

    public static DatasetDefinition getSchemaEvolutionDataTypeConversionMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(nullableIntIncome)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchUpdateTimestamp)
                .addFields(batchId)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionDataTypeSizeChangeStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(nameWithMoreLength)
                .addFields(nullableIntIncome)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionColumnNullabilityChangeStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(nullableName)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionDataTypeConversionAndColumnNullabilityChangeMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(notNullableIntIncome)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchUpdateTimestamp)
                .addFields(batchId)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionDataTypeConversionAndDataTypeSizeChangeStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(decimalIncome)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionMakeMainColumnNullableStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build())
            .build();
    }

    public static DatasetDefinition getSchemaEvolutionPKTypeDifferentMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(expiryDate)
                .addFields(digest)
                .addFields(batchUpdateTimestamp)
                .build())
            .build();
    }

    public static MetadataDataset getMetadataDataset()
    {
        return MetadataDataset.builder().build();
    }

    public static void assertFileAndTableDataEquals(String[] csvSchema, String csvPath, List<Map<String, Object>> dataFromTable) throws IOException
    {
        List<String[]> lines = readCsvData(csvPath);
        Assertions.assertEquals(lines.size(), dataFromTable.size());

        for (int i = 0; i < lines.size(); i++)
        {
            Map<String, Object> tableRow = dataFromTable.get(i);
            String[] expectedLine = lines.get(i);
            for (int j = 0; j < csvSchema.length; j++)
            {
                String expected = expectedLine[j];
                String tableData = String.valueOf(tableRow.get(csvSchema[j]));
                Assertions.assertEquals(expected, tableData);
            }
        }
    }

    // This is to check the Dataset objects - whether everything has been updated properly
    public static void assertUpdatedDataset(Dataset expectedDataset, Dataset actualDataset)
    {
        Set<Field> actualFieldsSet = actualDataset.schema().fields().stream().collect(Collectors.toSet());
        Set<Field> expectedFieldsSet = expectedDataset.schema().fields().stream().collect(Collectors.toSet());
        expectedFieldsSet.forEach(
                field ->
                {
                    Field matchedMainField = actualFieldsSet.stream().filter(mainField -> mainField.name().equals(field.name())).findFirst().orElse(null);
                    if (matchedMainField == null || field.nullable() != matchedMainField.nullable() || field.primaryKey() != matchedMainField.primaryKey() || !field.type().equals(matchedMainField.type()))
                    {
                        Assertions.fail("Updated dataset object does not match that of the expected dataset");
                    }
                });
    }

    // This is to check the actual database table - whether columns have been added properly
    public static void assertTableColumnsEquals(List<String> expectedSchema, List<Map<String, Object>> actualData)
    {
        for (Map<String, Object> actualTableRow : actualData)
        {
            for (String actualColumn : actualTableRow.keySet())
            {
                if (!expectedSchema.contains(actualColumn))
                {
                    Assertions.fail("Unexpected column found");
                }
            }
            for (String expectedColumn : expectedSchema)
            {
                if (!actualTableRow.containsKey(expectedColumn))
                {
                    Assertions.fail("Expected column not found");
                }
            }
        }
    }

    // This is to check the actual database table - what are the columns of a table
    public static List<String> getColumnsFromTable(Connection connection, String databaseName, String schemaName, String tableName) throws SQLException
    {
        DatabaseMetaData dbMetaData = connection.getMetaData();
        ResultSet columnResult = dbMetaData.getColumns(databaseName, schemaName, tableName, null);
        List<String> columnNames = new ArrayList<>();
        while (columnResult.next())
        {
            columnNames.add(columnResult.getString(RelationalExecutionHelper.COLUMN_NAME));
        }
        return columnNames;
    }

    // This is to check the actual database table - whether columns have the right nullability
    public static String getIsColumnNullableFromTable(RelationalExecutionHelper sink, String tableName, String columnName)
    {
        List<Map<String, Object>> result = sink.executeQuery("SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableName + "' and COLUMN_NAME ='" + columnName + "'");
        return result.get(0).get("IS_NULLABLE").toString();
    }

    // This is to check the actual database table - the length (precision) of the column data type
    public static int getColumnDataTypeLengthFromTable(RelationalExecutionHelper sink, String tableName, String columnName)
    {
        List<Map<String, Object>> result = sink.executeQuery("SELECT NUMERIC_PRECISION, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableName + "' and COLUMN_NAME ='" + columnName + "'");
        Object precisionOrLength = Optional.ofNullable(result.get(0).get("NUMERIC_PRECISION")).orElseGet(() ->
                result.get(0).get("CHARACTER_MAXIMUM_LENGTH")
        );
        return Integer.parseInt(precisionOrLength.toString());
    }

    // This is to check the actual database table - the scale of the column data type
    public static int getColumnDataTypeScaleFromTable(RelationalExecutionHelper sink, String tableName, String columnName)
    {
        List<Map<String, Object>> result = sink.executeQuery("SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableName + "' and COLUMN_NAME ='" + columnName + "'");
        return Integer.parseInt(result.get(0).get("NUMERIC_SCALE").toString());
    }

    // This is to check the actual database table - whether data types are correct
    public static String getColumnDataTypeFromTable(Connection connection, String database, String schema, String tableName, String columnName) throws SQLException
    {
        ResultSet result = connection.getMetaData().getColumns(database, schema, tableName, columnName);
        String dataType = "";
        if (result.next())
        {
            dataType = JDBCType.valueOf(result.getInt("DATA_TYPE")).name();
        }
        return dataType;
    }

    public static Dataset createDatasetWithUpdatedField(Dataset dataset, Field field)
    {
        List<Field> newFields = dataset.schema().fields()
            .stream()
            .filter(f -> f.name() != field.name())
            .collect(Collectors.toList());

        newFields.add(field);

        return dataset.withSchema(dataset.schema().withFields(newFields));
    }

    public static void assertEquals(List<Map<String, Object>> expectedList, List<Map<String, Object>> actualList)
    {
        if (expectedList.size() != actualList.size())
        {
            Assertions.fail("Size of expected List does not match actual List");
        }

        for (int i = 0; i < actualList.size(); i++)
        {
            Map<String, Object> expected = expectedList.get(i);
            Map<String, Object> actual = actualList.get(i);
            for (Map.Entry entry : expected.entrySet())
            {
                Object actualObj = actual.get(entry.getKey());
                Object expectedObj = entry.getValue();
                if (expectedObj == null && actualObj != null)
                {
                    Assertions.fail(String.format("Values mismatch. key: %s, actual value: %s, expected value: %s", entry.getKey(), actualObj, expectedObj));
                }
                if (expectedObj != null && !expectedObj.toString().equals(actualObj.toString()))
                {
                    Assertions.fail(String.format("Values mismatch. key: %s, actual value: %s, expected value: %s", entry.getKey(), actualObj, expectedObj));
                }

            }
        }
    }

    private static List<String[]> readCsvData(String path) throws IOException
    {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String[] values = line.split(COMMA_DELIMITER);
                lines.add(values);
            }
        }
        return lines;
    }

    private static String readFile(String dataPath)
    {
        String fileContent = "";
        try
        {
            byte[] bytes = Files.readAllBytes(Paths.get(dataPath));
            fileContent = new String(bytes);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return fileContent;
    }
}
