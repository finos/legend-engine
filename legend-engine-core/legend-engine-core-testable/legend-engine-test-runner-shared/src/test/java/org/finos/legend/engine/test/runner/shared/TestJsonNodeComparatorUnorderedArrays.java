// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.test.runner.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestJsonNodeComparatorUnorderedArrays
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonNodeComparator comparator = JsonNodeComparator.NULL_MISSING_EQUIVALENT_AND_UNORDERED_ARRAYS;

    @Test
    public void testArrayOrderDoesNotMatterForPrimitives() throws IOException
    {
        assertEqualUnordered("[1, 2, 3]", "[3, 2, 1]");
        assertEqualUnordered("[\"a\", \"b\", \"c\"]", "[\"c\", \"a\", \"b\"]");
        assertEqualUnordered("[true, false, true]", "[true, true, false]");
    }

    @Test
    public void testArrayOrderDoesNotMatterForObjects() throws IOException
    {
        assertEqualUnordered(
                "[{\"id\":1,\"name\":\"a\"},{\"id\":2},{\"id\":3,\"name\":\"c\"}]",
                "[{\"name\":\"c\",\"id\":3},{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":null}]");
    }

    @Test
    public void testNestedArrayOrderDoesNotMatter() throws IOException
    {
        assertEqualUnordered(
                "{\"rows\":[{\"tags\":[\"x\",\"y\"]},{\"tags\":[\"a\",\"b\"]}]}",
                "{\"rows\":[{\"tags\":[\"b\",\"a\"]},{\"tags\":[\"y\",\"x\"]}]}");
    }

    @Test
    public void testMultisetSemantics() throws IOException
    {
        assertNotEqualUnordered("[1, 1, 2]", "[1, 2, 2]");
        assertNotEqualUnordered("[1, 2, 3]", "[1, 2, 3, 3]");
    }

    @Test
    public void testDifferentContentsStillFail() throws IOException
    {
        assertNotEqualUnordered("[1, 2, 3]", "[1, 2, 4]");
        assertNotEqualUnordered("[1, 2]", "[1, 2, 3]");
        assertNotEqualUnordered(
                "[{\"id\":1},{\"id\":2}]",
                "[{\"id\":1},{\"id\":3}]");
    }

    @Test
    public void testEmptyArrays() throws IOException
    {
        assertEqualUnordered("[]", "[]");
        assertNotEqualUnordered("[]", "[1]");
    }

    private void assertEqualUnordered(String json1, String json2) throws IOException
    {
        JsonNode n1 = this.objectMapper.readTree(json1);
        JsonNode n2 = this.objectMapper.readTree(json2);
        Assert.assertEquals("Expected equal (order-insensitive):\n  " + json1 + "\n  " + json2,
                0, this.comparator.compare(n1, n2));
        Assert.assertEquals("Comparison must be symmetric (== 0):\n  " + json2 + "\n  " + json1,
                0, this.comparator.compare(n2, n1));
    }

    private void assertNotEqualUnordered(String json1, String json2) throws IOException
    {
        JsonNode n1 = this.objectMapper.readTree(json1);
        JsonNode n2 = this.objectMapper.readTree(json2);
        Assert.assertNotEquals("Expected not equal (order-insensitive):\n  " + json1 + "\n  " + json2,
                0, this.comparator.compare(n1, n2));
    }
}


