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

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class GenerateCast3Test extends GenerateCastTestBase
{
    @BeforeClass
    public static void setupSuite() throws IOException, ClassNotFoundException
    {
        setupSuite("meta::pure::changetoken::tests::getVersions3");
    }

    @Test
    public void testUpcast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        String input = "{\n" +
                "  \"version\": \"ftdm:abcdefg123\",\n" +
                "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                "  \"array\": [\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                "      \"existingValue\": \"someValue\",\n" +
                "      \"innerObject\": {\n" +
                "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                "        \"abc\": 123\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        Map<String,Object> jsonNode = mapper.readValue(input, Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\": \"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                        "  \"array\": [\n" +
                        "    {\n" +
                        "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                        "      \"existingValue\": \"someValue\",\n" +
                        "      \"innerObject\": {\n" +
                        "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "        \"abc\": \"123\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n", Map.class); // 123 converted to "123"
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);

        // assert that the input is not mutated
        Assert.assertEquals(mapper.readValue(input, Map.class), jsonNode);
    }

    @Test
    public void testDowncast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        String input = "{\n" +
                "  \"version\": \"ftdm:abcdefg456\",\n" +
                "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                "  \"array\": [\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                "      \"existingValue\": \"someValue\",\n" +
                "      \"innerObject\": {\n" +
                "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                "        \"abc\": \"123\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        Map<String,Object> jsonNode = mapper.readValue(input, Map.class);

        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class).invoke(null, jsonNode, "ftdm:abcdefg123");
//        Map<String,Object> jsonNodeOut = TempGenerated.downcast(jsonNode, "ftdm:abcdefg123");

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\": \"ftdm:abcdefg123\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                        "  \"array\": [\n" +
                        "    {\n" +
                        "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                        "      \"existingValue\": \"someValue\",\n" +
                        "      \"innerObject\": {\n" +
                        "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "        \"abc\": 123\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n", Map.class); // "123" converted to 123
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);

        // assert that the input is not mutated
        Assert.assertEquals(mapper.readValue(input, Map.class), jsonNode);
    }
}
