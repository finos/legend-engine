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

package org.finos.legend.engine.persistence.components.ingestmode.nontemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.NoOp;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NoOpTest extends BaseTest
{

    @Test
    void testNoOp()
    {
        String ingestRequestId1 = "abcd-pqrs-0001";
        String ingestRequestId2 = "abcd-pqrs-0002";
        IngestorResult result = runIngest(ingestRequestId1, CaseConversion.NONE);

        // Assertions
        Assertions.assertEquals(1, result.batchId().get());
        Assertions.assertEquals("SUCCEEDED", result.status().toString());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionEndTimestampUTC());

        Map<String, Object> metadataResult = h2Sink.executeQuery("select * from batch_metadata").get(0);
        Assertions.assertEquals(1, metadataResult.get("table_batch_id"));
        Assertions.assertEquals(ingestRequestId1, metadataResult.get("ingest_request_id"));
        Assertions.assertEquals("main", metadataResult.get("table_name"));

        // Test idempotency
        result = runIngest(ingestRequestId1, CaseConversion.NONE);
        // Assertions
        Assertions.assertEquals(1, result.batchId().get());
        Assertions.assertEquals("SUCCEEDED", result.status().toString());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionEndTimestampUTC());

        metadataResult = h2Sink.executeQuery("select * from batch_metadata").get(0);
        Assertions.assertEquals(1, metadataResult.get("table_batch_id"));
        Assertions.assertEquals(ingestRequestId1, metadataResult.get("ingest_request_id"));
        Assertions.assertEquals("main", metadataResult.get("table_name"));

        // Run 2nd batch
        result = runIngest(ingestRequestId2, CaseConversion.NONE);
        // Assertions
        Assertions.assertEquals(2, result.batchId().get());
        Assertions.assertEquals("SUCCEEDED", result.status().toString());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionEndTimestampUTC());

        metadataResult = h2Sink.executeQuery("select * from batch_metadata").get(1);
        Assertions.assertEquals(2, metadataResult.get("table_batch_id"));
        Assertions.assertEquals(ingestRequestId2, metadataResult.get("ingest_request_id"));
        Assertions.assertEquals("main", metadataResult.get("table_name"));
    }

    @Test
    void testNoOpWithUpperCase()
    {
        String ingestRequestId = "abcd-pqrs-0001";
        IngestorResult result = runIngest(ingestRequestId, CaseConversion.TO_UPPER);

        // Assertions
        Assertions.assertEquals(1, result.batchId().get());
        Assertions.assertEquals("SUCCEEDED", result.status().toString());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionEndTimestampUTC());

        Map<String, Object> metadataResult = h2Sink.executeQuery("select * from BATCH_METADATA").get(0);
        Assertions.assertEquals(1, metadataResult.get("TABLE_BATCH_ID"));
        Assertions.assertEquals(ingestRequestId, metadataResult.get("INGEST_REQUEST_ID"));
        Assertions.assertEquals("MAIN", metadataResult.get("TABLE_NAME"));
    }

    private IngestorResult runIngest(String ingestRequestId, CaseConversion caseConversion)
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNoPks();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NoOp.builder().build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(options.cleanupStagingData())
                .collectStatistics(options.collectStatistics())
                .ingestRequestId(ingestRequestId)
                .caseConversion(caseConversion)
                .writeStatistics(true)
                .enableIdempotencyCheck(true)
                .enableConcurrentSafety(true)
                .build();

        Executor executor = ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        ingestor.create();
        executor.begin();
        IngestorResult result = ingestor.ingest().get(0);
        executor.commit();
        return result;
    }

}
