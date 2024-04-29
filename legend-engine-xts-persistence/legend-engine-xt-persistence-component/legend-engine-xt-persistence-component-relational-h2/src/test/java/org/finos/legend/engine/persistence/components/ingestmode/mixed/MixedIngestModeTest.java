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

package org.finos.legend.engine.persistence.components.ingestmode.mixed;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.unitemporal.MultiTableIngestionTest;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

public class MixedIngestModeTest extends BaseTest
{

    private final String basePath = "src/test/resources/data/mixed/";

    @Test
    public void testMultiIngestionTypes() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta unitemporalDelta = UnitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();

        UnitemporalSnapshot unitemporalSnapshot = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // Pass 1 : unitemporalSnapshot
        String path = basePath + "input/staging_data_pass1.csv";
        loadBasicStagingData(path);
        String expectedPath = basePath + "output/expected_pass1.csv";
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(unitemporalSnapshot)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .enableConcurrentSafety(true)
                .build();

        IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        MultiTableIngestionTest.verifyResults(1, schema, expectedPath, "main", result, expectedStats);

        // Pass 2 : unitemporalDelta
        path = basePath + "input/staging_data_pass2.csv";
        loadBasicStagingData(path);
        expectedPath = basePath + "output/expected_pass2.csv";
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);

        ingestor = RelationalIngestor.builder()
                .ingestMode(unitemporalDelta)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .enableConcurrentSafety(true)
                .build();

        result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        MultiTableIngestionTest.verifyResults(2, schema, expectedPath, "main", result, expectedStats);

        // Pass 3 : unitemporalSnapshot
        path = basePath + "input/staging_data_pass3.csv";
        loadBasicStagingData(path);
        expectedPath = basePath + "output/expected_pass3.csv";
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 2);

        ingestor = RelationalIngestor.builder()
                .ingestMode(unitemporalSnapshot)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .enableConcurrentSafety(true)
                .build();

        result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        MultiTableIngestionTest.verifyResults(3, schema, expectedPath, "main", result, expectedStats);
    }

}
