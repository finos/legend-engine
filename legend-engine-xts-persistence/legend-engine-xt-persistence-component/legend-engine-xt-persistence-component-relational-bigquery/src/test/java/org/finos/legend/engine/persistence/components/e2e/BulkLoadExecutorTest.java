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
import org.finos.legend.engine.persistence.components.common.FileFormat;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;

@Disabled
public class BulkLoadExecutorTest extends BigQueryEndToEndTest
{
    private static final String APPEND_TIME = "append_time";
    private static final String BATCH_ID = "batch_id";
    private static final String BATCH_ID_VALUE = "xyz123";
    private static final String col_int = "col_int";
    private static final String col_string = "col_string";
    private static final String col_decimal = "col_decimal";
    private static final String col_datetime = "col_datetime";
    private static final List<String> file_list = Arrays.asList("the uri to the staged_file1.csv on GCS", "the uri to the staged_file2.csv on GCS", "the uri to the staged_file3.csv on GCS");
    private static Field col1 = Field.builder()
        .name(col_int)
        .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
        .build();
    private static Field col2 = Field.builder()
        .name(col_string)
        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
        .build();
    private static Field col3 = Field.builder()
        .name(col_decimal)
        .type(FieldType.of(DataType.DECIMAL, 5, 2))
        .build();
    private static Field col4 = Field.builder()
        .name(col_datetime)
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
                    .fileFormat(FileFormat.CSV)
                    .addAllFiles(file_list).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .group("demo").name("append_log")
            .schema(SchemaDefinition.builder().build())
            .build();

        BulkLoadMetadataDataset bulkLoadMetadataDataset = BulkLoadMetadataDataset.builder().group("demo").name("bulk_load_batch_metadata").build();

        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagedFilesDataset).bulkLoadMetadataDataset(bulkLoadMetadataDataset).build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");
        delete("demo", "append_log");
        delete("demo", "bulk_load_batch_metadata");


        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadBatchIdValue(BATCH_ID_VALUE)
            .build();

        RelationalConnection connection = BigQueryConnection.of(getBigQueryConnection());
        IngestorResult ingestorResult = ingestor.performFullIngestion(connection, datasets);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`append_log` order by col_int asc");
        String expectedPath = "src/test/resources/expected/bulk_load/expected_table1.csv";
        String[] schema = new String[]{col_int, col_string, col_decimal, col_datetime, BATCH_ID, APPEND_TIME};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        long rowsInserted = (long) ingestorResult.statisticByName().get(ROWS_INSERTED);
        Assertions.assertEquals(7, rowsInserted);
    }
}
