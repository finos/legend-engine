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
import com.google.cloud.bigquery.*;
import com.opencsv.CSVReader;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BigQueryEndToEndTest
{

    private String projectId = "blueplayground";
    private String datasetName = "mydb";
    private String credentialPath = "/Users/ashutosh/Downloads/blueplayground-85f1ebcae766.json";

    void delete(DatasetDefinition datasetDefinition) throws IOException, InterruptedException
    {
        String sql = "DROP TABLE IF EXISTS " + String.format("`%s`.`%s`", datasetDefinition.database().get(), datasetDefinition.name());
        System.out.println(sql);
        runSqlOnBigQuery(Arrays.asList(sql));
    }

    void ingest(List<String> preActionsSqlList, List<String> milestoningSqlList, List<String> metadataIngestSql, List<String> postActionsSql) throws IOException, InterruptedException
    {
        runSqlOnBigQuery(preActionsSqlList);
        runSqlOnBigQuery(milestoningSqlList);
        runSqlOnBigQuery(metadataIngestSql);
        runSqlOnBigQuery(postActionsSql);
    }

    void createTable(org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset stagingTable) throws InterruptedException, IOException
    {
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        List<String> sqlList = tableCreationPhysicalPlan.getSqlList();
        runSqlOnBigQuery(sqlList);
    }

    private void runSqlOnBigQuery(List<String> sqlList) throws IOException, InterruptedException
    {
        if (sqlList == null || sqlList.isEmpty())
        {
            return;
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialPath));
        BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();
        String sqls = String.join(";", sqlList);
        bigquery.query(QueryJobConfiguration.newBuilder(sqls).build());
    }

    void loadData(String path, DatasetDefinition stagingDataset, int attempt) throws IOException, InterruptedException
    {
        attempt++;
        String tableName = stagingDataset.name();
        List<String> schema = stagingDataset.schema().fields().stream().map(field -> field.name()).collect(Collectors.toList());
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialPath));
        BigQuery bigquery = BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();

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
            InsertAllResponse response = bigquery.insertAll(insertAllRequest);
        }
        catch (Exception e)
        {
            System.out.println("Error occured " + e.getMessage());
            if (attempt <= 100)
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
