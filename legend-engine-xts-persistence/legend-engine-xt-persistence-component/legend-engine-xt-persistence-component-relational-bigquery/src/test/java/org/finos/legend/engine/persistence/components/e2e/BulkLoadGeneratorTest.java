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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Disabled
public class BulkLoadGeneratorTest extends BigQueryEndToEndTest
{
    private static final String DIGEST = "digest";
    private static final String APPEND_TIME = "append_time";
    private static final String BATCH_ID = "batch_id";
    private static final String EVENT_ID = "xyz123";
    private static final String COL_INT = "col_int";
    private static final String COL_STRING = "col_string";
    private static final String COL_DECIMAL = "col_decimal";
    private static final String COL_DATETIME = "col_datetime";
    private static final List<String> FILE_LIST = Arrays.asList("the uri to the staged_file1.csv on GCS", "the uri to the staged_file2.csv on GCS", "the uri to the staged_file3.csv on GCS");
    private static Field col1 = Field.builder()
        .name(COL_INT)
        .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
        .build();
    private static Field col2 = Field.builder()
        .name(COL_STRING)
        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
        .build();
    private static Field col3 = Field.builder()
        .name(COL_DECIMAL)
        .type(FieldType.of(DataType.DECIMAL, 5, 2))
        .build();
    private static Field col4 = Field.builder()
        .name(COL_DATETIME)
        .type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty()))
        .build();

    @Test
    public void testMilestoning() throws IOException, InterruptedException
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(FILE_LIST).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .group("demo").name("append_log")
            .schema(SchemaDefinition.builder().build())
            .build();

        MetadataDataset metadataDataset = MetadataDataset.builder().metadataDatasetGroupName("demo").metadataDatasetName("batch_metadata").build();

        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagedFilesDataset).metadataDataset(metadataDataset).build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");
        delete("demo", "append_log");


        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRequestId(EVENT_ID)
            .bulkLoadBatchStatusPattern("{STATUS}")
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();
        List<String> postCleanupSql = operations.postCleanupSql();

        List<String> newMetadataIngestSql = new ArrayList<>();
        for (String metadataSql : metadataIngestSql)
        {
            String newSql = metadataSql.replace("{STATUS}", "SUCCEEDED");
            newMetadataIngestSql.add(newSql);
        }
        metadataIngestSql = newMetadataIngestSql;


        ingest(preActionsSqlList, milestoningSqlList, metadataIngestSql, postActionsSql, postCleanupSql);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table1.csv";
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
    }

    @Test
    public void testMilestoningWithDigestGeneration() throws IOException, InterruptedException
    {
        Map<DataType, String> typeConversionUdfs = new HashMap<>();
        typeConversionUdfs.put(DataType.INT, "demo.numericToString");
        typeConversionUdfs.put(DataType.DECIMAL, "demo.numericToString");
        typeConversionUdfs.put(DataType.DATETIME, "demo.datetimeToString");

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder()
                .digestUdfName("demo.LAKEHOUSE_MD5")
                .putAllTypeConversionUdfNames(typeConversionUdfs)
                .digestField(digestName)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(FILE_LIST).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .group("demo").name("append_log")
            .schema(SchemaDefinition.builder().build())
            .build();

        MetadataDataset metadataDataset = MetadataDataset.builder().metadataDatasetGroupName("demo").metadataDatasetName("batch_metadata").build();

        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagedFilesDataset).metadataDataset(metadataDataset).build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");
        delete("demo", "append_log");

        // Register UDF
        runQuery("DROP FUNCTION IF EXISTS demo.numericToString;");
        runQuery("DROP FUNCTION IF EXISTS demo.datetimeToString;");
        runQuery("DROP FUNCTION IF EXISTS demo.stringifyArr;");
        runQuery("DROP FUNCTION IF EXISTS demo.LAKEHOUSE_MD5;");
        runQuery("CREATE FUNCTION demo.numericToString(value NUMERIC)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.datetimeToString(value DATETIME)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.stringifyArr(arr1 ARRAY<STRING>, arr2 ARRAY<STRING>)\n" +
            "            RETURNS STRING\n" +
            "            LANGUAGE js AS \"\"\"\n" +
            "            let output = \"\"; \n" +
            "            for (const [index, element] of arr1.entries()) { output += arr1[index]; output += arr2[index]; }\n" +
            "            return output;\n" +
            "            \"\"\"; \n");
        runQuery("CREATE FUNCTION demo.LAKEHOUSE_MD5(arr1 ARRAY<STRING>, arr2 ARRAY<STRING>)\n" +
            "AS (\n" +
            "  TO_HEX(MD5(demo.stringifyArr(arr1, arr2)))\n" +
            ");\n");

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRequestId(EVENT_ID)
            .bulkLoadBatchStatusPattern("{STATUS}")
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();
        List<String> postCleanupSql = operations.postCleanupSql();

        List<String> newMetadataIngestSql = new ArrayList<>();
        for (String metadataSql : metadataIngestSql)
        {
            String newSql = metadataSql.replace("{STATUS}", "SUCCEEDED");
            newMetadataIngestSql.add(newSql);
        }
        metadataIngestSql = newMetadataIngestSql;


        ingest(preActionsSqlList, milestoningSqlList, metadataIngestSql, postActionsSql, postCleanupSql);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table4.csv";
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, DIGEST, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
    }
}
