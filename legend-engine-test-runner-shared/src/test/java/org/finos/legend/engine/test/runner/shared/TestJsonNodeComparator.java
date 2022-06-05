// Copyright 2021 Goldman Sachs
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
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

public abstract class TestJsonNodeComparator
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testNull() throws IOException
    {
        assertComparesEqual(NullNode.getInstance(), NullNode.getInstance());
        assertCompares(getExpectedNullMissingComparison(), NullNode.getInstance(), MissingNode.getInstance());
        assertCompares(getExpectedMissingNullComparison(), MissingNode.getInstance(), NullNode.getInstance());
        assertComparesEqual(MissingNode.getInstance(), MissingNode.getInstance());

        assertCompares(getExpectedMissingNullComparison(), null, NullNode.getInstance());
        assertComparesEqual(null, MissingNode.getInstance());
        assertCompares(getExpectedNullMissingComparison(), NullNode.getInstance(), null);
        assertComparesEqual(MissingNode.getInstance(), null);

        JsonNode parsedNull = this.objectMapper.readTree("null");
        assertComparesEqual(parsedNull, this.objectMapper.readTree("null"));
        assertComparesEqual(parsedNull, NullNode.getInstance());
        assertCompares(getExpectedNullMissingComparison(), parsedNull, MissingNode.getInstance());
        assertCompares(getExpectedNullMissingComparison(), parsedNull, null);
        assertComparesEqual(NullNode.getInstance(), parsedNull);
        assertCompares(getExpectedMissingNullComparison(), MissingNode.getInstance(), parsedNull);
        assertCompares(getExpectedMissingNullComparison(), null, parsedNull);
    }

    @Test
    public void testBoolean() throws IOException
    {
        assertComparesEqual(BooleanNode.valueOf(true), BooleanNode.valueOf(true));
        assertComparesEqual(BooleanNode.valueOf(false), BooleanNode.valueOf(false));
        assertComparesLessThan(BooleanNode.valueOf(false), BooleanNode.valueOf(true));
        assertComparesGreaterThan(BooleanNode.valueOf(true), BooleanNode.valueOf(false));

        assertComparesEqual("true", "true");
        assertComparesEqual("false", "false");
        assertComparesLessThan("false", "true");
        assertComparesGreaterThan("true", "false");
    }

    @Test
    public void testNumbers() throws IOException
    {
        short[] shorts = {Short.MIN_VALUE, -234, -1, 0, 1, Short.MAX_VALUE};
        int[] ints = {Integer.MIN_VALUE, -1 + Short.MIN_VALUE, 2 * Short.MAX_VALUE, 924835, Integer.MAX_VALUE};
        long[] longs = {Long.MIN_VALUE, -234L + Integer.MIN_VALUE, 2L * Integer.MAX_VALUE, Long.MAX_VALUE};
        BigInteger[] bigInts = Stream.of("198712593871298374987987987159871234", "-1290789487128934719235987981298479872539871234", "97650987723547777777239487509782378234069782354").map(BigInteger::new).toArray(BigInteger[]::new);
        float[] floats = {Float.MIN_VALUE, -7463487.9872347f, -1.0f, 0.0f, 1.0f, 987234987.124f, Float.MAX_VALUE};
        double[] doubles = {Double.MIN_VALUE, -4123.2143 * Float.MIN_VALUE, 92348.444 * Float.MAX_VALUE, Double.MAX_VALUE};
        BigDecimal[] bigDecimals = Stream.of("198712593871298374987987987159871234.0014089215409781278412057892103490781293075", "78234578923948567230957849283478459087.23894710975908712789410298375098719034908759871234").map(BigDecimal::new).toArray(BigDecimal[]::new);

        for (short i : shorts)
        {
            for (short j : shorts)
            {
                assertCompares(Short.compare(i, j), ShortNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertCompares(Integer.compare(i, j), ShortNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertCompares(Long.compare(i, j), IntNode.valueOf(i), LongNode.valueOf(j));
            }
            BigInteger iBigInt = BigInteger.valueOf(i);
            for (BigInteger j : bigInts)
            {
                assertCompares(iBigInt.compareTo(j), ShortNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertComparesLessThan(ShortNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertComparesLessThan(ShortNode.valueOf(i), DoubleNode.valueOf(j));
            }
            for (BigDecimal j : bigDecimals)
            {
                assertComparesLessThan(ShortNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (int i : ints)
        {
            for (short j : shorts)
            {
                assertCompares(Integer.compare(i, j), IntNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertCompares(Integer.compare(i, j), IntNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertCompares(Long.compare(i, j), IntNode.valueOf(i), LongNode.valueOf(j));
            }
            BigInteger iBigInt = BigInteger.valueOf(i);
            for (BigInteger j : bigInts)
            {
                assertCompares(iBigInt.compareTo(j), IntNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertComparesLessThan(IntNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertComparesLessThan(IntNode.valueOf(i), DoubleNode.valueOf(j));
            }
            for (BigDecimal j : bigDecimals)
            {
                assertComparesLessThan(IntNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (long i : longs)
        {
            for (short j : shorts)
            {
                assertCompares(Long.compare(i, j), LongNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertCompares(Long.compare(i, j), LongNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertCompares(Long.compare(i, j), LongNode.valueOf(i), LongNode.valueOf(j));
            }
            BigInteger iBigInt = BigInteger.valueOf(i);
            for (BigInteger j : bigInts)
            {
                assertCompares(iBigInt.compareTo(j), LongNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertComparesLessThan(LongNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertComparesLessThan(LongNode.valueOf(i), DoubleNode.valueOf(j));
            }
            for (BigDecimal j : bigDecimals)
            {
                assertComparesLessThan(LongNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (BigInteger i : bigInts)
        {
            for (short j : shorts)
            {
                assertCompares(i.compareTo(BigInteger.valueOf(j)), BigIntegerNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertCompares(i.compareTo(BigInteger.valueOf(j)), BigIntegerNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertCompares(i.compareTo(BigInteger.valueOf(j)), BigIntegerNode.valueOf(i), LongNode.valueOf(j));
            }
            for (BigInteger j : bigInts)
            {
                assertCompares(i.compareTo(j), BigIntegerNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertComparesLessThan(BigIntegerNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertComparesLessThan(BigIntegerNode.valueOf(i), DoubleNode.valueOf(j));
            }
            for (BigDecimal j : bigDecimals)
            {
                assertComparesLessThan(BigIntegerNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (float i : floats)
        {
            for (short j : shorts)
            {
                assertComparesGreaterThan(FloatNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertComparesGreaterThan(FloatNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertComparesGreaterThan(FloatNode.valueOf(i), LongNode.valueOf(j));
            }
            for (BigInteger j : bigInts)
            {
                assertComparesGreaterThan(FloatNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertCompares(Float.compare(i, j), FloatNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertCompares(Double.compare(i, j), FloatNode.valueOf(i), DoubleNode.valueOf(j));
            }
            BigDecimal iBigDecimal = new BigDecimal(i);
            for (BigDecimal j : bigDecimals)
            {
                assertCompares(iBigDecimal.compareTo(j), FloatNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (double i : doubles)
        {
            for (short j : shorts)
            {
                assertComparesGreaterThan(DoubleNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertComparesGreaterThan(DoubleNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertComparesGreaterThan(DoubleNode.valueOf(i), LongNode.valueOf(j));
            }
            for (BigInteger j : bigInts)
            {
                assertComparesGreaterThan(DoubleNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertCompares(Double.compare(i, j), DoubleNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertCompares(Double.compare(i, j), DoubleNode.valueOf(i), DoubleNode.valueOf(j));
            }
            BigDecimal iBigDecimal = new BigDecimal(i);
            for (BigDecimal j : bigDecimals)
            {
                assertCompares(iBigDecimal.compareTo(j), DoubleNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
        for (BigDecimal i : bigDecimals)
        {
            for (short j : shorts)
            {
                assertComparesGreaterThan(DecimalNode.valueOf(i), ShortNode.valueOf(j));
            }
            for (int j : ints)
            {
                assertComparesGreaterThan(DecimalNode.valueOf(i), IntNode.valueOf(j));
            }
            for (long j : longs)
            {
                assertComparesGreaterThan(DecimalNode.valueOf(i), LongNode.valueOf(j));
            }
            for (BigInteger j : bigInts)
            {
                assertComparesGreaterThan(DecimalNode.valueOf(i), BigIntegerNode.valueOf(j));
            }
            for (float j : floats)
            {
                assertCompares(i.compareTo(new BigDecimal(j)), DecimalNode.valueOf(i), FloatNode.valueOf(j));
            }
            for (double j : doubles)
            {
                assertCompares(i.compareTo(new BigDecimal(j)), DecimalNode.valueOf(i), DoubleNode.valueOf(j));
            }
            for (BigDecimal j : bigDecimals)
            {
                assertCompares(i.compareTo(j), DecimalNode.valueOf(i), DecimalNode.valueOf(j));
            }
        }
    }

    @Test
    public void testStrings() throws IOException
    {
        String[] strings = {"the quick brown fox jumps over the lazy dog", "", "The Quick Brown Fox", "Hello!", "1, 2, 3", "3, 2, 1"};
        for (String string1 : strings)
        {
            for (String string2 : strings)
            {
                int expected = string1.compareTo(string2);
                assertCompares(expected, TextNode.valueOf(string1), TextNode.valueOf(string2));
                assertCompares(expected, TextNode.valueOf(string1).toString(), TextNode.valueOf(string2).toString());
            }
        }
    }

    @Test
    public void testArrays() throws IOException
    {
        assertComparesEqual("[]", "[]");
        assertComparesEqual("[1, 2, 4, 5]", "[1, 2, 4, 5]");
        assertComparesEqual("[1, [2, 4], 5]", "[1, [2, 4], 5]");
        assertComparesEqual("[1, [\"a\", 4], 5]", "[1, [\"a\", 4], 5]");

        assertComparesLessThan("[]", "[0]");
        assertComparesLessThan("[0]", "[1]");
        assertComparesLessThan("[0]", "[0, 1]");

        assertComparesGreaterThan("[1, 1]", "[1]");
        assertComparesGreaterThan("[1, 2]", "[1, 1]");
        assertComparesGreaterThan("[[]]", "[]");

        assertComparesNotEqual("[1]", "[\"a\"]");
    }

    @Test
    public void testObjects() throws IOException
    {
        // simple equality
        assertComparesEqual("{}", "{}");
        assertComparesEqual("{\"a\":1}", "{\"a\":1}");
        assertComparesEqual("{\"a\":{\"b\":[1, 2, 3]}}", "{\"a\":{\"b\":[1, 2, 3]}}");

        // key ordering doesn't matter
        assertComparesEqual("{\"a\":1,\"b\":2}", "{\"b\":2,\"a\":1}");

        // null values == missing properties
        assertCompares(getExpectedNullMissingComparison(), "{\"a\":null}", "{}");
        assertComparesEqual("{\"a\":null}", "{\"a\":null}");
        assertCompares(getExpectedMissingNullComparison(), "{\"a\":1}", "{\"a\":1,\"b\":null}");

        // complex, nested equality
        assertCompares(getExpectedNullMissingComparison(), "{\"a\":{\"d\":null}, \"b\":{\"e\":[3,2,1],\"f\":[1,2,3]}, \"c\":null}", "{\"b\":{\"f\":[1,2,3],\"e\":[3,2,1]}, \"a\":{}}");

        // inequality
        assertComparesLessThan("{}", "{\"a\":[]}");
        assertComparesLessThan("{\"b\":1}", "{\"a\":[]}");
        assertComparesGreaterThan("{\"b\":1,\"a\":[]}", "{\"a\":[]}");
        assertComparesLessThan("{\"b\":1,\"a\":[]}", "{\"a\":[1, 2]}");
    }

    @Test
    public void testMixedTypes() throws IOException
    {
        assertComparesNotEqual("null", "1");
        assertComparesNotEqual("17", "\"the quick brown fox\"");
        assertComparesNotEqual("[\"a\", 2, null]", "{}");
        assertComparesNotEqual("[]", "{}");
        assertComparesNotEqual("[]", "null");
        assertComparesNotEqual("null", "{}");
        assertComparesNotEqual("null", "true");
        assertComparesNotEqual("null", "false");
    }

    private void assertComparesEqual(String json1, String json2) throws IOException
    {
        assertCompares(0, json1, json2);
    }

    private void assertComparesEqual(JsonNode node1, JsonNode node2)
    {
        assertCompares(0, node1, node2);
    }

    private void assertComparesLessThan(String json1, String json2) throws IOException
    {
        assertCompares(-1, json1, json2);
    }

    private void assertComparesLessThan(JsonNode node1, JsonNode node2)
    {
        assertCompares(-1, node1, node2);
    }

    private void assertComparesGreaterThan(String json1, String json2) throws IOException
    {
        assertCompares(1, json1, json2);
    }

    private void assertComparesGreaterThan(JsonNode node1, JsonNode node2)
    {
        assertCompares(1, node1, node2);
    }

    private void assertComparesNotEqual(String json1, String json2) throws IOException
    {
        assertComparesNotEqual(this.objectMapper.readTree(json1), this.objectMapper.readTree(json2));
    }

    private void assertComparesNotEqual(JsonNode node1, JsonNode node2)
    {
        int actual = getComparator().compare(node1, node2);
        if (actual == 0)
        {
            Assert.assertNotEquals("node1: " + node1 + "; node2: " + node2, 0, actual);
        }
    }

    private void assertCompares(int expected, String json1, String json2) throws IOException
    {
        assertCompares(expected, this.objectMapper.readTree(json1), this.objectMapper.readTree(json2));
    }

    private void assertCompares(int expected, JsonNode node1, JsonNode node2)
    {
        int actual = getComparator().compare(node1, node2);
        if (Integer.signum(expected) != Integer.signum(actual))
        {
            Assert.assertEquals("node1: " + node1 + "; node2: " + node2, expected, actual);
        }
    }

    protected abstract int getExpectedNullMissingComparison();

    private int getExpectedMissingNullComparison()
    {
        return -getExpectedNullMissingComparison();
    }

    private JsonNodeComparator getComparator()
    {
        int nullMissingComparison = getExpectedNullMissingComparison();
        return (nullMissingComparison == 0) ? JsonNodeComparator.NULL_MISSING_EQUIVALENT : ((nullMissingComparison < 0) ? JsonNodeComparator.NULL_BEFORE_MISSING : JsonNodeComparator.MISSING_BEFORE_NULL);
    }

    public static class TestNullMissingEquivalent extends TestJsonNodeComparator
    {
        @Override
        protected int getExpectedNullMissingComparison()
        {
            return 0;
        }
    }

    public static class TestNullBeforeMissing extends TestJsonNodeComparator
    {
        @Override
        protected int getExpectedNullMissingComparison()
        {
            return -1;
        }
    }

    public static class TestMissingBeforeNull extends TestJsonNodeComparator
    {
        @Override
        protected int getExpectedNullMissingComparison()
        {
            return 1;
        }
    }
}

