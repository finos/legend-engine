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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GenerateCastUtilTest extends GenerateCastTestBase
{
    @BeforeClass
    public static void setupSuite() throws IOException, ClassNotFoundException
    {
        setupSuite("meta::pure::changetoken::tests::getVersions3");
    }

    @Test
    public void testResolvePath() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method m = compiledClass.getMethod("resolvePath", List.class, String.class);

        // note that "0" is cast to int in resolveRelativeReference or setRelativeReference
        Assert.assertArrayEquals(
                Arrays.asList("a", "b", 1, "c", "0").toArray(),
                ((List<Object>)m.invoke(null, Arrays.asList("a", "b", 1, "c", 2), "../0")).toArray());

        InvocationTargetException ex = Assert.assertThrows(InvocationTargetException.class,
                () -> m.invoke(null, Arrays.asList("a", "b", 1, "c", 2), "../../../../../../x"));
        Assert.assertEquals("Relative reference escapes root (a/b/1/c/2) at index 5 of ../../../../../../x", ex.getCause().getMessage());
    }

    @Test
    public void testResolveRelativeReference() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method m = compiledClass.getMethod("resolveRelativeReference", Map.class, List.class, String.class);

        ObjectMapper mapper = new ObjectMapper();
        String input = "{\n" +
                "  \"version\": \"ftdm:abcdefg123\",\n" +
                "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                "  \"array\": [\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                "      \"existingValue\": \"someValue\",\n" +
                "      \"innerObject\": {\n" +
                "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        Map<String,Object> jsonNode = mapper.readValue(input, Map.class);

        Assert.assertEquals("someValue",
                m.invoke(null, jsonNode,
                        Arrays.asList("array", 0, "innerObject", "@type"),
                        "../../existingValue"));

        Assert.assertEquals("meta::pure::changetoken::tests::SampleClass",
                m.invoke(null, jsonNode,
                        Collections.emptyList(),
                        "array/0/innerObject/@type"));

        InvocationTargetException ex = Assert.assertThrows(InvocationTargetException.class,
                () -> m.invoke(null, jsonNode,
                        Collections.emptyList(),
                        "array/blah"));
        Assert.assertEquals("java.lang.NumberFormatException: For input string: \"blah\": at index 1 of array/blah", ex.getCause().getMessage());
    }

    @Test
    public void testSetRelativeReference() throws JsonProcessingException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method m = compiledClass.getMethod("setRelativeReference", Map.class, List.class, String.class, Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        String input = "{\n" +
                "  \"version\": \"ftdm:abcdefg123\",\n" +
                "  \"@type\": \"meta::pure::changetoken::tests::SomeClassWithAnArray\",\n" +
                "  \"array\": [\n" +
                "    {\n" +
                "      \"@type\": \"meta::pure::changetoken::tests::OuterClass\",\n" +
                "      \"existingValue\": \"someValue\",\n" +
                "      \"innerObject\": {\n" +
                "        \"@type\": \"meta::pure::changetoken::tests::SampleClass\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        Map<String,Object> jsonNode = mapper.readValue(input, Map.class);

        m.invoke(null, jsonNode,
                Arrays.asList("array", 0, "innerObject", "@type"), "../newValue", 123);
        Assert.assertEquals(123,
                compiledClass.getMethod("resolveRelativeReference", Map.class, List.class, String.class).invoke(null, jsonNode,
                        Arrays.asList("array", 0, "innerObject", "@type"),
                        "../newValue"));

        {
            InvocationTargetException ex = Assert.assertThrows(InvocationTargetException.class,
                    () -> m.invoke(null, jsonNode,
                            Collections.emptyList(),
                            "array/blah", 456));
            Assert.assertEquals("java.lang.NumberFormatException: For input string: \"blah\": at index 1 of array/blah", ex.getCause().getMessage());
        }

        {
            InvocationTargetException ex = Assert.assertThrows(InvocationTargetException.class,
                    () -> m.invoke(null, jsonNode,
                            Collections.emptyList(),
                            "0/blah", 456));
            Assert.assertEquals("java.lang.RuntimeException: No such element: at index 0 of 0/blah", ex.getCause().getMessage());
        }
    }
}
