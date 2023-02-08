// Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;

public class ExternalCsvDatasetImportTest extends BaseTest
{

    @Test
    void testImportCsvSql() throws Exception
    {
        String csvPath = "src/test/resources/data/snapshot-milestoning/input/vanilla_case/data_pass1.csv";
        CsvExternalDatasetReference csvDatasetReference = TestUtils.getCsvDatasetReferenceTable(csvPath, TEST_DATABASE, "my_staging_table", TEST_SCHEMA, "staging");

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(csvDatasetReference.getDatasetDefinition(), false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        LogicalPlan csvLoadLogicalPlan = LogicalPlanFactory.getLoadCsvPlan(csvDatasetReference);
        SqlPlan csvLoadPhysicalPlan = transformer.generatePhysicalPlan(csvLoadLogicalPlan);
        List<String> sqlList = csvLoadPhysicalPlan.getSqlList();
        String expectedLoadCsvString = "INSERT INTO \"TEST_DB\".\"TEST\".\"my_staging_table\" " +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\") " +
            "(SELECT \"id\",\"name\",\"income\",\"start_time\",\"expiry_date\",\"digest\" " +
            "FROM CSVREAD('%s'," +
            "'id,name,income,start_time,expiry_date,digest'," +
            "'fieldSeparator=, charset=UTF-8'))";
        expectedLoadCsvString = String.format(expectedLoadCsvString, csvPath);
        Assertions.assertEquals(expectedLoadCsvString, sqlList.get(0));

        validateFileExists(csvPath);
        executor.executePhysicalPlan(csvLoadPhysicalPlan);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST_DB\".\"TEST\".\"my_staging_table\"");
        TestUtils.assertFileAndTableDataEquals(schema, csvPath, tableData);
    }
}
