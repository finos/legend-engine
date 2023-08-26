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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class GenerateCastMoveExtractTest extends GenerateCastTestBase
{
    @BeforeClass
    public static void setupSuite() throws IOException, ClassNotFoundException
    {
        setupSuiteFromJson("{\n" +
                "  \"@type\": \"meta::pure::changetoken::Versions\",\n" +
                "  \"versions\": [\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::Version\",\n" +
                "      \"version\": \"ftdm:abcdefg123\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::Version\",\n" +
                "      \"version\": \"ftdm:abcdefg456\",\n" +
                "      \"prevVersion\": \"ftdm:abcdefg123\",\n" +
                "      \"changeTokens\": [\n" +
                "        {\n" +
                "          \"@type\": \"meta::pure::changetoken::RenameField\",\n" +
                "          \"oldFieldName\": [\n" +
                "            \"names\",\n" +
                "            \"first\"\n" +
                "          ],\n" +
                "          \"newFieldName\": [\n" +
                "            \"firstName\"\n" +
                "          ],\n" +
                "          \"class\": \"meta::pure::changetoken::tests::SampleClass\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"@type\": \"meta::pure::changetoken::RenameField\",\n" +
                "          \"oldFieldName\": [\n" +
                "            \"names\",\n" +
                "            \"last\"\n" +
                "          ],\n" +
                "          \"newFieldName\": [\n" +
                "            \"lastName\"\n" +
                "          ],\n" +
                "          \"class\": \"meta::pure::changetoken::tests::SampleClass\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"@type\": \"meta::pure::changetoken::RemoveField\",\n" +
                "          \"fieldName\": \"names\",\n" +
                "          \"fieldType\": \"NamesClass[1]\",\n" +
                "          \"defaultValue\": {\n" +
                "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                "            \"value\": \"{\\\"@type\\\":\\\"NamesClass\\\",\\\"middle\\\":\\\"\\\"}\"\n" +
                "          },\n" +
                "          \"safeCast\": true,\n" +
                "          \"class\": \"meta::pure::changetoken::tests::SampleClass\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");
    }

    @Test
    public void testUpcast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String, Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"1d\", \"middle\":\"\", \"last\":\"2d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"3d\", \"middle\":\"\", \"last\":\"4d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"5d\", \"middle\":\"\", \"last\":\"6d\"}}]\n" +
                        "  ],\n" +
                        "  \"names\": {\"@type\":\"NamesClass\", \"first\":\"7d\", \"middle\":\"\", \"last\":\"8d\"}\n" +
                        "}", Map.class);
        Map<String, Object> jsonNodeOut = (Map<String, Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String, Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"1d\", \"lastName\":\"2d\"},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"3d\", \"lastName\":\"4d\"},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"5d\", \"lastName\":\"6d\"}]\n" +
                        "  ],\n" +
                        "  \"firstName\":\"7d\", \"lastName\":\"8d\"\n" +
                        "}", Map.class); // updated version and new default value field added
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testDowncast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"1d\", \"lastName\":\"2d\"},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"3d\", \"lastName\":\"4d\"},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"firstName\":\"5d\", \"lastName\":\"6d\"}]\n" +
                        "  ],\n" +
                        "  \"firstName\":\"7d\", \"lastName\":\"8d\"\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class)
                .invoke(null, jsonNode, "ftdm:abcdefg123");
        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"1d\", \"middle\":\"\", \"last\":\"2d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"3d\", \"middle\":\"\", \"last\":\"4d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"names\": {\"@type\":\"NamesClass\", \"first\":\"5d\", \"middle\":\"\", \"last\":\"6d\"}}]\n" +
                        "  ],\n" +
                        "  \"names\": {\"@type\":\"NamesClass\", \"first\":\"7d\", \"middle\":\"\", \"last\":\"8d\"}\n" +
                        "}", Map.class); // remove default values
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }
}
