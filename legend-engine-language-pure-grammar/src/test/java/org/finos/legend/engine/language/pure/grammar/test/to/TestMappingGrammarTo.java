//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.grammar.test.to;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

public class TestMappingGrammarTo
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static void testTo(String protocolResource, String expectedCode)
    {
        try
        {
            PureModelContextData modelData = objectMapper.readValue(Objects.requireNonNull(TestMappingGrammarTo.class.getClassLoader().getResourceAsStream(protocolResource)), PureModelContextData.class);
            String modelCode = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(modelData);
            Assert.assertEquals(expectedCode, modelCode);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSimpleAggregationAwareMapping()
    {
        testTo(
                "SimpleAggregationAwareMapping.json",
                "###Mapping\n" +
                        "Mapping test::map\n" +
                        "(\n" +
                        "  test::DiscountedProduct[PR]: AggregationAware \n" +
                        "  {\n" +
                        "    Views: [\n" +
                        "      (\n" +
                        "        ~modelOperation: {\n" +
                        "          ~canAggregate true,\n" +
                        "          ~groupByFunctions (\n" +
                        "            $this.id\n" +
                        "          ),\n" +
                        "          ~aggregateValues (\n" +
                        "            ( ~mapFn:$this.price , ~aggregateFn: $mapped->sum() )\n" +
                        "          )\n" +
                        "        },\n" +
                        "        ~aggregateMapping: Pure\n" +
                        "        {\n" +
                        "          ~src test::Service\n" +
                        "          price: $src.price,\n" +
                        "          producer: $src.provider\n" +
                        "        }\n" +
                        "      )\n" +
                        "    ],\n" +
                        "    ~mainMapping: Pure\n" +
                        "    {\n" +
                        "      ~src test::DiscountedProduct\n" +
                        "      price: $src.price,\n" +
                        "      producer: $src.producer,\n" +
                        "      discount: $src.discount\n" +
                        "    }\n" +
                        "  }\n" +
                        ")\n"
        );
    }

    @Test
    public void testIncludeDispatch()
    {
        testTo("simpleIncludeMappingWithOutdatedProperty.json",
                "###Mapping\n" +
                "Mapping mapping::simpleModelMapping\n" +
                "(\n" +
                "  include mapping test::mapping::DispatchMapping\n" +
                "  include mapping test::mapping::Mapping\n\n" +
                "  *model::TargetClass[my_mapping_id]: Pure\n" +
                "  {\n" +
                "    ~src model::SourceClass\n" +
                "    name: $src.name\n" +
                "  }\n" +
                ")\n");
    }
}
