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
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class GenerateCastUtilTest
{
    @Test
    public void testResolvePath()
    {
        // note that "0" is cast to int in resolveRelativeReference or setRelativeReference
        Assert.assertArrayEquals(
                Arrays.asList("a", "b", 1, "c", "0").toArray(),
                GenerateCastUtil.resolvePath(Arrays.asList("a", "b", 1, "c", 2), "../0").toArray());

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> GenerateCastUtil.resolvePath(Arrays.asList("a", "b", 1, "c", 2), "../../../../../../x"));
        Assert.assertEquals("Relative reference escapes root (a/b/1/c/2) at index 5 of ../../../../../../x", ex.getMessage());
    }

    @Test
    public void testResolveRelativeReference() throws JsonProcessingException
    {
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
                GenerateCastUtil.resolveRelativeReference(jsonNode,
                        Arrays.asList("array", 0, "innerObject", "@type"),
                        "../../existingValue"));

        Assert.assertEquals("meta::pure::changetoken::tests::SampleClass",
                GenerateCastUtil.resolveRelativeReference(jsonNode,
                        Collections.emptyList(),
                        "array/0/innerObject/@type"));

        RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                () -> GenerateCastUtil.resolveRelativeReference(jsonNode,
                        Collections.emptyList(),
                        "array/blah"));
        Assert.assertEquals("java.lang.NumberFormatException: For input string: \"blah\": at index 1 of array/blah", ex.getMessage());
    }

    @Test
    public void testSetRelativeReference() throws JsonProcessingException
    {
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

        GenerateCastUtil.setRelativeReference(jsonNode,
                Arrays.asList("array", 0, "innerObject", "@type"), "../newValue", 123);
        Assert.assertEquals(123,
                GenerateCastUtil.resolveRelativeReference(jsonNode,
                        Arrays.asList("array", 0, "innerObject", "@type"),
                        "../newValue"));

        {
            RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                    () -> GenerateCastUtil.setRelativeReference(jsonNode,
                            Collections.emptyList(),
                            "array/blah", 456));
            Assert.assertEquals("java.lang.NumberFormatException: For input string: \"blah\": at index 1 of array/blah", ex.getMessage());
        }

        {
            RuntimeException ex = Assert.assertThrows(RuntimeException.class,
                    () -> GenerateCastUtil.setRelativeReference(jsonNode,
                            Collections.emptyList(),
                            "0/blah", 456));
            Assert.assertEquals("java.lang.RuntimeException: No such element: at index 0 of 0/blah", ex.getMessage());
        }
    }
}
