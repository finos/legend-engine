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
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_WITH_ERRORS;

@Disabled
public class BulkLoadExecutorTest extends BigQueryEndToEndTest
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
    private static final List<String> BAD_FILE_LIST = Arrays.asList("the uri to the bad_file.csv on GCS", "the uri to the staged_file1.csv on GCS");
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


        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .build();

        RelationalConnection connection = BigQueryConnection.of(getBigQueryConnection());
        IngestorResult ingestorResult = ingestor.performFullIngestion(connection, datasets).get(0);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table1.csv";
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        long rowsInserted = (long) ingestorResult.statisticByName().get(ROWS_INSERTED);
        long rowsWithErrors = (long) ingestorResult.statisticByName().get(ROWS_WITH_ERRORS);
        Assertions.assertEquals(7, rowsInserted);
        Assertions.assertEquals(0, rowsWithErrors);
        Assertions.assertEquals(IngestStatus.SUCCEEDED, ingestorResult.status());
    }

    @Test
    public void testMilestoningFailure() throws IOException, InterruptedException
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Map<String, Object> loadOptions = new HashMap<>();
        loadOptions.put("max_bad_records", 10L);
        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .putAllLoadOptions(loadOptions)
                    .addAllFilePaths(BAD_FILE_LIST).build())
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


        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        RelationalConnection connection = BigQueryConnection.of(getBigQueryConnection());
        IngestorResult ingestorResult = ingestor.performFullIngestion(connection, datasets).get(0);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table2.csv";
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        long rowsInserted = (long) ingestorResult.statisticByName().get(ROWS_INSERTED);
        long rowsWithErrors = (long) ingestorResult.statisticByName().get(ROWS_WITH_ERRORS);
        Assertions.assertEquals(4, rowsInserted);
        Assertions.assertEquals(2, rowsWithErrors);
        Assertions.assertEquals(IngestStatus.FAILED, ingestorResult.status());
    }

    @Test
    public void testMilestoningWithUdfBasedDigestGenerationWithFieldsToExclude() throws IOException, InterruptedException
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName("demo.LAKEHOUSE_MD5").digestField(digestName).addAllFieldsToExcludeFromDigest(Arrays.asList(col1.name(), col4.name())).build())
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
        runQuery("DROP FUNCTION IF EXISTS demo.stringifyJson;");
        runQuery("DROP FUNCTION IF EXISTS demo.LAKEHOUSE_MD5;");
        runQuery("CREATE FUNCTION demo.stringifyJson(json_data JSON)\n" +
            "            RETURNS STRING\n" +
            "            LANGUAGE js AS \"\"\"\n" +
            "            let output = \"\"; \n" +
            "            Object.keys(json_data).sort().filter(field => json_data[field] != null).forEach(field => { output += field; output += json_data[field];})\n" +
            "            return output;\n" +
            "            \"\"\"; \n");
        runQuery("CREATE FUNCTION demo.LAKEHOUSE_MD5(json_data JSON)\n" +
            "AS (\n" +
            "  TO_HEX(MD5(demo.stringifyJson(json_data)))\n" +
            ");\n");

        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .build();

        RelationalConnection connection = BigQueryConnection.of(getBigQueryConnection());
        IngestorResult ingestorResult = ingestor.performFullIngestion(connection, datasets).get(0);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table3.csv";
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, DIGEST, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        long rowsInserted = (long) ingestorResult.statisticByName().get(ROWS_INSERTED);
        long rowsWithErrors = (long) ingestorResult.statisticByName().get(ROWS_WITH_ERRORS);
        Assertions.assertEquals(7, rowsInserted);
        Assertions.assertEquals(0, rowsWithErrors);
        Assertions.assertEquals(IngestStatus.SUCCEEDED, ingestorResult.status());
    }
}
