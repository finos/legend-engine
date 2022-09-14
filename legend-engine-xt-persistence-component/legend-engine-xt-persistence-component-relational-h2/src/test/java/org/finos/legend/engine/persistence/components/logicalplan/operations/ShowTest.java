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
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ShowTest extends BaseTest
{

    @Test
    public void testShowCommand() throws Exception
    {
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        stagingTable = stagingTable.withDatabase("TEST_DB");
        createStagingTable(stagingTable);

        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(stagingTable);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<TabularData> result = executor.executePhysicalPlanAndGetResults(physicalPlan);

        Assertions.assertEquals(1, result.get(0).getData().size());
        Assertions.assertEquals("staging", result.get(0).getData().get(0).get("TABLE_NAME"));

        h2Sink.executeStatement("DROP TABLE TEST_DB.TEST.staging");

        DatasetDefinition mainTable = TestUtils.getBasicMainTable();

        LogicalPlan datasetExistslogicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(mainTable);
        SqlPlan datasetExistsPhysicalPlan = transformer.generatePhysicalPlan(datasetExistslogicalPlan);
        List<TabularData> mainResult = executor.executePhysicalPlanAndGetResults(datasetExistsPhysicalPlan);
        Assertions.assertEquals(0, mainResult.get(0).getData().size());
    }
}
