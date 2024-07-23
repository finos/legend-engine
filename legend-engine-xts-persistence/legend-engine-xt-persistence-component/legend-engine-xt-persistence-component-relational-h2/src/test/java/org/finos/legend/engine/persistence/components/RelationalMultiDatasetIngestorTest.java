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
import org.finos.legend.engine.persistence.components.relational.api.IngestStage;
import org.finos.legend.engine.persistence.components.relational.api.RelationalMultiDatasetIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.persistence.components.TestUtils.appendTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestUDF;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchema2WithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.getStagingSchemaWithoutPkWithoutDigest;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

class RelationalMultiDatasetIngestorTest extends BaseTest
{
    private static final String suffixForAppendTable = "_APPEND";
    private static final String suffixForFinalTable = "_FINAL";
    private static final String suffixForBatchMetadataTable = "_BATCH_METADATA";
    private static final String dataset1 = "DATASET_1";
    private static final String dataset2 = "DATASET_2";
    private static final String lockDataset = "LOCK_DATASET";
    private static final LockInfoDataset lockInfoDataset = LockInfoDataset.builder()
        .database(testDatabaseName)
        .group(testSchemaName)
        .name(lockDataset)
        .build();

    @Test
    public void testSameIngestMode()
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
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file3.csv")).build())
            .schema(getStagingSchemaWithoutPkWithoutDigest())
            .build();
        DatasetReference bulkLoadMainTableForDataset1 = DatasetReferenceImpl.builder()
            .name(dataset1 + suffixForAppendTable)
            .build();
        DatasetDefinition unitemporalDeltaStageTableForDataset1 = DatasetDefinition.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset1 + suffixForAppendTable)
            .schema(getStagingSchema())
            .build();
        DatasetReference unitemporalDeltaMainTableForDataset1 = DatasetReferenceImpl.builder()
            .name(dataset1 + suffixForFinalTable)
            .build();

        // Configure dataset 2
        StagedFilesDataset bulkLoadStageTableForDataset2 = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file3.csv")).build())
            .schema(getStagingSchema2WithoutPkWithoutDigest())
            .build();
        DatasetReference bulkLoadMainTableForDataset2 = DatasetReferenceImpl.builder()
            .name(dataset2 + suffixForAppendTable)
            .build();
        DatasetDefinition unitemporalDeltaStageTableForDataset2 = DatasetDefinition.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(dataset2 + suffixForAppendTable)
            .schema(getStagingSchema2())
            .build();
        DatasetReference unitemporalDeltaMainTableForDataset2 = DatasetReferenceImpl.builder()
            .name(dataset2 + suffixForFinalTable)
            .build();

        IngestStage ingestStage1ForDataset1 = IngestStage.builder().ingestMode(bulkLoad).stagingDataset(bulkLoadStageTableForDataset1).mainDataset(bulkLoadMainTableForDataset1).build();
        IngestStage ingestStage2ForDataset1 = IngestStage.builder().ingestMode(unitemporalDelta).stagingDataset(unitemporalDeltaStageTableForDataset1).mainDataset(unitemporalDeltaMainTableForDataset1).build();
        IngestStage ingestStage1ForDataset2 = IngestStage.builder().ingestMode(bulkLoad).stagingDataset(bulkLoadStageTableForDataset2).mainDataset(bulkLoadMainTableForDataset2).build();
        IngestStage ingestStage2ForDataset2 = IngestStage.builder().ingestMode(unitemporalDelta).stagingDataset(unitemporalDeltaStageTableForDataset2).mainDataset(unitemporalDeltaMainTableForDataset2).build();

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
            .build();

        ingestor.init(datasetIngestDetails, JdbcConnection.of(h2Sink.connection()));
        ingestor.create();
        ingestor.ingest();
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
}