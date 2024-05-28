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

package org.finos.legend.engine.persistence.components.e2e.versioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.e2e.TestUtils;
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
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.exception.DataQualityException;
import org.finos.legend.engine.persistence.components.relational.postgres.PostgresSink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.TableNameGenUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.e2e.TestUtils.*;
import static org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategyAbstract.DATA_SPLIT;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_STAGING_DATASET_QUALIFIER;

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

    16.Fail on Dups, NoVersion with fail on duplicate PKs -> tempStagingTable with count column and pk_count column [Throw error on pk dups]
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

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, postgresSink.doesTableExist(getTempStagingDataset(ingestRunId)));
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

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, postgresSink.doesTableExist(getTempStagingDataset(ingestRunId)));
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_allow_dups_max_versioning.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersion, ingestRunId);

        // Data error scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);

            DataError dataError = buildDataError(ErrorCategory.DATA_VERSION_ERROR, row1, buildErrorDetailsMap("num_data_version_errors", 2L));
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, postgresSink.doesTableExist(getTempStagingDataset(ingestRunId)));
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_allow_dups_all_version.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndDataSplit, ingestRunId);

        // Data error scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);

            DataError dataError = buildDataError(ErrorCategory.DATA_VERSION_ERROR, row1, buildErrorDetailsMap("num_data_version_errors", 2L));
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath = "data/dedup-and-versioning/input/data1_with_dups.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data1_filter_dups_no_versioning.csv";
        loadDedupAndVersioningStagingDataWithoutVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithCount, ingestRunId, "order by \"id\"");
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_max_versioning.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);

        // Data error scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);

            DataError dataError = buildDataError(ErrorCategory.DATA_VERSION_ERROR, row1, buildErrorDetailsMap("num_data_version_errors", 2L));
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);
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
        String srcDataPath = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_all_version.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath);

        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionCountAndDataSplit, ingestRunId);

        // Data error scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data3_with_dups_and_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1 = new HashMap<>();
            row1.put("name", "Cathy");
            row1.put("id", 3);
            row1.put("version", 1);

            DataError dataError = buildDataError(ErrorCategory.DATA_VERSION_ERROR, row1, buildErrorDetailsMap("num_data_version_errors", 2L));
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath = "data/dedup-and-versioning/input/data1_with_dups.csv";
        loadDedupAndVersioningStagingDataWithoutVersion(srcDataPath);

        try
        {
            String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1  = new HashMap<>();
            row1.put("name", "Andy");
            row1.put("id", 1);

            Map<String, Object> row2  = new HashMap<>();
            row2.put("name", "Becky");
            row2.put("id", 2);

            DataError dataError1 = buildDataError(ErrorCategory.DUPLICATES, row1, buildErrorDetailsMap("num_duplicates", 3));
            DataError dataError2 = buildDataError(ErrorCategory.DUPLICATES, row2, buildErrorDetailsMap("num_duplicates", 2));
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError1, dataError2), e.getDataErrors());
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
        String srcDataPath1 = "data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);

            DataError dataError = buildDataError(ErrorCategory.DUPLICATES, row, buildErrorDetailsMap("num_duplicates", 2));
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath1 = "data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_max_versioin.csv";
        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);

            DataError dataError = buildDataError(ErrorCategory.DUPLICATES, row, buildErrorDetailsMap("num_duplicates", 2));
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath1 = "data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount, ingestRunId);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);

            DataError dataError = buildDataError(ErrorCategory.DUPLICATES, row, buildErrorDetailsMap("num_duplicates", 2));
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
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
        String srcDataPath1 = "data/dedup-and-versioning/input/data4_without_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data4_fail_on_dups_no_versioning.csv";
        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionCountAndDataSplit, ingestRunId);


        // Duplicates scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data2_with_dups_no_data_error.csv";
        loadDedupAndVersioningStagingDataWithVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row  = new HashMap<>();
            row.put("name", "Becky");
            row.put("id", 2);

            DataError dataError = buildDataError(ErrorCategory.DUPLICATES, row, buildErrorDetailsMap("num_duplicates", 2));
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError), e.getDataErrors());
        }
    }

    // Scenario 16
    @Test
    void testFailOnDupsNoVersionFailOnDupPks() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithoutVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
            .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
            .build();

        // Happy scenario
        createStagingTableWithoutPks(stagingTable);
        String srcDataPath1 = "data/dedup-and-versioning/input/data5_without_dups.csv";
        loadDedupAndVersioningStagingDataWithoutVersion(srcDataPath1);

        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data5_fail_on_dups_no_versioning_fail_on_dups_pk.csv";
        String ingestRunId = performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithCount, ingestRunId, "order by \"id\"");


        // Duplicate PK scenario, should throw error
        String srcDataPath2 = "data/dedup-and-versioning/input/data6_with_dups_pk.csv";
        loadDedupAndVersioningStagingDataWithoutVersion(srcDataPath2);
        try
        {
            ingestRunId = performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (DataQualityException e)
        {
            Map<String, Object> row1  = new HashMap<>();
            row1.put("name", "Andy");
            row1.put("id", 1);

            Map<String, Object> row2  = new HashMap<>();
            row2.put("name", "Cathy");
            row2.put("id", 3);

            DataError dataError1 = buildDataError(ErrorCategory.DUPLICATE_PRIMARY_KEYS, row1, buildErrorDetailsMap("num_pk_duplicates", 3L));
            DataError dataError2 = buildDataError(ErrorCategory.DUPLICATE_PRIMARY_KEYS, row2, buildErrorDetailsMap("num_pk_duplicates", 2L));
            Assertions.assertEquals("Encountered multiple rows with duplicate primary keys, Failing the batch as Fail on Duplicate Primary Keys is selected", e.getMessage());
            Assertions.assertEquals(Arrays.asList(dataError1, dataError2), e.getDataErrors());
        }
    }

    @Test
    void testInvalidCombination()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithoutVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
            .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
            .build();

        try
        {
            performDedupAndVersioining(datasets, ingestMode);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("For failOnDuplicatePrimaryKeys, FailOnDuplicates must be selected as the DeduplicationStrategy", e.getMessage());
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

    private Dataset getTempStagingDataset(String ingestRunId)
    {
        return DatasetReferenceImpl.builder()
                .group(testSchemaName)
                .name(getTempStagingTableName(ingestRunId))
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
        postgresSink.executeStatement(createSql);
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
        postgresSink.executeStatement(createSql);
    }

    private static String performDedupAndVersioining(Datasets datasets, IngestMode ingestMode)
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(PostgresSink.get())
                .build();

        Executor executor = ingestor.initExecutor(JdbcConnection.of(postgresSink.connection()));
        ingestor.initDatasets(datasets);
        ingestor.create();
        ingestor.dedupAndVersion();
        return ingestor.getIngestRunId();
    }

    private void verifyResults(String expectedDataPath, String [] schema, String ingestRunId, String orderByClause) throws IOException
    {
        Assertions.assertEquals(true, postgresSink.doesTableExist(getTempStagingDataset(ingestRunId)));
        List<Map<String, Object>> tableData = postgresSink.executeQuery(String.format("select * from \"TEST\".\"%s\"" +  orderByClause, getTempStagingTableName(ingestRunId)));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
    }

    private void verifyResults(String expectedDataPath, String [] schema, String ingestRunId) throws IOException
    {
        Assertions.assertEquals(true, postgresSink.doesTableExist(getTempStagingDataset(ingestRunId)));
        List<Map<String, Object>> tableData = postgresSink.executeQuery(String.format("select * from \"TEST\".\"%s\" order by \"id\", \"version\"", getTempStagingTableName(ingestRunId)));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
    }

    private String getTempStagingTableName(String ingestRunId)
    {
        return TableNameGenUtils.generateTableName(stagingTableName, TEMP_STAGING_DATASET_QUALIFIER, ingestRunId);
    }

    private Map<String, Object> buildErrorDetailsMap(String key, Object value)
    {
        Map<String, Object> errorDetailsMap = new HashMap<>();
        errorDetailsMap.put(key, value);
        return errorDetailsMap;
    }

    private DataError buildDataError(ErrorCategory errorCategory, Map<String, Object> row, Map<String, Object> errorDetailsMap) throws JsonProcessingException
    {
        DataError dataError = DataError.builder()
                .errorMessage(errorCategory.getDefaultErrorMessage())
                .errorCategory(errorCategory)
                .errorRecord(new ObjectMapper().writeValueAsString(row))
                .putAllErrorDetails(errorDetailsMap)
                .build();
        return dataError;
    }

    protected void loadStagingDataWithVersion(String path)
    {
        postgresTestContainer.copyFileToContainer(path, path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "COPY \"TEST\".\"staging\"" +
                "(\"id\", \"name\", \"version\", \"income\", \"expiry_date\", \"digest\")" +
                " FROM '/" + path + "' CSV";
        postgresSink.executeStatement(loadSql);
    }
}
