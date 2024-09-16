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
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Disabled
public class AppendOnlyGeneratorTest extends BigQueryEndToEndTest
{

    @Test
    public void testMilestoning() throws IOException, InterruptedException
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField("digest").build())
                .filterExistingRecords(true)
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
        ingestViaGenerator(ingestMode, stagingSchema, stagingFilter, pathPass1, fixedClock_2000_01_01);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`main` order by id asc");
        String expectedPath = "src/test/resources/expected/append/data_pass1.csv";
        String [] schema = new String[] {"id", "name", "amount", "biz_date", "digest", "insert_ts", "audit_ts", "batch_id"};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 2
        System.out.println("--------- Batch 2 started ------------");
        String pathPass2 = "src/test/resources/input/data_pass2.csv";
        stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-02 00:00:00");
        ingestViaGenerator(ingestMode, stagingSchema, stagingFilter, pathPass2, fixedClock_2000_01_02);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by id asc, insert_ts");
        expectedPath = "src/test/resources/expected/append/data_pass2.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
    }

    @Test
    public void testMilestoningWithDigestGenerationWithFieldsToExclude() throws IOException, InterruptedException
    {
        Map<DataType, String> typeConversionUdfs = new HashMap<>();
        typeConversionUdfs.put(DataType.INTEGER, "demo.numericToString");

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder()
                .digestUdfName("demo.LAKEHOUSE_MD5")
                .putAllTypeConversionUdfNames(typeConversionUdfs)
                .digestField(digestName)
                .addAllFieldsToExcludeFromDigest(Arrays.asList(bizDate.name(), insertTimestamp.name())).build())
            .auditing(NoAuditing.builder().build())
            .batchIdField("batch_id")
            .build();

        SchemaDefinition stagingSchema = SchemaDefinition.builder()
            .addFields(nameNonPk)
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
        runQuery("DROP FUNCTION IF EXISTS demo.stringifyArr;");
        runQuery("DROP FUNCTION IF EXISTS demo.LAKEHOUSE_MD5;");
        runQuery("CREATE FUNCTION demo.numericToString(value NUMERIC)\n" +
            "AS (\n" +
            "  CAST(value AS STRING)\n" +
            ");\n");
        runQuery("CREATE FUNCTION demo.stringifyArr(arr1 ARRAY<STRING>, arr2 ARRAY<STRING>)\n" +
            "            RETURNS STRING\n" +
            "            LANGUAGE js AS \"\"\"\n" +
            "            let output = \"\"; \n" +
            "            for (const [index, element] of arr1.entries()) { output += arr1[index]; output += arr2[index]; }\n" +
            "            return output;\n" +
            "            \"\"\"; \n");
        runQuery("CREATE FUNCTION demo.LAKEHOUSE_MD5(arr1 ARRAY<STRING>, arr2 ARRAY<STRING>)\n" +
            "AS (\n" +
            "  TO_HEX(MD5(demo.stringifyArr(arr1, arr2)))\n" +
            ");\n");

        // Pass 1
        System.out.println("--------- Batch 1 started ------------");
        String pathPass1 = "src/test/resources/input/digest_generation/data_pass1.csv";
        DatasetFilter stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-01 00:00:00");
        ingestViaGenerator(ingestMode, stagingSchema, stagingFilter, pathPass1, fixedClock_2000_01_01);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`main` order by name asc");
        String expectedPath = "src/test/resources/expected/append/digest_generation/data_pass1.csv";
        String [] schema = new String[] {"name", "amount", "biz_date", "insert_ts", "digest", "batch_id"};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 2
        System.out.println("--------- Batch 2 started ------------");
        String pathPass2 = "src/test/resources/input/digest_generation/data_pass2.csv";
        stagingFilter = DatasetFilter.of("insert_ts", FilterType.EQUAL_TO, "2023-01-02 00:00:00");
        ingestViaGenerator(ingestMode, stagingSchema, stagingFilter, pathPass2, fixedClock_2000_01_02);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by name asc, insert_ts");
        expectedPath = "src/test/resources/expected/append/digest_generation/data_pass2.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
    }
}
