// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CreateAndPopulateTempTableExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalSaveNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

public class TestLoadRelationalExecutionPlans
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testLoadSingleExecutionPlan() throws Exception
    {
        SingleExecutionPlan singleExecutionPlan = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("singleExecutionPlan.json")), SingleExecutionPlan.class);
        List<ExecutionNode> nodes = singleExecutionPlan.rootExecutionNode.executionNodes;
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(1, nodes.get(1).executionNodes.size());
    }

    /**
     * Before the connection parser stripped them, the grammar's quotes around a zone id were kept, so a plan generated
     * then names its connections' zone as 'US/Arizona', quotes and all. The quotes come off as the plan is read, so
     * every node that reads or writes dates through its connection finds the zone the connection names.
     */
    @Test
    public void testLoadExecutionPlanWithTimeZoneQuotedByOldParser() throws Exception
    {
        SingleExecutionPlan singleExecutionPlan = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("executionPlanWithTimeZoneQuotedByOldParser.json")), SingleExecutionPlan.class);
        List<ExecutionNode> nodes = singleExecutionPlan.rootExecutionNode.executionNodes;
        Assert.assertEquals(4, nodes.size());
        Assert.assertEquals("US/Arizona", ((CreateAndPopulateTempTableExecutionNode) nodes.get(0)).getDatabaseTimeZone());
        Assert.assertEquals("US/Arizona", ((SQLExecutionNode) nodes.get(1)).getDatabaseTimeZone());
        Assert.assertEquals("US/Arizona", ((RelationalSaveNode) nodes.get(2)).getDatabaseTimeZone());
        Assert.assertEquals("US/Arizona", ((RelationalExecutionNode) nodes.get(3)).getDatabaseTimeZone());
    }
}
