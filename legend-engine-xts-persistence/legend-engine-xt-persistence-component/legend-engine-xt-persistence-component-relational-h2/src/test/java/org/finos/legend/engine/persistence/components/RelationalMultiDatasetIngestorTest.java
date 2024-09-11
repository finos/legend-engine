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
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionComparator;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestDetails;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestResults;
import org.finos.legend.engine.persistence.components.relational.api.DryRunResult;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.IngestStage;
import org.finos.legend.engine.persistence.components.relational.api.IngestStageResult;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.RelationalMultiDatasetIngestor;
import org.finos.legend.engine.persistence.components.relational.exception.MultiDatasetException;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.BatchErrorDataset;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithVersion;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithVersionWithoutPk;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithVersionWithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithNonPkVersion;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithVersionWithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.ratingName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;
import static org.finos.legend.engine.persistence.components.TestUtils.versionName;

public class RelationalMultiDatasetIngestorTest extends BaseTest
{
    private static final String suffixForAppendTable = "_APPEND";
    private static final String suffixForFinalTable = "_FINAL";
    private static final String suffixForBatchMetadataTable = "_BATCH_METADATA";
    private static final String dataset1 = "DATASET_1";
    private static final String dataset2 = "DATASET_2";
    private static final String dataset3 = "DATASET_3";
    private static final String requestId1 = "REQUEST_1";
    private static final String requestId2 = "REQUEST_2";
    private static final String requestId3 = "REQUEST_3";
    private static final String lockDataset = "LOCK_DATASET";
    private static final String batchErrorDatasetName = "BATCH_ERROR_DATASET";

    private static final MetadataDataset metadataDataset1 = MetadataDataset.builder()
        .metadataDatasetDatabaseName(testDatabaseName)
        .metadataDatasetGroupName(testSchemaName)
        .metadataDatasetName(dataset1 + suffixForBatchMetadataTable)
        .build();

    private static final MetadataDataset metadataDataset2 = MetadataDataset.builder()
        .metadataDatasetDatabaseName(testDatabaseName)
        .metadataDatasetGroupName(testSchemaName)
        .metadataDatasetName(dataset2 + suffixForBatchMetadataTable)
        .build();

    private static final MetadataDataset metadataDataset3 = MetadataDataset.builder()
            .metadataDatasetDatabaseName(testDatabaseName)
            .metadataDatasetGroupName(testSchemaName)
            .metadataDatasetName(dataset3 + suffixForBatchMetadataTable)
            .build();

    private static final LockInfoDataset lockInfoDataset = LockInfoDataset.builder()
        .database(testDatabaseName)
        .group(testSchemaName)
        .name(lockDataset)
        .build();

    private static final BatchErrorDataset batchErrorDataset = BatchErrorDataset.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(batchErrorDatasetName)
            .build();

