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

package org.finos.legend.engine.persistence.components.e2e;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.opencsv.CSVReader;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.*;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryConnection;
import org.finos.legend.engine.persistence.components.relational.bigquery.executor.BigQueryHelper;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BigQueryEndToEndTest
{
    protected Field id = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field amount = Field.builder().name("amount").type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).build();
    protected Field bizDate = Field.builder().name("biz_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();
    protected Field digest = Field.builder().name("digest").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
    protected Field insertTimestamp = Field.builder().name("insert_ts").type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty())).build();
    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final ZonedDateTime fixedZonedDateTime_2000_01_02 = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);

    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_02 = Clock.fixed(fixedZonedDateTime_2000_01_02.toInstant(), ZoneOffset.UTC);

    private String projectId = "blueplayground";
    private String credentialPath = "/Users/ashutosh/Downloads/blueplayground-85f1ebcae766.json";

    protected SchemaDefinition stagingSchema = SchemaDefinition.builder()
            .addFields(id) // PK
            .addFields(name) // PK
            .addFields(amount)
            .addFields(bizDate)
            .addFields(digest)
            .addFields(insertTimestamp)
            .build();

    protected DatasetDefinition mainDataset = DatasetDefinition.builder()
            .group("demo").name("main").alias("sink")
            .schema(SchemaDefinition.builder().build())
            .build();

    MetadataDataset metadataDataset = MetadataDataset.builder().metadataDatasetGroupName("demo").metadataDatasetName("batch_metadata").build();


    protected IngestorResult ingestViaExecutor(IngestMode ingestMode, DatasetFilter stagingFilter, String path, Clock clock) throws IOException, InterruptedException
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(BigQuerySink.get())
                .collectStatistics(true)
                .cleanupStagingData(false)
                .executionTimestampClock(clock)
                .build();

        DerivedDataset stagingDataset = DerivedDataset.builder()
                .group("demo")
                .name("staging")
                .alias("stage")
                .schema(stagingSchema)
                .addDatasetFilters(stagingFilter)
                .build();
        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagingDataset).metadataDataset(metadataDataset).build();

        // Load csv data
        loadData(path, datasets.stagingDataset(), 1);
        RelationalConnection connection = BigQueryConnection.of(getBigQueryConnection());
        IngestorResult ingestorResult = ingestor.ingest(connection, datasets);
        return ingestorResult;
    }


    protected void ingestViaGenerator(IngestMode ingestMode, DatasetFilter stagingFilter, String path, Clock clock) throws IOException, InterruptedException
    {

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(BigQuerySink.get())
                .collectStatistics(true)
                .cleanupStagingData(false)
                .executionTimestampClock(clock)
                .build();

        DerivedDataset stagingDataset = DerivedDataset.builder()
                .group("demo")
                .name("staging")
                .alias("stage")
                .schema(stagingSchema)
                .addDatasetFilters(stagingFilter)
                .build();
        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagingDataset).metadataDataset(metadataDataset).build();

        // Load csv data
        loadData(path, datasets.stagingDataset(), 1);

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();

        // Perform ingestion
        ingest(preActionsSqlList, milestoningSqlList, metadataIngestSql, postActionsSql);
    }

    void delete(String dataset, String name) throws IOException, InterruptedException
    {
        String sql = "DROP TABLE IF EXISTS " + String.format("`%s`.`%s`", dataset, name);
        runQueries(Arrays.asList(sql));
    }

    void ingest(List<String> preActionsSqlList, List<String> milestoningSqlList, List<String> metadataIngestSql, List<String> postActionsSql) throws IOException, InterruptedException
    {
        runQueries(preActionsSqlList);
        runQueries(milestoningSqlList);
        runQueries(metadataIngestSql);
        runQueries(postActionsSql);
    }

    void createTable(org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset stagingTable) throws InterruptedException, IOException
    {
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        List<String> sqlList = tableCreationPhysicalPlan.getSqlList();
        runQueries(sqlList);
    }

    private BigQuery getBigQueryConnection() throws IOException
    {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialPath));
        BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();
        return bigquery;
    }

    private void runQueries(List<String> sqlList) throws IOException, InterruptedException
    {
        if (sqlList == null || sqlList.isEmpty())
        {
            return;
        }
        String sqls = String.join(";", sqlList);
        System.out.println("Running: " + sqls);
        BigQuery bigQuery = getBigQueryConnection();
        bigQuery.query(QueryJobConfiguration.newBuilder(sqls).build());
    }

    protected List<Map<String, Object>> runQuery(String sql) throws IOException
    {
        BigQuery bigQuery = getBigQueryConnection();
        BigQueryHelper helper = BigQueryHelper.of(bigQuery);
        return helper.executeQuery(sql);
    }

    public static void assertFileAndTableDataEquals(String[] csvSchema, String csvPath, List<Map<String, Object>> dataFromTable) throws IOException
    {
        List<String[]> lines = readCsvData(csvPath);
        Assertions.assertEquals(lines.size(), dataFromTable.size());

        for (int i = 0; i < lines.size(); i++)
        {
            Map<String, Object> tableRow = dataFromTable.get(i);
            String[] expectedLine = lines.get(i);
            for (int j = 0; j < csvSchema.length; j++)
            {
                String expected = expectedLine[j];
                Object value = tableRow.get(csvSchema[j]);
                if (value instanceof Instant)
                {
                    Instant instant = (Instant) value;
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
                    value = formatter.format(instant);
                }
                String tableData = String.valueOf(value);
                Assertions.assertEquals(expected, tableData);
            }
        }
    }

    void loadData(String path, Dataset stagingDataset, int attempt) throws IOException, InterruptedException
    {
        // Create Staging table
        createTable(stagingDataset);

        attempt++;
        String tableName = stagingDataset.datasetReference().name().get();
        String datasetName = stagingDataset.datasetReference().group().get();
        List<String> schema = stagingDataset.schema().fields().stream().map(field -> field.name()).collect(Collectors.toList());
        BigQuery bigQuery = getBigQueryConnection();

        TableId tableId = TableId.of(datasetName, tableName);
        List<InsertAllRequest.RowToInsert> rows = new ArrayList<>();

        List<String[]> csvLines = readCsvData(path);
        for (String[] line : csvLines)
        {
            int i = 0;
            Map<String, Object> map = new HashMap<>();
            for (String field : schema)
            {
                map.put(field, line[i++]);
            }
            InsertAllRequest.RowToInsert rowToInsert = InsertAllRequest.RowToInsert.of(map);
            rows.add(rowToInsert);
        }

        InsertAllRequest insertAllRequest = InsertAllRequest.newBuilder(tableId).setRows(rows).build();
        try
        {
            bigQuery.insertAll(insertAllRequest);
        }
        catch (Exception e)
        {
            System.out.println("Error occurred: " + e.getMessage());
            if (attempt <= 10)
            {
                // Retry
                Thread.sleep(5000);
                loadData(path, stagingDataset, attempt);
            }
            else
            {
                throw e;
            }
        }
    }

    private static List<String[]> readCsvData(String path) throws IOException
    {
        FileReader fileReader = new FileReader(path);
        CSVReader csvReader = new CSVReader(fileReader);
        List<String[]> lines = csvReader.readAll();
        return lines;
    }
}
