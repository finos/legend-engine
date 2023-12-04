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
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class GenerateCastRenameTest extends GenerateCastTestBase
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
                "            \"abc\"\n" +
                "          ],\n" +
                "          \"newFieldName\": [\n" +
                "            \"xyz\"\n" +
                "          ],\n" +
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
        expect(upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastType() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastMissing() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncastType() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::OtherClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::OtherClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncastMissing() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"4d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"3d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"def\": {\"@type\":\"Custom\", \"value\":\"2d\"}}]\n" +
                        "  ],\n" +
                        "  \"def\": {\"@type\":\"Custom\", \"value\":\"1d\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastExistingTheSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"Custom\", \"value\":\"4d\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncastExistingTheSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"8d\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastExistingTheDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"1d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"2d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"9d\"}\n" +
                        "}"),
                "Cannot overwrite with different value:{@type=Custom, value=9d}");
    }

    @Test
    public void testDowncastExistingTheDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"5d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"5d\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"6d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"6d\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"Custom\", \"value\":\"7d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"7d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"Custom\", \"value\":\"8d\"}, \"xyz\": {\"@type\":\"Custom\", \"value\":\"9d\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "Cannot overwrite with different value:{@type=Custom, value=8d}");
    }
}
