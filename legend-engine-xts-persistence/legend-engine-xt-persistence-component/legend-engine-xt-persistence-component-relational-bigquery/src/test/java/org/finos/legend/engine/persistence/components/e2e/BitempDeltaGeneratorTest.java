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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@Disabled
public class BitempDeltaGeneratorTest extends BigQueryEndToEndTest
{
    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    public void testMilestoning() throws Exception
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(startDateTimeName)
                        .dateTimeThruName(endDateTimeName)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(dateTimeName)
                                .build())
                        .build())
                .build();

        // Clean up
        delete("demo", "main");
        delete("demo", "staging");
        delete("demo", "batch_metadata");

        // Pass 1
        System.out.println("--------- Batch 1 started ------------");
        String pathPass1 = "src/test/resources/input/bitemp_delta/data_pass1.csv";
        DatasetFilter stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 1250000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass1, fixedClock_2000_01_01);

        // Verify
        List<Map<String, Object>> tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        String expectedPath = "src/test/resources/expected/bitemp_delta/data_pass1.csv";
        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 2
        System.out.println("--------- Batch 2 started ------------");
        String pathPass2 = "src/test/resources/input/bitemp_delta/data_pass2.csv";
        stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 124000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass2, fixedClock_2000_01_02);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        expectedPath = "src/test/resources/expected/bitemp_delta/data_pass2.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 3
        System.out.println("--------- Batch 3 started ------------");
        String pathPass3 = "src/test/resources/input/bitemp_delta/data_pass3.csv";
        stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 120000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass3, fixedClock_2000_01_03);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        expectedPath = "src/test/resources/expected/bitemp_delta/data_pass3.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 4
        System.out.println("--------- Batch 4 started ------------");
        String pathPass4 = "src/test/resources/input/bitemp_delta/data_pass4.csv";
        stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 122000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass4, fixedClock_2000_01_04);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        expectedPath = "src/test/resources/expected/bitemp_delta/data_pass4.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 5
        System.out.println("--------- Batch 5 started ------------");
        String pathPass5 = "src/test/resources/input/bitemp_delta/data_pass5.csv";
        stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 110000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass5, fixedClock_2000_01_05);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        expectedPath = "src/test/resources/expected/bitemp_delta/data_pass5.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);

        // Pass 6
        System.out.println("--------- Batch 6 started ------------");
        String pathPass6 = "src/test/resources/input/bitemp_delta/data_pass6.csv";
        stagingFilter = DatasetFilter.of(balanceName, FilterType.EQUAL_TO, 110000);
        ingestViaGenerator(ingestMode, bitempStagingSchema, stagingFilter, pathPass6, fixedClock_2000_01_06);

        // Verify
        tableData = runQuery("select * from `demo`.`main` order by " + batchIdInName + ", " + batchIdOutName + ", " + balanceName + " asc");
        expectedPath = "src/test/resources/expected/bitemp_delta/data_pass6.csv";
        assertFileAndTableDataEquals(schema, expectedPath, tableData);
    }
}
