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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.alterColumn;
import static org.finos.legend.engine.persistence.components.TestUtils.alterColumnName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeChanged;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.name;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

class AlterTest extends BaseTest
{
    private String basePath = "src/test/resources/data/alter-table/";

    @Test
    void testAlterTableAddColumn() throws Exception
    {
        // Prepare main table
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(mainTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
        String inputPath = basePath + "input/add_data_pass.csv";
        insertMainData(inputPath);

        // Generate and execute schema evolution plan
        Operation add = Alter.of(mainTable, Alter.AlterOperation.ADD, alterColumn, Optional.empty());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(add).build();
        SqlPlan schemaEvolutionPhysicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        executor.executePhysicalPlan(schemaEvolutionPhysicalPlan);

        // Verify the new schema
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        List<String> expectedNewSchema = Arrays.asList(idName, nameName, incomeName, startTimeName, expiryDateName, digestName, alterColumnName);
        String expectedPath = basePath + "expected/add_expected_pass.csv";
        TestUtils.assertTableColumnsEquals(expectedNewSchema, actualTableData);
        TestUtils.assertFileAndTableDataEquals(expectedNewSchema.toArray(new String[]{}), expectedPath, actualTableData);
    }

    @Test
    void testAlterTableChangeDataType() throws Exception
    {
        // Prepare main table
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(mainTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
        String inputPath = basePath + "input/change_type_data_pass.csv";
        insertMainData(inputPath);

        // Assert column is of type BIGINT before operation
        String dataType = TestUtils.getCheckDataTypeFromTableSql(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName);
        Assertions.assertEquals("BIGINT", dataType);

        // Generate and execute schema evolution plan
        Operation changeDataType = Alter.of(mainTable, Alter.AlterOperation.CHANGE_DATATYPE, incomeChanged, Optional.empty());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(changeDataType).build();
        SqlPlan schemaEvolutionPhysicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        executor.executePhysicalPlan(schemaEvolutionPhysicalPlan);

        // Verify the new schema
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        List<String> expectedNewSchema = Arrays.asList(idName, nameName, incomeName, startTimeName, expiryDateName, digestName);
        String expectedPath = basePath + "expected/change_type_expected_pass.csv";
        TestUtils.assertTableColumnsEquals(expectedNewSchema, actualTableData);
        TestUtils.assertFileAndTableDataEquals(expectedNewSchema.toArray(new String[]{}), expectedPath, actualTableData);

        // Assert column is of type INTEGER after operation
        dataType = TestUtils.getCheckDataTypeFromTableSql(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName);
        Assertions.assertEquals("INTEGER", dataType);
    }

    @Test
    void testAlterTableNullableColumn() throws Exception
    {
        // Prepare main table
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(mainTable, true);

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
        String inputPath = basePath + "input/nullable_column_data_pass.csv";
        insertMainData(inputPath);

        // Assert column is not nullable before operation
        List<Map<String, Object>> result = h2Sink.executeQuery(TestUtils.getCheckIsNullableFromTableSql(mainTableName, nameName));
        Assertions.assertEquals("NO", result.get(0).get("IS_NULLABLE"));

        // Generate and execute schema evolution plan
        Operation nullableColumn = Alter.of(mainTable, Alter.AlterOperation.NULLABLE_COLUMN, name, Optional.empty());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(nullableColumn).build();
        SqlPlan schemaEvolutionPhysicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        executor.executePhysicalPlan(schemaEvolutionPhysicalPlan);

        // Verify the new schema
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        List<String> expectedNewSchema = Arrays.asList(idName, nameName, incomeName, startTimeName, expiryDateName, digestName);
        String expectedPath = basePath + "expected/nullable_column_expected_pass.csv";
        TestUtils.assertTableColumnsEquals(expectedNewSchema, actualTableData);
        TestUtils.assertFileAndTableDataEquals(expectedNewSchema.toArray(new String[]{}), expectedPath, actualTableData);

        // Assert column is nullable after operation
        result = h2Sink.executeQuery(TestUtils.getCheckIsNullableFromTableSql(mainTableName, nameName));
        Assertions.assertEquals("YES", result.get(0).get("IS_NULLABLE"));
    }

    private void insertMainData(String path) throws Exception
    {
        String loadSql = "INSERT INTO \"TEST\".\"main\"(id, name, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", INT), CONVERT(\"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE) ,  \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }
}