    /*
    Test Case:
        - [Dataset1: [BulkLoad, UnitemporalDelta],
           Dataset2: [BulkLoad, UnitemporalDelta]]
           Also tests idempotency
     */
    @Test
    public void testSameIngestMode() throws IOException
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);



        // Batch 1
        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .enableIdempotencyCheck(true)
            .build();

        List<DatasetIngestDetails> datasetIngestDetails = configureForTest1("src/test/resources/data/multi-dataset/set1/input/file1_for_dataset1.csv", "src/test/resources/data/multi-dataset/set1/input/file1_for_dataset2.csv");

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        List<DatasetIngestResults> actual = ingestor.ingest();

        // Verify results
        IngestStageResult ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 3);
        IngestStageResult ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 3, 0, 3, 0, 0);
        IngestStageResult ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 4);
        IngestStageResult ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 4, 0, 4, 0, 0);

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
            Arrays.asList("src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset1_final.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset1 + suffixForFinalTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName},
                new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName},
                new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdName},
                new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName}));


        // Test Idempotency - trigger with same batch id again
        List<DatasetIngestResults> rerunResults = ingestor.ingest();
        verifyResults(
                rerunResults, expected,
                Arrays.asList("src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset1_append.csv",
                        "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset1_final.csv",
                        "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset2_append.csv",
                        "src/test/resources/data/multi-dataset/set1/expected/expected_pass1_for_dataset2_final.csv"),
                Arrays.asList(dataset1 + suffixForAppendTable,
                        dataset1 + suffixForFinalTable,
                        dataset2 + suffixForAppendTable,
                        dataset2 + suffixForFinalTable),
                Arrays.asList(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName},
                        new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName},
                        new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdName},
                        new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName}));

        // Batch 2
        ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId2)
            .executionTimestampClock(fixedClock_2000_01_02)
            .build();

        datasetIngestDetails = configureForTest1("src/test/resources/data/multi-dataset/set1/input/file2_for_dataset1.csv", "src/test/resources/data/multi-dataset/set1/input/file2_for_dataset2.csv");

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        actual = ingestor.ingest();

        // Verify results
        ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 3);
        ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 3, 0, 1, 0, 1);
        ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 3);
        ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 3, 0, 0, 0, 2);

        expected = new ArrayList<>();
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset1)
            .batchId(2L)
            .ingestRequestId(requestId2)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset1, ingestStageResult2ForDataset1))
            .build());
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset2)
            .batchId(2L)
            .ingestRequestId(requestId2)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset2, ingestStageResult2ForDataset2))
            .build());

        verifyResults(
            actual, expected,
            Arrays.asList("src/test/resources/data/multi-dataset/set1/expected/expected_pass2_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass2_for_dataset1_final.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass2_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set1/expected/expected_pass2_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset1 + suffixForFinalTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName},
                new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName},
                new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdName},
                new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName}));
    }

    public static List<DatasetIngestDetails> configureForTest1(String filePathForBulkLoad1, String filePathForBulkLoad2)
    {
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
                    .addAllFilePaths(Collections.singletonList(filePathForBulkLoad1)).build())
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
                    .addAllFilePaths(Collections.singletonList(filePathForBulkLoad2)).build())
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

        return buildDatasetIngestDetails(Arrays.asList(ingestStage1ForDataset1, ingestStage2ForDataset1), Arrays.asList(ingestStage1ForDataset2, ingestStage2ForDataset2));
    }

    /*
    Test Case:
        - [Dataset1: [BulkLoad, UnitemporalDelta],
           Dataset2: [BulkLoad, UnitemporalDelta]]
       - CaseConversion: TO_UPPER
       - FilterDuplicates
       - MaxVersioning
     */
    @Test
    public void testSameIngestModeWithDedupAndVersioningUpperCase() throws IOException
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);



        // Batch 1
        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId1)
            .caseConversion(CaseConversion.TO_UPPER)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<DatasetIngestDetails> datasetIngestDetails = configureForTest2("src/test/resources/data/multi-dataset/set2/input/file1_for_dataset1.csv", "src/test/resources/data/multi-dataset/set2/input/file1_for_dataset2.csv");

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        List<DatasetIngestResults> actual = ingestor.ingest();

        // Verify results
        IngestStageResult ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 9);
        IngestStageResult ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 9, 0, 3, 0, 0);
        IngestStageResult ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 8);
        IngestStageResult ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 8, 0, 4, 0, 0);

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
            Arrays.asList("src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset1_final.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset1 + suffixForFinalTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new)));



        // Batch 2 (empty batch)
        ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId2)
            .caseConversion(CaseConversion.TO_UPPER)
            .executionTimestampClock(fixedClock_2000_01_02)
            .build();

        datasetIngestDetails = configureForTest2("src/test/resources/data/empty_file.csv", "src/test/resources/data/empty_file.csv");

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        actual = ingestor.ingest();

        // Verify results
        ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 0);
        ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 0, 0, 0, 0, 0);
        ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 0);
        ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-02 00:00:00.123456", "2000-01-02 00:00:00.123456", 0, 0, 0, 0, 0);

        expected = new ArrayList<>();
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset1)
            .batchId(2L)
            .ingestRequestId(requestId2)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset1, ingestStageResult2ForDataset1))
            .build());
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset2)
            .batchId(2L)
            .ingestRequestId(requestId2)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset2, ingestStageResult2ForDataset2))
            .build());

        verifyResults(
            actual, expected,
            Arrays.asList("src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset1_final.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass1_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset1 + suffixForFinalTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new)));



        // Batch 3
        ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .ingestRequestId(requestId3)
            .caseConversion(CaseConversion.TO_UPPER)
            .executionTimestampClock(fixedClock_2000_01_03)
            .build();

        datasetIngestDetails = configureForTest2("src/test/resources/data/multi-dataset/set2/input/file2_for_dataset1.csv", "src/test/resources/data/multi-dataset/set2/input/file2_for_dataset2.csv");

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        actual = ingestor.ingest();

        // Verify results
        ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-03 00:00:00.000000", "2000-01-03 00:00:00.000000", 5);
        ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-03 00:00:00.000000", "2000-01-03 00:00:00.000000", 5, 0, 1, 0, 1);
        ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-03 00:00:00.000000", "2000-01-03 00:00:00.000000", 5);
        ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-03 00:00:00.000000", "2000-01-03 00:00:00.000000", 5, 0, 0, 0, 1);

        expected = new ArrayList<>();
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset1)
            .batchId(3L)
            .ingestRequestId(requestId3)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset1, ingestStageResult2ForDataset1))
            .build());
        expected.add(DatasetIngestResults.builder()
            .dataset(dataset2)
            .batchId(3L)
            .ingestRequestId(requestId3)
            .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset2, ingestStageResult2ForDataset2))
            .build());

        verifyResults(
            actual, expected,
            Arrays.asList("src/test/resources/data/multi-dataset/set2/expected/expected_pass2_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass2_for_dataset1_final.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass2_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set2/expected/expected_pass2_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset1 + suffixForFinalTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdName}).map(String::toUpperCase).toArray(String[]::new),
                Arrays.stream(new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdInName, batchIdOutName}).map(String::toUpperCase).toArray(String[]::new)));

    }

    private List<DatasetIngestDetails> configureForTest2(String filePathForBulkLoad1, String filePathForBulkLoad2)
    {
        // Configure ingest modes
        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUDF).digestField(digestName).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(appendTimeName).build())
            .batchIdField(batchIdName)
            .build();

        UnitemporalDelta unitemporalDelta = UnitemporalDelta.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(true)
                .build())
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .build();

        // Configure dataset 1
        StagedFilesDataset bulkLoadStageTableForDataset1 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePathForBulkLoad1)).build())
            .schema(getStagingSchemaWithVersionWithoutPkWithoutDigest())
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
            .schema(getStagingSchemaWithNonPkVersion())
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
                    .addAllFilePaths(Collections.singletonList(filePathForBulkLoad2)).build())
            .schema(getStagingSchema2WithVersionWithoutPkWithoutDigest())
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
            .schema(getStagingSchema2WithVersion())
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

        return buildDatasetIngestDetails(Arrays.asList(ingestStage1ForDataset1, ingestStage2ForDataset1), Arrays.asList(ingestStage1ForDataset2, ingestStage2ForDataset2));
    }

    /*
    Test Case:
        - [Dataset1: [BulkLoad],
           Dataset2: [BulkLoad, NontemporalSnapshot]]
           Dataset3: [NoOp, NoOp]]
       - FilterDuplicates
       - MaxVersioning
     */
    @Test
    public void testMixedIngestMode() throws IOException
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        // Configure ingest modes
        BulkLoad bulkLoad1 = BulkLoad.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUDF).digestField(digestName).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(appendTimeName).build())
            .batchIdField(batchIdName)
            .build();

        BulkLoad bulkLoad2 = BulkLoad.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(appendTimeName).build())
            .batchIdField(batchIdName)
            .build();

        NontemporalSnapshot nontemporalSnapshot = NontemporalSnapshot.builder()
            .auditing(NoAuditing.builder().build())
            .batchIdField(batchIdName)
            .versioningStrategy(MaxVersionStrategy.builder().versioningField(versionName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        NoOp noOp = NoOp.builder().build();

        // Configure dataset 1
        StagedFilesDataset bulkLoadStageTableForDataset1 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set3/input/file1_for_dataset1.csv")).build())
            .schema(getStagingSchemaWithoutPkWithoutDigest())
            .build();
        DatasetReference bulkLoadMainTableForDataset1 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset1 + suffixForAppendTable)
            .build();

        // Configure dataset 2
        StagedFilesDataset bulkLoadStageTableForDataset2 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set3/input/file1_for_dataset2.csv")).build())
            .schema(getStagingSchema2WithVersionWithoutPk())
            .build();
        DatasetReference bulkLoadMainTableForDataset2 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForAppendTable)
            .build();
        DatasetDefinition nontemporalSnapshotStageTableForDataset2 = DatasetDefinition.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForAppendTable)
            .schema(getStagingSchema2WithVersion())
            .build();
        DatasetReference nontemporalSnapshotMainTableForDataset2 = DatasetReferenceImpl.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForFinalTable)
            .build();

        // Configure dataset3
        StagedFilesDataset noOpStageTable1ForDataset3 = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(H2StagedFilesDatasetProperties.builder().fileFormat(FileFormatType.CSV).build())
                .schema(getStagingSchema2WithVersionWithoutPk())
                .build();
        DatasetReference noOpMainTable1ForDataset3 = DatasetReferenceImpl.builder()
                .database(testDatabaseName)
                .group(testSchemaName)
                .name(dataset3 + suffixForAppendTable)
                .build();
        DatasetDefinition noOpStageTable2ForDataset3 = DatasetDefinition.builder()
                .database(testDatabaseName)
                .group(testSchemaName)
                .name(dataset3 + suffixForAppendTable)
                .schema(getStagingSchema2WithVersion())
                .build();
        DatasetReference noOpMainTable2ForDataset3 = DatasetReferenceImpl.builder()
                .database(testDatabaseName)
                .group(testSchemaName)
                .name(dataset3 + suffixForFinalTable)
                .build();

        // Configure ingest stages
        IngestStage ingestStage1ForDataset1 = IngestStage.builder().ingestMode(bulkLoad1).stagingDataset(bulkLoadStageTableForDataset1).mainDataset(bulkLoadMainTableForDataset1).build();
        IngestStage ingestStage1ForDataset2 = IngestStage.builder().ingestMode(bulkLoad2).stagingDataset(bulkLoadStageTableForDataset2).mainDataset(bulkLoadMainTableForDataset2).build();
        IngestStage ingestStage2ForDataset2 = IngestStage.builder().ingestMode(nontemporalSnapshot).stagingDataset(nontemporalSnapshotStageTableForDataset2).mainDataset(nontemporalSnapshotMainTableForDataset2).stagingDatasetBatchIdField(batchIdName).build();
        IngestStage ingestStage1ForDataset3 = IngestStage.builder().ingestMode(noOp).stagingDataset(noOpStageTable1ForDataset3).mainDataset(noOpMainTable1ForDataset3).build();
        IngestStage ingestStage2ForDataset3 = IngestStage.builder().ingestMode(noOp).stagingDataset(noOpStageTable2ForDataset3).mainDataset(noOpMainTable2ForDataset3).build();

        List<DatasetIngestDetails> datasetIngestDetails = buildDatasetIngestDetails(Collections.singletonList(ingestStage1ForDataset1),
                Arrays.asList(ingestStage1ForDataset2, ingestStage2ForDataset2),
                Arrays.asList(ingestStage1ForDataset3, ingestStage2ForDataset3));

        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .batchErrorDataset(batchErrorDataset)
            .ingestRequestId(requestId1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        List<DatasetIngestResults> actual = ingestor.ingest();

        // Verify results
        IngestStageResult ingestStageResult1ForDataset1 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 3);
        IngestStageResult ingestStageResult2ForDataset1 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 3, 0, 3, 0, 0);
        IngestStageResult ingestStageResult1ForDataset2 = buildIngestStageResultForBulkLoad("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 8);
        IngestStageResult ingestStageResult2ForDataset2 = buildIngestStageResult("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000", 8, 0, 4, 0, 0);
        IngestStageResult ingestStageResult1ForDataset3 = buildIngestStageResultForNoOp("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000");
        IngestStageResult ingestStageResult2ForDataset3 = buildIngestStageResultForNoOp("2000-01-01 00:00:00.000000", "2000-01-01 00:00:00.000000");

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
        expected.add(DatasetIngestResults.builder()
                .dataset(dataset3)
                .batchId(1L)
                .ingestRequestId(requestId1)
                .addAllIngestStageResults(Arrays.asList(ingestStageResult1ForDataset3, ingestStageResult2ForDataset3))
                .build());

        verifyResults(
            actual, expected,
            Arrays.asList("src/test/resources/data/multi-dataset/set3/expected/expected_pass1_for_dataset1_append.csv",
                "src/test/resources/data/multi-dataset/set3/expected/expected_pass1_for_dataset2_append.csv",
                "src/test/resources/data/multi-dataset/set3/expected/expected_pass1_for_dataset2_final.csv"),
            Arrays.asList(dataset1 + suffixForAppendTable,
                dataset2 + suffixForAppendTable,
                dataset2 + suffixForFinalTable),
            Arrays.asList(new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName},
                new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdName},
                new String[]{idName, nameName, ratingName, startTimeName, versionName, digestName, batchIdName}));

        // Verify the results of batch metadata for dataset 3
        List<Map<String, Object>> results = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset3 + suffixForBatchMetadataTable + "\"");
        Assertions.assertEquals("2000-01-01 00:00:00.0", results.get(0).get("batch_end_ts_utc").toString());
        Assertions.assertEquals(1, results.get(0).get("table_batch_id"));
        Assertions.assertEquals("REQUEST_1", results.get(0).get("ingest_request_id"));
        Assertions.assertEquals("SUCCEEDED", results.get(0).get("batch_status"));
        Assertions.assertEquals("DATASET_3_APPEND", results.get(0).get("table_name"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", results.get(0).get("batch_start_ts_utc").toString());

        Assertions.assertEquals("2000-01-01 00:00:00.0", results.get(1).get("batch_end_ts_utc").toString());
        Assertions.assertEquals(1, results.get(1).get("table_batch_id"));
        Assertions.assertEquals("REQUEST_1", results.get(1).get("ingest_request_id"));
        Assertions.assertEquals("SUCCEEDED", results.get(1).get("batch_status"));
        Assertions.assertEquals("DATASET_3_FINAL", results.get(1).get("table_name"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", results.get(1).get("batch_start_ts_utc").toString());
    }

    /*
    Test Case:
        - [Dataset1: [BulkLoad, UnitemporalDelta],
           Dataset2: [BulkLoad, UnitemporalDelta]]
       - FailOnDuplicates
       - Execution fails at second stage of Dataset2, commit is reverted
     */
    @Test
    public void testFirstDatasetSuccessAndSecondDatasetFailure()
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
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        // Configure dataset 1
        StagedFilesDataset bulkLoadStageTableForDataset1 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set4/input/file1_for_dataset1.csv")).build())
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
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set4/input/file1_for_dataset2.csv")).build())
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

        List<DatasetIngestDetails> datasetIngestDetails = buildDatasetIngestDetails(Arrays.asList(ingestStage1ForDataset1, ingestStage2ForDataset1), Arrays.asList(ingestStage1ForDataset2, ingestStage2ForDataset2));

        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .batchErrorDataset(batchErrorDataset)
            .ingestRequestId(requestId1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        // Run ingestion
        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        try
        {
            List<DatasetIngestResults> actual = ingestor.ingest();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof MultiDatasetException);
            MultiDatasetException multiDatasetException = (MultiDatasetException) e;
            Assertions.assertTrue(multiDatasetException.getMessage().contains("Encountered exception for dataset: [DATASET_2] : Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy"));
            Assertions.assertEquals("DATASET_2", multiDatasetException.getDataset());
            Assertions.assertEquals(1, multiDatasetException.getStageIndex());

            List<Map<String, Object>> tableData1 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset1 + suffixForAppendTable + "\"");
            List<Map<String, Object>> tableData2 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset1 + suffixForFinalTable + "\"");
            List<Map<String, Object>> tableData3 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset1 + suffixForBatchMetadataTable + "\"");
            List<Map<String, Object>> tableData4 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset2 + suffixForAppendTable + "\"");
            List<Map<String, Object>> tableData5 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset2 + suffixForFinalTable + "\"");
            List<Map<String, Object>> tableData6 = h2Sink.executeQuery("select * from \"TEST\".\"" + dataset2 + suffixForBatchMetadataTable + "\"");
            Assertions.assertTrue(tableData1.isEmpty());
            Assertions.assertTrue(tableData2.isEmpty());
            Assertions.assertTrue(tableData3.isEmpty());
            Assertions.assertTrue(tableData4.isEmpty());
            Assertions.assertTrue(tableData5.isEmpty());
            Assertions.assertTrue(tableData6.isEmpty());
        }
    }

    /*
    Test Case:
        - [Dataset1: [BulkLoad, UnitemporalDelta],
           Dataset2: [BulkLoad, UnitemporalDelta]]
       - Execution fails at first stage of Dataset2
       - Dry run is triggered
     */
    @Test
    public void testDryRun()
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
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        // Configure dataset 1
        StagedFilesDataset bulkLoadStageTableForDataset1 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set5/input/file1_for_dataset1.csv")).build())
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
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/multi-dataset/set5/input/file1_for_dataset2.csv")).build())
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

        List<DatasetIngestDetails> datasetIngestDetails = buildDatasetIngestDetails(Arrays.asList(ingestStage1ForDataset1, ingestStage2ForDataset1), Arrays.asList(ingestStage1ForDataset2, ingestStage2ForDataset2));

        RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
            .relationalSink(H2Sink.get())
            .lockInfoDataset(lockInfoDataset)
            .batchErrorDataset(batchErrorDataset)
            .ingestRequestId(requestId1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        // Run ingestion

        Executor executor = ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();

        try
        {
            executor.begin();
            List<DatasetIngestResults> actual = ingestor.ingestInCurrentTransaction();
            executor.commit();

            Assertions.fail("Exception was not thrown");
        }
        catch (MultiDatasetException mde)
        {
            executor.revert();
            Assertions.assertEquals(0, mde.getStageIndex());
            Assertions.assertTrue(mde.getMessage().contains("Encountered exception for dataset: [DATASET_2] : Data conversion error converting"));

            // Trigger dry run
            DryRunResult dryRunResult = ingestor.dryRun(mde.getDataset());
            List<DataError> expectedErrorRecords = Arrays.asList(DataError.builder()
                .errorCategory(ErrorCategory.TYPE_CONVERSION)
                .errorRecord("{\"start_time\":\"2020-01-01 00:00:00.0\",\"name\":\"HARRY\",\"rating\":\"10\",\"id\":\"???\"}")
                .errorMessage("Unable to type cast column")
                .putAllErrorDetails(buildErrorDetails("src/test/resources/data/multi-dataset/set5/input/file1_for_dataset2.csv", idName, 1L))
                .build(), DataError.builder()
                .errorCategory(ErrorCategory.TYPE_CONVERSION)
                .errorRecord("{\"start_time\":\"2020-01-99 00:00:00.0\",\"name\":\"ANDY\",\"rating\":\"8\",\"id\":\"3\"}")
                .errorMessage("Unable to type cast column")
                .putAllErrorDetails(buildErrorDetails("src/test/resources/data/multi-dataset/set5/input/file1_for_dataset2.csv", startTimeName, 3L))
                .build());
            Assertions.assertEquals(IngestStatus.FAILED, dryRunResult.status());
            Assertions.assertEquals(new HashSet<>(expectedErrorRecords), new HashSet<>(dryRunResult.errorRecords()));
        }
        finally
        {
            executor.close();
            ingestor.cleanup();
        }
    }

    private static List<DatasetIngestDetails> buildDatasetIngestDetails(List<IngestStage> ingestStages1, List<IngestStage> ingestStages2)
    {
        List<DatasetIngestDetails> datasetIngestDetails = new ArrayList<>();
        datasetIngestDetails.add(DatasetIngestDetails.builder()
            .dataset(dataset1)
            .addAllIngestStages(ingestStages1)
            .metadataDataset(metadataDataset1)
            .build());
        datasetIngestDetails.add(DatasetIngestDetails.builder()
            .dataset(dataset2)
            .addAllIngestStages(ingestStages2)
            .metadataDataset(metadataDataset2)
            .build());
        return datasetIngestDetails;
    }

    private List<DatasetIngestDetails> buildDatasetIngestDetails(List<IngestStage> ingestStages1, List<IngestStage> ingestStages2, List<IngestStage> ingestStages3)
    {
        List<DatasetIngestDetails> datasetIngestDetails = new ArrayList<>();
        datasetIngestDetails.add(DatasetIngestDetails.builder()
                .dataset(dataset1)
                .addAllIngestStages(ingestStages1)
                .metadataDataset(metadataDataset1)
                .build());
        datasetIngestDetails.add(DatasetIngestDetails.builder()
                .dataset(dataset2)
                .addAllIngestStages(ingestStages2)
                .metadataDataset(metadataDataset2)
                .build());
        datasetIngestDetails.add(DatasetIngestDetails.builder()
                .dataset(dataset3)
                .addAllIngestStages(ingestStages3)
                .metadataDataset(metadataDataset3)
                .build());
        return datasetIngestDetails;
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

    private IngestStageResult buildIngestStageResultForNoOp(String ingestionStartTimestampUTC, String ingestionEndTimestampUTC)
    {
        return IngestStageResult.builder()
                .ingestionStartTimestampUTC(ingestionStartTimestampUTC)
                .ingestionEndTimestampUTC(ingestionEndTimestampUTC)
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
            List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST\".\"" + datasetNames.get(i) + "\"");
            TestUtils.assertFileAndTableDataEquals(schema.get(i), expectedDataPaths.get(i), tableData);
        }

    }

    private void verifyStatsForIngestStage(Map<StatisticName, Object> expectedStats, Map<StatisticName, Object> actualStats)
    {
        Map<String, Object> expectedStatsWithStringKey = expectedStats.keySet().stream().collect(Collectors.toMap(Enum::name, expectedStats::get));
        verifyStats(expectedStatsWithStringKey, actualStats);
    }

    private Map<String, Object> buildErrorDetails(String fileName, String columnName, Long recordNumber)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(DataError.FILE_NAME, fileName);
        errorDetails.put(DataError.COLUMN_NAME, columnName);
        errorDetails.put(DataError.RECORD_NUMBER, recordNumber);
        return errorDetails;
    }
}