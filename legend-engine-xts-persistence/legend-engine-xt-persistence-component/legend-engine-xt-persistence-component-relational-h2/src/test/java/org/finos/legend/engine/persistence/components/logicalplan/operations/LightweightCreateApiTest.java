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

package org.finos.legend.engine.persistence.components.logicalplan.operations;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.TableNameGenUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.versionName;
import static org.finos.legend.engine.persistence.components.relational.api.utils.ApiUtils.LOCK_INFO_DATASET_SUFFIX;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_STAGING_DATASET_QUALIFIER;


class LightweightCreateApiTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_and_time_based/";

    @Test
    void testApiBasic()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .build();

        // Calling API
        ingestor.initExecutor(executor);
        ingestor.create(Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build());

        // Asserting on table creation
        Assertions.assertTrue(h2Sink.doesTableExist(mainTable));
        Assertions.assertTrue(h2Sink.doesTableExist(MetadataDataset.builder().build().get()));
    }

    @Test
    void testApiWithTempTablesAndCaseConversion()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .enableConcurrentSafety(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        // Calling API
        ingestor.initExecutor(executor);
        ingestor.create(Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build());

        // Asserting on table creation
        String ingestRunId = ingestor.getRunId();
        String tempStagingTableName = TableNameGenUtils.generateTableName(stagingTable.name(), TEMP_STAGING_DATASET_QUALIFIER, ingestRunId);
        DatasetReference tempStagingTable = DatasetReferenceImpl.builder().database(stagingTable.database()).group(stagingTable.group()).name(tempStagingTableName).build();
        String lockTableName = mainTable.name() + LOCK_INFO_DATASET_SUFFIX;
        DatasetReference lockTable = DatasetReferenceImpl.builder().database(mainTable.database()).group(mainTable.group()).name(lockTableName).build();

        Assertions.assertTrue(h2Sink.doesTableExist(convertDatasetToUpperCase(mainTable)));
        Assertions.assertTrue(h2Sink.doesTableExist(convertDatasetToUpperCase(MetadataDataset.builder().build().get())));
        Assertions.assertTrue(h2Sink.doesTableExist(convertDatasetToUpperCase(tempStagingTable)));
        Assertions.assertTrue(h2Sink.doesTableExist(convertDatasetToUpperCase(lockTable)));

        // Asserting on lock table initialization
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST\".\"" + lockTableName.toUpperCase() + "\"");
        Assertions.assertEquals("2000-01-01 00:00:00.0", tableData.get(0).get("INSERT_TS_UTC").toString());
    }

    @Test
    void testEndToEnd() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTable(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/digest_based/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResultsUsingLightweightCreate(ingestMode, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, true);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/digest_based/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(9, 0, 1, 2, 0);
        executePlansAndVerifyResultsUsingLightweightCreate(ingestMode, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01, true);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResultsUsingLightweightCreate(ingestMode, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01, true);
    }

    private Dataset convertDatasetToUpperCase(Dataset dataset)
    {
        return DatasetReferenceImpl.builder()
            .database(dataset.datasetReference().database().map(String::toUpperCase))
            .group(dataset.datasetReference().group().map(String::toUpperCase))
            .name(dataset.datasetReference().name().map(String::toUpperCase))
            .build();
    }
}