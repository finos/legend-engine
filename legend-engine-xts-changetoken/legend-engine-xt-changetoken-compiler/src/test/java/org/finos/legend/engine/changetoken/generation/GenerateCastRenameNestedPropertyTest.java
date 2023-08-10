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
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.assertThrows;

public class GenerateCastRenameNestedPropertyTest extends GenerateCastTestBase
{
    @BeforeClass
    public static void setupSuite() throws IOException, ClassNotFoundException
    {
        setupSuite("meta::pure::changetoken::tests::getVersionsRenameNestedProperty");
    }

    @Test
    public void testUpcast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"4d\"}\n" +
                        "}", Map.class); // updated version and new default value field added
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testUpcastType() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"4d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"4d\"}\n" +
                        "}", Map.class); // updated version and new default value field added
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }


    @Test
    public void testUpcastMissing() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"valueOther\":\"4d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"valueOther\":\"4d\"}\n" +
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
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"1d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class)
                .invoke(null, jsonNode, "ftdm:abcdefg123");
        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", Map.class); // remove default values
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testDowncastType() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"1d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class)
                .invoke(null, jsonNode, "ftdm:abcdefg123");
        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"OtherCustom\", \"value\":\"1d\"}\n" +
                        "}", Map.class); // remove default values
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testDowncastMissing() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class)
                .invoke(null, jsonNode, "ftdm:abcdefg123");
        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"valueOther\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", Map.class); // remove default values
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testUpcastExistingTheSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\", \"valueCustom\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\", \"valueCustom\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\", \"valueCustom\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\", \"valueCustom\":\"4d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("upcast", Map.class).invoke(null, jsonNode);

        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"valueCustom\":\"4d\"}\n" +
                        "}", Map.class); // updated version and new default value field added
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testDowncastExistingTheSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\", \"valueCustom\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\", \"valueCustom\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\", \"valueCustom\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\", \"valueCustom\":\"8d\"}\n" +
                        "}", Map.class);
        Map<String,Object> jsonNodeOut = (Map<String,Object>) compiledClass.getMethod("downcast", Map.class, String.class)
                .invoke(null, jsonNode, "ftdm:abcdefg123");
        Map<String,Object> expectedJsonNodeOut = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\"}\n" +
                        "}", Map.class); // remove default values
        Assert.assertEquals(expectedJsonNodeOut, jsonNodeOut);
    }

    @Test
    public void testUpcastExistingTheDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\", \"valueCustom\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\", \"valueCustom\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\", \"valueCustom\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\", \"valueCustom\":\"9d\"}\n" +
                        "}", Map.class);
        Method upcastMethod = compiledClass.getMethod("upcast", Map.class);
        InvocationTargetException re = assertThrows("non-default", InvocationTargetException.class, () -> upcastMethod.invoke(null, jsonNode));
        Assert.assertEquals("Cannot overwrite with different value:9d", re.getCause().getMessage());
   }

    @Test
    public void testDowncastExistingTheDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonNode = mapper.readValue(
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\", \"valueCustom\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\", \"valueCustom\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\", \"valueCustom\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\", \"valueCustom\":\"9d\"}\n" +
                        "}", Map.class);
        Method downcastMethod = compiledClass.getMethod("downcast", Map.class, String.class);
        InvocationTargetException re = assertThrows("non-default", InvocationTargetException.class, () -> downcastMethod.invoke(null, jsonNode, "ftdm:abcdefg123"));
        Assert.assertEquals("Cannot overwrite with different value:8d", re.getCause().getMessage());
    }
}
