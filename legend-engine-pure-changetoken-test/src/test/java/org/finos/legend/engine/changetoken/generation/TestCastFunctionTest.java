//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.generated.meta.pure.changetoken.cast_generation.TestCastFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCastFunctionTest
{
    @Test
    public void testUpcast() throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\"},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\"}, \n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\"}]\n" +
                        "  ]\n" +
                        "}");
        JsonNode jsonNodeOut = TestCastFunction.upcast(jsonNode);
        JsonNode expectedJsonNodeOut = mapper.readTree(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": 100},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": 100},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": 100}]\n" +
                        "  ],\n" +
                        "  \"abc\": 100\n" +
                        "}"); // updated version and new default value field added
        assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testDowncast()
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.createObjectNode();
        JsonNode jsonNodeOut = TestCastFunction2.downcast(jsonNode, "ftdm:<version>");
        assertEquals(jsonNode, jsonNodeOut);
    }
}
