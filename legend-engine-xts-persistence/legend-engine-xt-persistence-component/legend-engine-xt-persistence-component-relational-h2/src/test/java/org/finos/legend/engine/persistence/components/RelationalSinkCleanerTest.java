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
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSinkCleaner;
import org.finos.legend.engine.persistence.components.relational.api.SinkCleanupIngestorResult;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
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
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        createSampleMainTableWithData();
        createBatchMetadataTableWithData();
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(H2Sink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .build();

        //Table counts before sink cleanup
        List<Map<String, Object>> tableBeforeSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableBeforeSinkCleanup.get(0).get("batch_metadata_count"), 1L);

        SinkCleanupIngestorResult result = sinkCleaner.executeOperationsForSinkCleanup(JdbcConnection.of(h2Sink.connection()));
        Assertions.assertEquals(result.status(), IngestStatus.SUCCEEDED);

        //Table counts after sink cleanup
        List<Map<String, Object>> auditTableData = h2Sink.executeQuery("select count(*) as audit_table_count from \"sink_cleanup_audit\" where table_name = 'main'");
        Assertions.assertEquals(auditTableData.get(0).get("audit_table_count"), 1L);
        List<Map<String, Object>> tableAfterSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableAfterSinkCleanup.get(0).get("batch_metadata_count"), 0L);
    }

    @Test
    void testExecuteSinkCleanupWithConcurrency()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        LockInfoDataset lockTable = LockInfoDataset.builder().name("lock_info").build();
        createSampleMainTableWithData();
        createBatchMetadataTableWithData();
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(H2Sink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .enableConcurrentSafety(true)
                .lockInfoDataset(lockTable)
                .requestedBy("lh_dev")
                .build();

        //Table counts before sink cleanup
        List<Map<String, Object>> tableBeforeSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableBeforeSinkCleanup.get(0).get("batch_metadata_count"), 1L);

        SinkCleanupIngestorResult result = sinkCleaner.executeOperationsForSinkCleanup(JdbcConnection.of(h2Sink.connection()));
        Assertions.assertEquals(result.status(), IngestStatus.SUCCEEDED);

        //Table counts after sink cleanup
        List<Map<String, Object>> auditTableData = h2Sink.executeQuery("select count(*) as audit_table_count from \"sink_cleanup_audit\" where table_name = 'main'");
        Assertions.assertEquals(auditTableData.get(0).get("audit_table_count"), 1L);
        List<Map<String, Object>> tableAfterSinkCleanup = h2Sink.executeQuery("select count(*) as batch_metadata_count from \"batch_metadata\" where table_name = 'main'");
        Assertions.assertEquals(tableAfterSinkCleanup.get(0).get("batch_metadata_count"), 0L);
    }

    private void createBatchMetadataTableWithData()
    {
        List<String> list = new ArrayList<>();
        list.add("CREATE TABLE IF NOT EXISTS batch_metadata" +
                "(`table_name` VARCHAR(255)," +
                "`batch_start_ts_utc` DATETIME," +
                "`batch_end_ts_utc` DATETIME," +
                "`batch_status` VARCHAR(32)," +
                "`table_batch_id` INTEGER," +
                "`batch_source_info` JSON," +
                "`ADDITIONAL_METADATA` JSON)");
        list.add("INSERT INTO batch_metadata " +
                "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
                " (SELECT 'main',1,'2000-01-01 00:00:00.000000','2000-01-01 00:00:00.000000','DONE')");
        h2Sink.executeStatements(list);
    }

    private void createSampleMainTableWithData()
    {
        List<String> list = new ArrayList<>();
        list.add("CREATE TABLE main(ID INT PRIMARY KEY, NAME VARCHAR(255), BIRTH DATETIME)");
        list.add("INSERT INTO main VALUES (1, 'A', '2020-01-01 00:00:00')");
        list.add("INSERT INTO main VALUES (2, 'B', '2021-01-01 00:00:00')");
        h2Sink.executeStatements(list);
    }
}
