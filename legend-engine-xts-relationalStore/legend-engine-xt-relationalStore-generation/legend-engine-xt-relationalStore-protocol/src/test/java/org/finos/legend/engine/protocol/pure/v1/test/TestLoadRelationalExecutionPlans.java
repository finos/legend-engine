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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalClassInstantiationExecutionNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class TestLoadRelationalExecutionPlans
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testLoadSingleExecutionPlan() throws Exception
    {
        SingleExecutionPlan singleExecutionPlan = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("singleExecutionPlan.json")), SingleExecutionPlan.class);
        Assert.assertEquals(singleExecutionPlan.rootExecutionNode.executionNodes.size(), 2);
        Assert.assertEquals(((RelationalClassInstantiationExecutionNode) singleExecutionPlan.rootExecutionNode.executionNodes.get(1)).executionNodes.size(), 1);
    }
}
