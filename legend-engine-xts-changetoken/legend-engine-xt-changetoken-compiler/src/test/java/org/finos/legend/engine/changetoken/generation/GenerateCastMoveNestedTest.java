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

public class GenerateCastMoveNestedTest extends GenerateCastTestBase
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
                "            \"abc\",\n" +
                "            \"value\"\n" +
                "          ],\n" +
                "          \"newFieldName\": [\n" +
                "            \"xyz\",\n" +
                "            \"value\"\n" +
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
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncast() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}\n");
    }

    @Test
    public void testDowncastSame() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        expect(downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}\n");
    }

    @Test
    public void testUpcastDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": {\"@type\":\"String\", \"value\":\"5d\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}"),
                "Cannot overwrite with different value:5d");
    }

    @Test
    public void testDowncastDifferent() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\": {\"@type\":\"String\", \"value\":\"6d\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "Cannot overwrite with different value:6d");
    }

    @Test
    public void testUpcastInvalidDestination() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"3d\"}, \"xyz\": \"invalid\"}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}"),
                "Destination is not a map: xyz");
    }

    @Test
    public void testDowncastInvalidDestination() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"3d\"}, \"abc\":  \"invalid\"}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "Destination is not a map: abc");
    }

    @Test
    public void testUpcastInvalidSource() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> upcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg123\", \n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"1d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": {\"@type\":\"String\", \"value\":\"2d\"}, \"xyz\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"abc\": \"invalid\", \"xyz\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"abc\": {\"@type\":\"String\", \"value\":\"4d\"}, \"xyz\": {\"@type\":\"String\"}\n" +
                        "}"),
                "Source is not a map: abc");
    }

    @Test
    public void testDowncastInvalidSource() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        exception(() -> downcast("{\n" +
                        "  \"version\":\"ftdm:abcdefg456\",\n" +
                        "  \"@type\": \"meta::pure::changetoken::tests::SampleClass\",\n" +
                        "  \"innerObject\": {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"1d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "  \"innerNestedArray\":[\n" +
                        "    {\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": {\"@type\":\"String\", \"value\":\"2d\"}, \"abc\": {\"@type\":\"String\"}},\n" +
                        "    [{\"@type\": \"meta::pure::changetoken::tests::SampleClass\", \"xyz\": \"invalid\", \"abc\": {\"@type\":\"String\"}}]\n" +
                        "  ],\n" +
                        "  \"xyz\": {\"@type\":\"String\", \"value\":\"4d\"}, \"abc\": {\"@type\":\"String\"}\n" +
                        "}", "ftdm:abcdefg123"),
                "Source is not a map: xyz");
    }
}
