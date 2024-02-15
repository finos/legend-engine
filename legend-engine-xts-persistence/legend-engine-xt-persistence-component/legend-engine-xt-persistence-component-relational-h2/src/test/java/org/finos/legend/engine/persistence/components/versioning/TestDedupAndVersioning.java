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

package org.finos.legend.engine.persistence.components.versioning;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.exception.DataQualityException;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;
import static org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategyAbstract.DATA_SPLIT;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.TEMP_STAGING_DATASET_BASE_NAME;

public class TestDedupAndVersioning extends BaseTest
{

    /* Scenarios:
    1. No Dedup, NoVersion -> No tempStagingTable
    2. No Dedup, MaxVersion do not perform versioning -> No tempStagingTable
    3. No Dedup, MaxVersion with perform versioning -> tempStagingTable with only MaxVersioned Data [throw Error on Data errors]
    4. No Dedup, AllVersion do not perform versioning -> No tempStagingTable
    5. No Dedup, AllVersion with perform versioning -> tempStagingTable with Data splits [throw Error on Data errors]

    6. Filter Dups, NoVersion -> tempStagingTable with count column
    7. Filter Dups, MaxVersion do not perform versioning -> tempStagingTable with count column
    8. Filter Dups, MaxVersion with perform versioning -> tempStagingTable with count column and only max version [throw Error on Data errors]
    9. Filter Dups, AllVersion do not perform versioning -> tempStagingTable with count column
    10. Filter Dups, AllVersion with perform versioning -> tempStagingTable with count column and Data splits [throw Error on Data errors]

    11.Fail on Dups, NoVersion -> tempStagingTable with count column [Throw error on dups]
    12.Fail on Dups, MaxVersion do not perform versioning -> tempStagingTable with count column [Throw error on dups]
    13.Fail on Dups, MaxVersion with perform versioning -> tempStagingTable with count column and only max version [Throw error on dups, throw Error on Data errors]
    14.Fail on Dups, AllVersion do not perform versioning -> tempStagingTable with count column [Throw error on dups]
    15.Fail on Dups, AllVersion with perform versioning -> tempStagingTable with count column and Data splits [Throw error on dups, throw Error on Data errors]
    */

