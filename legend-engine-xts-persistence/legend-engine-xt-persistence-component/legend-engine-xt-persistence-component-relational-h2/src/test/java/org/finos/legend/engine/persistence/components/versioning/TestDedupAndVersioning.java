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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningComparator;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.TEMP_STAGING_DATASET_BASE_NAME;

public class TestDedupAndVersioning extends BaseTest
{

    /* Scenarios:
    1. [DONE] No Dedup, NoVersion -> No tempStagingTable
    2. [DONE] No Dedup, MaxVersion do not perform versioning -> No tempStagingTable
    3. No Dedup, MaxVersion with perform versioning -> tempStagingTable with only MaxVersioned Data [throw Error on Data errors]
    4. [DONE] No Dedup, AllVersion do not perform versioning -> No tempStagingTable
    5. No Dedup, AllVersion with perform versioning -> tempStagingTable with Data splits [throw Error on Data errors]

    6. [DONE] Filter Dups, NoVersion -> tempStagingTable with count column
    7. [DONE] Filter Dups, MaxVersion do not perform versioning -> tempStagingTable with count column
    8. Filter Dups, MaxVersion with perform versioning -> tempStagingTable with count column and only max version [throw Error on Data errors]
    9. [DONE] Filter Dups, AllVersion do not perform versioning -> tempStagingTable with count column
    10. Filter Dups, AllVersion with perform versioning -> tempStagingTable with count column and Data splits [throw Error on Data errors]

    11. Fail on Dups, NoVersion -> tempStagingTable with count column [Throw error on dups]
    12. Fail on Dups, MaxVersion do not perform versioning -> tempStagingTable with count column [Throw error on dups]
    13. Fail on Dups, MaxVersion with perform versioning -> tempStagingTable with count column and only max version [Throw error on dups, throw Error on Data errors]
    14. Fail on Dups, AllVersion do not perform versioning -> tempStagingTable with count column [Throw error on dups]
    15. Fail on Dups, AllVersion with perform versioning -> tempStagingTable with count column and Data splits [Throw error on dups, throw Error on Data errors]
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

    private static final String tempStagingTableName = stagingTableName +  "_" + TEMP_STAGING_DATASET_BASE_NAME;

    String[] schemaWithCount = new String[]{idName, nameName, incomeName, expiryDateName, digestName, "legend_persistence_count"};

    String[] schemaWithVersionAndCount = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, "legend_persistence_count"};


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
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performVersioning(false).versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, h2Sink.doesTableExist(getTempStagingDataset()));
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
                .digestField("digest")
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version").performVersioning(false).versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        Assertions.assertEquals(false, h2Sink.doesTableExist(getTempStagingDataset()));
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
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data1.csv";
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
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").performVersioning(false).versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);
    }


    // Scenario 8
    @Test
    void testFilterDupsMaxVersion()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        createStagingTableWithVersion();
        // TODO LOAD DATA

        performDedupAndVersioining(datasets, ingestMode);

        // Validate tempTableExists
        Assertions.assertEquals(true, h2Sink.doesTableExist(getTempStagingDataset()));
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
                .digestField("digest")
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version").versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        createStagingTableWithVersion();
        String srcDataPath = "src/test/resources/data/dedup-and-versioning/input/data2.csv";
        String expectedDataPath = "src/test/resources/data/dedup-and-versioning/expected/expected_data2_filter_dups_no_versioning.csv";
        loadDataIntoStagingTableWithVersion(srcDataPath);

        performDedupAndVersioining(datasets, ingestMode);
        // Validate tempTableExists
        verifyResults(expectedDataPath, schemaWithVersionAndCount);
    }

    // Scenario 10
    @Test
    void testFilterDupsAllVersion()
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = getStagingTableWithVersion();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IngestMode ingestMode = AppendOnly.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField("append_time").build())
                .digestField("digest")
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("version").versioningComparator(VersioningComparator.ALWAYS).build())
                .build();

        createStagingTableWithVersion();
        // TODO LOAD DATA

        performDedupAndVersioining(datasets, ingestMode);

        // Validate tempTableExists
        Assertions.assertEquals(true, h2Sink.doesTableExist(getTempStagingDataset()));
    }

    private DatasetDefinition getStagingTableWithoutVersion()
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

    private DatasetDefinition getStagingTableWithVersion()
    {
        return DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(baseSchemaWithVersion)
                .build();
    }


    private void createStagingTableWithoutVersion()
    {
        String createSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"staging\"" +
                "(\"id\" INTEGER NOT NULL," +
                "\"name\" VARCHAR(64) NOT NULL," +
                "\"income\" BIGINT," +
                "\"expiry_date\" DATE," +
                "\"digest\" VARCHAR)";
        h2Sink.executeStatement(createSql);
    }

    private void createStagingTableWithVersion()
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

        Executor executor = ingestor.init(JdbcConnection.of(h2Sink.connection()));
        datasets = ingestor.create(datasets);
        datasets = ingestor.dedupAndVersion(datasets);
    }

    protected void loadDataIntoStagingTableWithoutVersion(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(id, name, income ,expiry_date, digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, income, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadDataIntoStagingTableWithVersion(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(id, name, version, income ,expiry_date, digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"version\",INT ), CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, version, income, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    private void verifyResults(String expectedDataPath, String [] schema) throws IOException
    {
        Assertions.assertEquals(true, h2Sink.doesTableExist(getTempStagingDataset()));
        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from \"TEST\".\"%s\"", tempStagingTableName));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
    }


}
