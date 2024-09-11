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

import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;

@Disabled
public class AppendOnlyExecutorTest extends BigQueryEndToEndTest
{

    @Test
    public void testMilestoning() throws IOException, InterruptedException
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .filterExistingRecords(true)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField("audit_ts").build())
                .batchIdField("batch_id")
                .build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");

        // Pass 1
        System.out.println("--------- Batch 1 started ------------");
        String pathPass1 = "src/test/resources/input/data_pass1.csv";
        DatasetFilter stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-01 00:00:00");
        IngestorResult result = ingestViaExecutor(ingestMode, stagingSchema, stagingFilter, pathPass1, fixedClock_2000_01_01);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`main` order by id asc");
        String expectedPath = "src/test/resources/expected/append/data_pass1.csv";
        String [] schema = new String[] {"id", "name", "amount", "biz_date", "digest", "insert_ts", "audit_ts", "batch_id"};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        Map<StatisticName, Object> map = result.statisticByName();

        long incomingRecords = (long) result.statisticByName().get(INCOMING_RECORD_COUNT);
        long rowsInserted = (long) result.statisticByName().get(ROWS_INSERTED);
        Assertions.assertEquals(3, incomingRecords);
        Assertions.assertEquals(3, rowsInserted);

        // Pass 2
        System.out.println("--------- Batch 2 started ------------");
        String pathPass2 = "src/test/resources/input/data_pass2.csv";
        stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-02 00:00:00");
        result = ingestViaExecutor(ingestMode, stagingSchema, stagingFilter, pathPass2, fixedClock_2000_01_02);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by id asc, insert_ts");
        expectedPath = "src/test/resources/expected/append/data_pass2.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
        incomingRecords = (long) result.statisticByName().get(INCOMING_RECORD_COUNT);
        rowsInserted = (long) result.statisticByName().get(ROWS_INSERTED);
        Assertions.assertEquals(3, incomingRecords);
        Assertions.assertEquals(2, rowsInserted);
    }

    @Test
    public void testMilestoningWithDigestGeneration() throws IOException, InterruptedException
    {
        Map<DataType, String> typeConversionUdfs = new HashMap<>();
        typeConversionUdfs.put(DataType.INT, "demo.numericToString");
        typeConversionUdfs.put(DataType.INTEGER, "demo.numericToString");
        typeConversionUdfs.put(DataType.DATE, "demo.dateToString");
        typeConversionUdfs.put(DataType.TIMESTAMP, "demo.timestampToString");

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder()
                .digestUdfName("demo.LAKEHOUSE_MD5")
                .putAllTypeConversionUdfNames(typeConversionUdfs)
                .digestField(digestName).build())
            .auditing(DateTimeAuditing.builder().dateTimeField("audit_ts").build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .batchIdField("batch_id")
            .build();

        SchemaDefinition stagingSchema = SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(amount)
            .addFields(bizDate)
            .addFields(insertTimestamp)
            .build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");

        // Register UDF
        runQuery("DROP FUNCTION IF EXISTS demo.numericToString;");
        runQuery("DROP FUNCTION IF EXISTS demo.timestampToString;");
        runQuery("DROP FUNCTION IF EXISTS demo.dateToString;");
        runQuery("DROP FUNCTION IF EXISTS demo.stringifyArr;");
        runQuery("DROP FUNCTION IF EXISTS demo.LAKEHOUSE_MD5;");
        runQuery("CREATE FUNCTION demo.numericToString(value NUMERIC)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.timestampToString(value TIMESTAMP)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.dateToString(value DATE)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.stringifyArr(args ARRAY<STRING>)\n" +
            "            RETURNS STRING\n" +
            "            LANGUAGE js AS \"\"\"\n" +
            "            let output = \"\"; \n" +
            "            for (const [index, element] of args.entries()) { output += args[index]; }\n" +
            "            return output;\n" +
            "            \"\"\"; \n");
        runQuery("CREATE FUNCTION demo.LAKEHOUSE_MD5(args ARRAY<STRING>)\n" +
            "AS (\n" +
            "  TO_HEX(MD5(demo.stringifyArr(args)))\n" +
            ");\n");

        // Pass 1
        System.out.println("--------- Batch 1 started ------------");
        String pathPass1 = "src/test/resources/input/digest_generation/data_pass3.csv";
        DatasetFilter stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-01 00:00:00");
        IngestorResult result = ingestViaExecutor(ingestMode, stagingSchema, stagingFilter, pathPass1, fixedClock_2000_01_01);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`main` order by name asc");
        String expectedPath = "src/test/resources/expected/append/digest_generation/data_pass3.csv";
        String [] schema = new String[] {"id", "name", "amount", "biz_date", "insert_ts", "digest", "audit_ts", "batch_id"};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
        long incomingRecords = (long) result.statisticByName().get(INCOMING_RECORD_COUNT);
        Assertions.assertEquals(3, incomingRecords);

        // Pass 2
        System.out.println("--------- Batch 2 started ------------");
        String pathPass2 = "src/test/resources/input/digest_generation/data_pass4.csv";
        stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-02 00:00:00");
        result = ingestViaExecutor(ingestMode, stagingSchema, stagingFilter, pathPass2, fixedClock_2000_01_02);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by name asc, insert_ts");
        expectedPath = "src/test/resources/expected/append/digest_generation/data_pass4.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
        incomingRecords = (long) result.statisticByName().get(INCOMING_RECORD_COUNT);
        Assertions.assertEquals(3, incomingRecords);
    }
}