    private static Field name = Field.builder().name(nameName).type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).primaryKey(true).fieldAlias(nameName).build();

    // Base Schema : PK : id, name
     public static SchemaDefinition baseSchemaWithoutVersion =
             SchemaDefinition.builder()
                     .addFields(id)
                     .addFields(name)
                     .addFields(income)
                     .addFields(expiryDate)
                     .addFields(digest)
                     .build();

    public static SchemaDefinition baseSchemaWithVersion =
            SchemaDefinition.builder()
                    .addFields(id)
                    .addFields(name)
                    .addFields(version)
                    .addFields(income)
                    .addFields(expiryDate)
                    .addFields(digest)
                    .build();

    public static SchemaDefinition baseSchemaWithVersionAndBatch =
            SchemaDefinition.builder()
                    .addFields(id)
                    .addFields(name)
                    .addFields(version)
                    .addFields(income)
                    .addFields(expiryDate)
                    .addFields(digest)
                    .addFields(batch)
                    .build();

    private static final String tempStagingTableName = stagingTableName +  "_" + TEMP_STAGING_DATASET_BASE_NAME;

    String[] schemaWithCount = new String[]{idName, nameName, incomeName, expiryDateName, digestName, "legend_persistence_count"};
    String[] schemaWithVersion = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName};
    String[] schemaWithVersionAndCount = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, "legend_persistence_count"};
    String[] schemaWithVersionCountAndDataSplit = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, "legend_persistence_count", DATA_SPLIT};

    String[] schemaWithVersionAndDataSplit = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, DATA_SPLIT};


    // Scenario 1
    @Test
    void testNoDedupNoVersioning()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithoutVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .build();

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, h2Sink.doesTableExist(getTempStagingDataset()));
    }

    // Scenario 2
    @Test
    void testNoDedupMaxVersioningDoNotPerform()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performStageVersioning(false).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, h2Sink.doesTableExist(getTempStagingDataset()));
    }

    // Scenario 3
    @Test
    void testNoDedupMaxVersioning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_allow_dups_max_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersion);

        // Data error scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);
            row1.put("legend_persistence_error_count", 2);
            expectedSampleRows.add(row1);
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 4
    @Test
    void testNoDedupAllVersioningDoNotPerform()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version").performStageVersioning(false).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, h2Sink.doesTableExist(getTempStagingDataset()));
    }

    // Scenario 5
    @Test
    void testNoDedupAllVersion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version")
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(true).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_allow_dups_all_version.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndDataSplit);

        // Data error scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);
            row1.put("legend_persistence_error_count", 2);
            expectedSampleRows.add(row1);
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 6
    @Test
    void testFilterDupsNoVersioning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithoutVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        createStagingTableWithoutVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data1_with_dups.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data1_filter_dups_no_versioning.csv";
        loadDataIntoStagingTableWithoutVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithCount);
    }

    // Scenario 7
    @Test
    void testFilterDupsMaxVersionDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performStageVersioning(false).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndCount);
    }


    // Scenario 8
    @Test
    void testFilterDupsMaxVersion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_max_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndCount);

        // Data error scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);
            row1.put("legend_persistence_error_count", 2);
            expectedSampleRows.add(row1);
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 9
    @Test
    void testFilterDupsAllVersionDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version")
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(false).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);
    }

    // Scenario 10
    @Test
    void testFilterDupsAllVersion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version")
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(true).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_all_version.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionCountAndDataSplit);

        // Data error scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);
            row1.put("legend_persistence_error_count", 2);
            expectedSampleRows.add(row1);
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 11
    @Test
    void testFailOnDupsNoVersioning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithoutVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();
        createStagingTableWithoutVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data1_with_dups.csv";
        loadDataIntoStagingTableWithoutVersion(srcDataPath);

        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row1  = new HashMap<>();
            row1.put("name", "Andy");
            row1.put("id", 1);
            row1.put("legend_persistence_count", 3);
            Map<String, Object> row2  = new HashMap<>();
            row2.put("name", "Becky");
            row2.put("id", 2);
            row2.put("legend_persistence_count", 2);
            expectedSampleRows.add(row1);
            expectedSampleRows.add(row2);
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 12
    @Test
    void testFailOnDupsMaxVersionDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performStageVersioning(false).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        // Happy scenario
        createStagingTableWithVersion();
        String srcDataPath1 = "src/test/resources/data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);
            row.put("legend_persistence_count", 2);
            expectedSampleRows.add(row);
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 13
    @Test
    void testFailOnDupsMaxVersion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performStageVersioning(true).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .build();

        // Happy scenario
        createStagingTableWithVersion();
        String srcDataPath1 = "src/test/resources/data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_max_versioin.csv";
        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);
            row.put("legend_persistence_count", 2);
            expectedSampleRows.add(row);
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }


    // Scenario 14
    @Test
    void testFailOnDupsAllVersionDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version")
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(false).build())
                .build();

        // Happy scenario
        createStagingTableWithVersion();
        String srcDataPath1 = "src/test/resources/data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);
            row.put("legend_persistence_count", 2);
            expectedSampleRows.add(row);
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy",e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }

    // Scenario 15
    @Test
    void testFailOnDupsAllVersion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version")
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(true).build())
                .build();

        // Happy scenario
        createStagingTableWithVersion();
        String srcDataPath1 = "src/test/resources/data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionCountAndDataSplit);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "src/test/resources/data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath2);
        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            List<Map<String, Object>> expectedSampleRows = new ArrayList<>();
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);
            row.put("legend_persistence_count", 2);
            expectedSampleRows.add(row);
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            TestUtils.assertEquals(expectedSampleRows, e.getSampleRows());
        }
    }


    public static DatasetDefinition getStagingTableWithoutVersion()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(baseSchemaWithoutVersion)
                .build();
    }

    private Dataset getTempStagingDataset()
    {
        return DatasetReferenceImpl.builder()
                .group(testSchemaName)
                .name(tempStagingTableName)
                .build();
    }

    public static DatasetDefinition getStagingTableWithVersion()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(baseSchemaWithVersion)
                .build();
    }


    public static void createStagingTableWithoutVersion()
    {
        String createSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"staging\"" +
                "(\"id\" INTEGER NOT NULL," +
                "\"name\" VARCHAR(64) NOT NULL," +
                "\"income\" BIGINT," +
                "\"expiry_date\" DATE," +
                "\"digest\" VARCHAR)";
        h2Sink.executeStatement(createSql);
    }

    public static void createStagingTableWithVersion()
    {
        String createSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"staging\"" +
                "(\"id\" INTEGER NOT NULL," +
                "\"name\" VARCHAR(64) NOT NULL," +
                "\"version\" INTEGER," +
                "\"income\" BIGINT," +
                "\"expiry_date\" DATE," +
                "\"digest\" VARCHAR)";
        h2Sink.executeStatement(createSql);
    }

    private static void performDedupAndVersioining(Datasets datasets, IngestMode ingestMode)
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .build();

        Executor executor = ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        ingestor.create();
        ingestor.dedupAndVersion();
    }

    public static void loadDataIntoStagingTableWithoutVersion(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(id, name, income ,expiry_date, digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, income, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    public static void loadDataIntoStagingTableWithVersion(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(id, name, version, income ,expiry_date, digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"version\",INT ), CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, version, income, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    public static void loadDataIntoStagingTableWithVersionAndBatch(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(id, name, version, income ,expiry_date, digest, batch) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"version\",INT ), CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE), digest, CONVERT( \"batch\",INT )" +
                " FROM CSVREAD( '" + path + "', 'id, name, version, income, expiry_date, digest, batch', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    private void verifyResults(String expectedDataPath, String [] schema) throws IOException
    {
        Assertions.assertEquals(true, h2Sink.doesTableExist(getTempStagingDataset()));
        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from \"TEST\".\"%s\"", tempStagingTableName));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
    }
}
