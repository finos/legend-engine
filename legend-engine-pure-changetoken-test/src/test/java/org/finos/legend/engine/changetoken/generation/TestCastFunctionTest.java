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
        JsonNode jsonNode = mapper.readTree("{\"version\":\"ftdm:abcdefg123\", \"@type\": \"meta::pure::changetoken::tests::SampleClass\"}");
        JsonNode jsonNodeOut = TestCastFunction2.upcast(jsonNode);
        JsonNode expectedJsonNodeOut = mapper.readTree("{\"version\":\"ftdm:abcdefg456\","
                + "\"@type\": \"meta::pure::changetoken::tests::SampleClass\", "
                + "\"abc\": 100}"); // new default value field added
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
