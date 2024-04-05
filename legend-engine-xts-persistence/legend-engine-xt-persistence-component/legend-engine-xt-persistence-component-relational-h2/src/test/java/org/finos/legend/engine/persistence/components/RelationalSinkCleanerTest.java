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
//

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.ApiUtils;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSinkCleaner;
import org.finos.legend.engine.persistence.components.relational.api.SinkCleanupIngestorResult;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelationalSinkCleanerTest extends BaseTest
{
    @Test
    void testExecuteSinkCleanup()
    {
        MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").build();
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        createSampleMainTableWithData(mainTable.name());
        String lockTable = mainTable.name() + ApiUtils.LOCK_INFO_DATASET_SUFFIX;
        createLockTable(lockTable);
        LockInfoDataset lockDataset = LockInfoDataset.builder()
                .database(mainTable.datasetReference().database())
                .group(mainTable.datasetReference().group())
                .name(lockTable)
                .build();

        createBatchMetadataTableWithData(metadata.metadataDatasetName(), mainTable.name());
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(H2Sink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .metadataDataset(metadata)
                .build();

        //Table counts before sink cleanup
        List<Map<String, Object>> tableBeforeSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableBeforeSinkCleanup.get(0).get("batch_metadata_count"), 1L);
        Assertions.assertTrue(h2Sink.doesTableExist(mainTable));
        Assertions.assertTrue(h2Sink.doesTableExist(lockDataset.get()));

        SinkCleanupIngestorResult result = sinkCleaner.executeOperationsForSinkCleanup(JdbcConnection.of(h2Sink.connection()));
        Assertions.assertEquals(result.status(), IngestStatus.SUCCEEDED);

        List<Map<String, Object>> tableAfterSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableAfterSinkCleanup.get(0).get("batch_metadata_count"), 0L);

        Assertions.assertFalse(h2Sink.doesTableExist(mainTable));
        Assertions.assertFalse(h2Sink.doesTableExist(lockDataset.get()));
    }

    @Test
    void testExecuteSinkCleanupWithLockTable()
    {
        MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").build();
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        createSampleMainTableWithData(mainTable.name());
        String lockTable = "lock_table";
        createLockTable(lockTable);
        LockInfoDataset lockDataset = LockInfoDataset.builder()
                .database(mainTable.datasetReference().database())
                .group(mainTable.datasetReference().group())
                .name(lockTable)
                .build();

        createBatchMetadataTableWithData(metadata.metadataDatasetName(), mainTable.name());
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(H2Sink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .metadataDataset(metadata)
                .lockDataset(lockDataset)
                .build();

        //Table counts before sink cleanup
        List<Map<String, Object>> tableBeforeSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableBeforeSinkCleanup.get(0).get("batch_metadata_count"), 1L);
        Assertions.assertTrue(h2Sink.doesTableExist(mainTable));
        Assertions.assertTrue(h2Sink.doesTableExist(lockDataset.get()));

        SinkCleanupIngestorResult result = sinkCleaner.executeOperationsForSinkCleanup(JdbcConnection.of(h2Sink.connection()));
        Assertions.assertEquals(result.status(), IngestStatus.SUCCEEDED);

        List<Map<String, Object>> tableAfterSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableAfterSinkCleanup.get(0).get("batch_metadata_count"), 0L);

        Assertions.assertFalse(h2Sink.doesTableExist(mainTable));
        Assertions.assertFalse(h2Sink.doesTableExist(lockDataset.get()));
    }

    private void createLockTable(String lockTable)
    {
        h2Sink.executeStatement("CREATE TABLE TEST." + lockTable + " (ID INT PRIMARY KEY, NAME VARCHAR(255), BIRTH DATETIME)");
    }

    @Test
    void testExecuteSinkCleanupWithFailureStatus()
    {
        MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").build();
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        createSampleMainTableWithData(mainTable.name());
        createBatchMetadataTableWithData(metadata.metadataDatasetName(), mainTable.name());
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(H2Sink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .metadataDataset(metadata.withMetadataDatasetName("batch_metadata2"))
                .build();

        SinkCleanupIngestorResult result = sinkCleaner.executeOperationsForSinkCleanup(JdbcConnection.of(h2Sink.connection()));
        Assertions.assertEquals(result.status(), IngestStatus.FAILED);
        Assertions.assertTrue(result.message().get().contains("Table \"batch_metadata2\" not found"));
    }

    private void createBatchMetadataTableWithData(String metaTableName, String mainTableName)
    {
        String createMetaTable = "CREATE TABLE IF NOT EXISTS " + metaTableName +
                " (`table_name` VARCHAR(255)," +
                "`batch_start_ts_utc` DATETIME," +
                "`batch_end_ts_utc` DATETIME," +
                "`batch_status` VARCHAR(32)," +
                "`table_batch_id` INTEGER," +
                "`batch_source_info` JSON," +
                "`ADDITIONAL_METADATA` JSON)";
        String insertData = "INSERT INTO " + metaTableName +
                " (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
                " (SELECT '" + mainTableName + "',1,'2000-01-01 00:00:00.000000','2000-01-01 00:00:00.000000','DONE')";
        List<String> list = new ArrayList<>();
        if (metaTableName.toUpperCase().equals(metaTableName))
        {
            list.add(createMetaTable.toUpperCase());
            list.add(insertData.toUpperCase());
        }
        else
        {
            list.add(createMetaTable);
            list.add(insertData);
        }
        h2Sink.executeStatements(list);
    }

    private void createSampleMainTableWithData(String tableName)
    {
        List<String> list = new ArrayList<>();
        list.add("CREATE TABLE TEST." + tableName + " (ID INT PRIMARY KEY, NAME VARCHAR(255), BIRTH DATETIME)");
        list.add("INSERT INTO  TEST." + tableName + "  VALUES (1, 'A', '2020-01-01 00:00:00')");
        list.add("INSERT INTO  TEST." + tableName + "  VALUES (2, 'B', '2021-01-01 00:00:00')");
        h2Sink.executeStatements(list);
    }
}
