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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestDetails;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestResults;
import org.finos.legend.engine.persistence.components.relational.api.IngestStage;
import org.finos.legend.engine.persistence.components.relational.api.IngestStageResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalMultiDatasetIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.TestUtils.appendTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestUDF;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.ratingName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

class RelationalMultiDatasetIngestorTest extends BaseTest
{
    private static final String suffixForAppendTable = "_APPEND";
    private static final String suffixForFinalTable = "_FINAL";
    private static final String suffixForBatchMetadataTable = "_BATCH_METADATA";
    private static final String dataset1 = "DATASET_1";
    private static final String dataset2 = "DATASET_2";
    private static final String requestId1 = "REQUEST_1";
    private static final String lockDataset = "LOCK_DATASET";
    private static final String[] schema1 = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};
    private static final String[] schema2 = new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};
    private static final LockInfoDataset lockInfoDataset = LockInfoDataset.builder()
        .database(testDatabaseName)
        .group(testSchemaName)
        .name(lockDataset)
        .build();

    @Test
    public void testSameIngestMode() throws IOException
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        // Configure ingest modes
        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUDF).digestField(digestName).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(appendTimeName).build())
            .batchIdField(batchIdName)
            .build();

        UnitemporalDelta unitemporalDelta = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        // Configure dataset 1
        StagedFilesDataset bulkLoadStageTableForDataset1 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/same-ingest-mode/input/file1_for_dataset1.csv")).build())
            .schema(getStagingSchemaWithoutPkWithoutDigest())
            .build();
        DatasetReference bulkLoadMainTableForDataset1 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset1 + suffixForAppendTable)
            .build();
        DatasetDefinition unitemporalDeltaStageTableForDataset1 = DatasetDefinition.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset1 + suffixForAppendTable)
            .schema(getStagingSchema())
            .build();
        DatasetReference unitemporalDeltaMainTableForDataset1 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset1 + suffixForFinalTable)
            .build();

        // Configure dataset 2
        StagedFilesDataset bulkLoadStageTableForDataset2 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/same-ingest-mode/input/file1_for_dataset2.csv")).build())
            .schema(getStagingSchema2WithoutPkWithoutDigest())
            .build();
        DatasetReference bulkLoadMainTableForDataset2 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForAppendTable)
            .build();
        DatasetDefinition unitemporalDeltaStageTableForDataset2 = DatasetDefinition.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForAppendTable)
            .schema(getStagingSchema2())
            .build();
        DatasetReference unitemporalDeltaMainTableForDataset2 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForFinalTable)
            .build();

        // Configure ingest stages
        IngestStage ingestStage1ForDataset1 = IngestStage.builder().ingestMode(bulkLoad).stagingDataset(bulkLoadStageTableForDataset1).mainDataset(bulkLoadMainTableForDataset1).build();
        IngestStage ingestStage2ForDataset1 = IngestStage.builder().ingestMode(unitemporalDelta).stagingDataset(unitemporalDeltaStageTableForDataset1).mainDataset(unitemporalDeltaMainTableForDataset1).stagingDatasetBatchIdField(batchIdName).build();
        IngestStage ingestStage1ForDataset2 = IngestStage.builder().ingestMode(bulkLoad).stagingDataset(bulkLoadStageTableForDataset2).mainDataset(bulkLoadMainTableForDataset2).build();
        IngestStage ingestStage2ForDataset2 = IngestStage.builder().ingestMode(unitemporalDelta).stagingDataset(unitemporalDeltaStageTableForDataset2).mainDataset(unitemporalDeltaMainTableForDataset2).stagingDatasetBatchIdField(batchIdName).build();

        List<DatasetIngestDetails> datasetIngestDetails = new ArrayList<>();
        datasetIngestDetails.add(DatasetIngestDetails.builder()
            .dataset(dataset1)
            .addIngestStages(ingestStage1ForDataset1, ingestStage2ForDataset1)
            .metadataDataset(MetadataDataset.builder().metadataDatasetName(dataset1 + suffixForBatchMetadataTable).build())
            .build());
        datasetIngestDetails.add(DatasetIngestDetails.builder()
            .dataset(dataset2)
            .addIngestStages(ingestStage1ForDataset2, ingestStage2ForDataset2)
            .metadataDataset(MetadataDataset.builder().metadataDatasetName(dataset2 + suffixForBatchMetadataTable).build())
            .build());

        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        List<DatasetIngestResults> actual = ingestor.ingest();

        // Verify results
        IngestStageResult ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "DUMMY", 3);
        IngestStageResult ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-01 00:00:00.000000", "DUMMY", 3, 0, 3, 0, 0);
        IngestStageResult ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "DUMMY", 4);
        IngestStageResult ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-01 00:00:00.000000", "DUMMY", 4, 0, 4, 0, 0);

        List<DatasetIngestResults> expected = new ArrayList<>();
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset1)
            .batchId(1L)
            .ingestRequestId(requestId1)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset1, ingestStageResult2ForDataset1))
            .build());
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset2)
            .batchId(1L)
            .ingestRequestId(requestId1)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset2, ingestStageResult2ForDataset2))
            .build());

        verifyResults(
            actual, expected,
            Arrays.asList("src/test/resources/data/multi-dataset/same-ingest-mode/expected/expected_pass1_for_dataset1.csv", "src/test/resources/data/multi-dataset/same-ingest-mode/expected/expected_pass1_for_dataset2.csv"),
            Arrays.asList(dataset1, dataset2), Arrays.asList(schema1, schema2));
    }

    @Test
    public void testSameIngestModeUpperCase()
    {
    }

    @Test
    public void testMixedIngestMode()
    {
    }

    @Test
    public void testFailure()
    {
    }

    private IngestStageResult buildIngestStageResultForBulkLoad(String ingestionStartTimestampUTC, String ingestionEndTimestampUTC, int rowsInserted)
    {
        return IngestStageResult.builder()
            .ingestionStartTimestampUTC(ingestionStartTimestampUTC)
            .ingestionEndTimestampUTC(ingestionEndTimestampUTC)
            .putStatisticByName(StatisticName.FILES_LOADED, 1)
            .putStatisticByName(StatisticName.ROWS_WITH_ERRORS, 0)
            .putStatisticByName(StatisticName.ROWS_INSERTED, rowsInserted)
            .build();
    }

    private IngestStageResult buildIngestStageResult(String ingestionStartTimestampUTC, String ingestionEndTimestampUTC,
                                                     int incomingRecordCount, int rowsDeleted, int rowsInserted, int rowsTerminated, int rowsUpdated)
    {
        return IngestStageResult.builder()
            .ingestionStartTimestampUTC(ingestionStartTimestampUTC)
            .ingestionEndTimestampUTC(ingestionEndTimestampUTC)
            .putStatisticByName(StatisticName.INCOMING_RECORD_COUNT, incomingRecordCount)
            .putStatisticByName(StatisticName.ROWS_DELETED, rowsDeleted)
            .putStatisticByName(StatisticName.ROWS_INSERTED, rowsInserted)
            .putStatisticByName(StatisticName.ROWS_TERMINATED, rowsTerminated)
            .putStatisticByName(StatisticName.ROWS_UPDATED, rowsUpdated)
            .build();
    }

    private void verifyResults(List<DatasetIngestResults> actual, List<DatasetIngestResults> expected, List<String> expectedDataPaths, List<String> datasetNames, List<String[]> schema) throws IOException
    {
        // Verify results including stats
        Assertions.assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
        {
            DatasetIngestResults actualDatasetIngestResult = actual.get(i);
            DatasetIngestResults expectedDatasetIngestResult = expected.get(i);
            Assertions.assertEquals(expectedDatasetIngestResult.dataset(), actualDatasetIngestResult.dataset());
            Assertions.assertEquals(expectedDatasetIngestResult.batchId(), actualDatasetIngestResult.batchId());
            Assertions.assertEquals(expectedDatasetIngestResult.ingestRequestId(), actualDatasetIngestResult.ingestRequestId());
            for (int j = 0; j < actualDatasetIngestResult.ingestStageResults().size(); j++)
            {
                IngestStageResult actualIngestStageResult = actualDatasetIngestResult.ingestStageResults().get(j);
                IngestStageResult expectedIngestStageResult = expectedDatasetIngestResult.ingestStageResults().get(j);
                Assertions.assertEquals(expectedIngestStageResult.ingestionStartTimestampUTC(), actualIngestStageResult.ingestionStartTimestampUTC());
                Assertions.assertEquals(expectedIngestStageResult.ingestionEndTimestampUTC(), actualIngestStageResult.ingestionEndTimestampUTC());
                verifyStatsForIngestStage(expectedIngestStageResult.statisticByName(), actualIngestStageResult.statisticByName());
            }
        }

        // Verify table data
        for (int i = 0; i < datasetNames.size(); i++)
        {
            List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST\".\"" + datasetNames.get(i) + suffixForFinalTable + "\"");
            TestUtils.assertFileAndTableDataEquals(schema.get(i), expectedDataPaths.get(i), tableData);
        }

    }

    private void verifyStatsForIngestStage(Map<StatisticName, Object> expectedStats, Map<StatisticName, Object> actualStats)
    {
        Map<String, Object> expectedStatsWithStringKey = expectedStats.keySet().stream().collect(Collectors.toMap(Enum::name, expectedStats::get));
        verifyStats(expectedStatsWithStringKey, actualStats);
    }
}