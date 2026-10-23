// Copyright 2026 Goldman Sachs
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
import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNodeHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestGenerationTreeNodeSerialization
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testSingleGenerationElementNode() throws Exception
    {
        GenerationTreeNode node = new GenerationTreeNode();
        node.id = "model::myElement";
        node.generationElement = "model::myElement";

        // no generationElements key, so the serialized form of an existing generation node does not change
        JsonAssert.assertJsonEquals("{\"id\":\"model::myElement\",\"generationElement\":\"model::myElement\",\"sourceInformation\":null}", objectMapper.writeValueAsString(node));
    }

    @Test
    public void testParallelGenerationElementsNode() throws Exception
    {
        GenerationTreeNode node = new GenerationTreeNode();
        node.id = "myNode";
        node.generationElements = Arrays.asList("model::a", "model::b");

        // generationElement stays null, so a reader which does not understand groups fails rather than generating one element out of two
        JsonAssert.assertJsonEquals("{\"id\":\"myNode\",\"generationElement\":null,\"generationElements\":[\"model::a\",\"model::b\"],\"sourceInformation\":null}", objectMapper.writeValueAsString(node));
    }

    @Test
    public void testParallelGenerationElementsNodeRoundTrip() throws Exception
    {
        GenerationTreeNode node = objectMapper.readValue("{\"id\":\"myNode\",\"generationElements\":[\"model::a\",\"model::b\"]}", GenerationTreeNode.class);

        Assert.assertNull(node.generationElement);
        Assert.assertEquals(Arrays.asList("model::a", "model::b"), GenerationTreeNodeHelper.generationElements(node));
    }
}
