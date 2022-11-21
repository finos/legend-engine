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

package org.finos.legend.engine.persistence.components;

import com.opencsv.CSVReader;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

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
    // Adapted from https://confluence.site.gs.com/display/DIO/Alloy+Streaming+-+Milestoning+Schemes#AlloyStreamingMilestoningSchemes-Withpartitioning
    public static String dateName = "date";
    public static String tickerName = "ticker";
    public static String closePriceName = "close_price";
    public static String volumeName = "volume";

    // Sample table 3
    // Adapted from https://confluence.site.gs.com/display/EP/Data+Lake%3A+Ingesting+Data%3A+Milestoning+Examples#DataLake:IngestingData:MilestoningExamples-BITEMPORALINCREMENTAL
    public static String key1Name = "key1";
    public static String key2Name = "key2";
    public static String valueName = "value1";
    public static String dateInName = "date_in";
    public static String dateOutName = "date_out";

    // Sample table 4
    // Adapted from Loans use case
    public static String loanIdName = "gs_loan_id";
    public static String loanDateTimeName = "datetime";
    public static String loanBalanceName = "balance";


    // Special columns
    public static String digestName = "digest";
    public static String batchUpdateTimeName = "batch_update_time";
    public static String batchIdInName = "batch_id_in";
    public static String batchIdOutName = "batch_id_out";
    public static String batchTimeInName = "batch_time_in";
    public static String batchTimeOutName = "batch_time_out";
    public static String deleteIndicatorName = "delete_indicator";
    public static String[] deleteIndicatorValues = new String[]{"yes", "1", "true"};
    public static String[] deleteIndicatorValuesEdgeCase = new String[]{"0"};
    public static String alterColumnName = "alter_column";
    public static String lakeFromName = "lake_from";
    public static String lakeThroughName = "lake_through";
    public static String loanStartDateTimeName = "start_datetime";
    public static String loanEndDateTimeName = "end_datetime";
    public static String dataSplitName = "data_split";

    public static HashMap<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put(dateName, new HashSet<>(Arrays.asList("2021-12-01", "2021-12-02")));
    }};

    public static Field id = Field.builder().name(idName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(idName).build();
    public static Field tinyIntId = Field.builder().name(idName).type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(idName).build();
    public static Field name = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).fieldAlias(nameName).build();
    public static Field nameWithMoreLength = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 256, null)).nullable(false).fieldAlias(nameName).build();
    public static Field income = Field.builder().name(incomeName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(incomeName).build();
    public static Field startTime = Field.builder().name(startTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(startTimeName).build();
    public static Field expiryDate = Field.builder().name(expiryDateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).fieldAlias(expiryDateName).build();
    public static Field date = Field.builder().name(dateName).type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dateName).build();
    public static Field ticker = Field.builder().name(tickerName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(tickerName).build();
    public static Field closePrice = Field.builder().name(closePriceName).type(FieldType.of(DataType.DECIMAL, 20, 2)).fieldAlias(closePriceName).build();
    public static Field volume = Field.builder().name(volumeName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(volumeName).build();
    public static Field key1 = Field.builder().name(key1Name).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(key1Name).build();
    public static Field key2 = Field.builder().name(key2Name).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(key2Name).build();
    public static Field value = Field.builder().name(valueName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(valueName).build();
    public static Field dateIn = Field.builder().name(dateInName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dateInName).build();
    public static Field dateOut = Field.builder().name(dateOutName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(dateOutName).build();
    public static Field digest = Field.builder().name(digestName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(digestName).build();
    public static Field batchUpdateTimestamp = Field.builder().name(batchUpdateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    public static Field batchIdIn = Field.builder().name(batchIdInName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(batchIdInName).build();
    public static Field batchIdOut = Field.builder().name(batchIdOutName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(batchIdOutName).build();
    public static Field batchTimeIn = Field.builder().name(batchTimeInName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(batchTimeInName).build();
    public static Field batchTimeOut = Field.builder().name(batchTimeOutName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(batchTimeOutName).build();
    public static Field deleteIndicator = Field.builder().name(deleteIndicatorName).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).fieldAlias(deleteIndicatorName).build();
    public static Field booleanDeleteIndicator = Field.builder().name(deleteIndicatorName).type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).fieldAlias(deleteIndicatorName).build();
    public static Field alterColumn = Field.builder().name(alterColumnName).type(FieldType.of(DataType.VARCHAR, 64, null)).fieldAlias(alterColumnName).build();
    public static Field incomeChanged = Field.builder().name(incomeName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).fieldAlias(incomeName).build();
    public static Field lakeFrom = Field.builder().name(lakeFromName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(lakeFromName).primaryKey(true).build();
    public static Field lakeThrough = Field.builder().name(lakeThroughName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(lakeThroughName).build();
    public static Field loanId = Field.builder().name(loanIdName).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(loanIdName).build();
    public static Field loanDateTime = Field.builder().name(loanDateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(loanDateTimeName).primaryKey(true).build();
    public static Field loanBalance = Field.builder().name(loanBalanceName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).fieldAlias(loanBalanceName).build();
    public static Field loanStartDateTime = Field.builder().name(loanStartDateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(loanStartDateTimeName).primaryKey(true).build();
    public static Field loanEndDateTime = Field.builder().name(loanEndDateTimeName).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).fieldAlias(loanEndDateTimeName).build();
    public static Field dataSplit = Field.builder().name(dataSplitName).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).primaryKey(true).fieldAlias(dataSplitName).build();

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
                .build()
            )
            .build();
    }

    public static DatasetDefinition getSchemaEvolMainTableWithMissingColumn()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(digest)
                .build())
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

    public static DatasetDefinition getStagingTableForImplicitSchemaEvolution()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(tinyIntId)
                .addFields(nameWithMoreLength)
                .addFields(income)
                .addFields(startTime)
                .addFields(expiryDate)
                .addFields(digest)
                .build())
            .build();
    }

    public static DatasetDefinition getBasicTableWithNoPks()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(getSchemaWithNoPKs())
            .build();
    }

    public static Dataset getCsvDatasetRefWithLessColumnsThanMain(String dataPath)
    {
        return CsvExternalDatasetReference.builder()
            .schema(getStagingSchemaWithLessColumnThanMain())
            .csvDataPath(dataPath)
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

    public static DatasetDefinition getUnitemporalMainTableWithMissingColumn()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(income)
                .addFields(startTime)
                .addFields(digest)
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

    public static DatasetDefinition getTickerPriceMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(ticker)
                .addFields(closePrice)
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

    public static DatasetDefinition getTickerPriceIdBasedMainTable()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .group(testSchemaName).name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(ticker)
                .addFields(closePrice)
                .addFields(volume)
                .addFields(digest)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
        return mainTable;
    }

    public static DatasetDefinition getTickerPriceTimeBasedMainTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(ticker)
                .addFields(closePrice)
                .addFields(volume)
                .addFields(digest)
                .addFields(batchTimeIn)
                .addFields(batchTimeOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getTickerPriceStagingTable()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(date)
                .addFields(ticker)
                .addFields(closePrice)
                .addFields(volume)
                .addFields(digest)
                .build()
            )
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
                .addFields(lakeFrom)
                .addFields(lakeThrough)
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
                .addFields(lakeFrom)
                .addFields(lakeThrough)
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

    public static DatasetDefinition getLoansMainTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(mainTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(loanStartDateTime)
                .addFields(loanEndDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansTempTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(tempTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(loanStartDateTime)
                .addFields(loanEndDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansTempTableWithDeleteIndicatorIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(tempWithDeleteIndicatorTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(loanStartDateTime)
                .addFields(loanEndDateTime)
                .addFields(batchIdIn)
                .addFields(batchIdOut)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableWithoutDuplicatesIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableWithoutDuplicatesName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(dataSplit)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableWithDeleteIndicatorIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(deleteIndicator)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableWithDeleteIndicatorWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(deleteIndicator)
                .addFields(dataSplit)
                .build()
            )
            .build();
    }

    public static DatasetDefinition getLoansStagingTableWithoutDuplicatesWithDeleteIndicatorWithDataSplitIdBased()
    {
        return DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableWithoutDuplicatesName)
            .schema(SchemaDefinition.builder()
                .addFields(loanId)
                .addFields(loanDateTime)
                .addFields(loanBalance)
                .addFields(digest)
                .addFields(deleteIndicator)
                .addFields(dataSplit)
                .build()
            )
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

    public static String getCheckIsNullableFromTableSql(String tableName, String columnName)
    {
        return "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='" + tableName + "' and COLUMN_NAME ='" + columnName + "'";
    }

    public static String getCheckDataTypeFromTableSql(Connection connection, String database, String schema, String tableName, String columnName) throws SQLException
    {
        ResultSet result = connection.getMetaData().getColumns(database, schema, tableName, columnName);
        String dataType = "";
        if (result.next())
        {
            dataType = JDBCType.valueOf(result.getInt("DATA_TYPE")).name();
        }
        return dataType;
    }

    private static List<String[]> readCsvData(String path) throws IOException
    {
        FileReader fileReader = new FileReader(path);
        CSVReader csvReader = new CSVReader(fileReader);
        List<String[]> lines = csvReader.readAll();
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
