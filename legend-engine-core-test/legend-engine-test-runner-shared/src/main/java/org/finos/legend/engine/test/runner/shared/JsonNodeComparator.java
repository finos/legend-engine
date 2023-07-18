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
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonNodeComparator implements Comparator<JsonNode>
{
    public static final JsonNodeComparator NULL_MISSING_EQUIVALENT = new JsonNodeComparator(0);
    public static final JsonNodeComparator NULL_BEFORE_MISSING = new JsonNodeComparator(-1);
    public static final JsonNodeComparator MISSING_BEFORE_NULL = new JsonNodeComparator(1);

    private final int nullMissingComparison;

    private JsonNodeComparator(int nullMissingComparison)
    {
        this.nullMissingComparison = nullMissingComparison;
    }

    @Override
    public int compare(JsonNode node1, JsonNode node2)
    {
        if (node1 == null)
        {
            return (node2 == null) ? 0 : compareNodeTypes(JsonNodeType.MISSING, node2.getNodeType());
        }
        if (node2 == null)
        {
            return compareNodeTypes(node1.getNodeType(), JsonNodeType.MISSING);
        }

        if (node1 == node2)
        {
            return 0;
        }

        JsonNodeType nodeType = node1.getNodeType();
        int cmp = compareNodeTypes(nodeType, node2.getNodeType());
        if (cmp != 0)
        {
            return cmp;
        }

        switch (nodeType)
        {
            case NULL:
            case MISSING:
            {
                return 0;
            }
            case BOOLEAN:
            {
                return Boolean.compare(node1.booleanValue(), node2.booleanValue());
            }
            case STRING:
            {
                return node1.textValue().compareTo(node2.textValue());
            }
            case NUMBER:
            {
                return compareNumbers(node1, node2);
            }
            case BINARY:
            {
                return compareBinaries(node1, node2);
            }
            case OBJECT:
            {
                return compareObjects(node1, node2);
            }
            case ARRAY:
            {
                return compareArrays(node1, node2);
            }
            case POJO:
            {
                return comparePojos(node1, node2);
            }
            default:
            {
                // This should never happen, but just in case ...
                return node1.toString().compareTo(node2.toString());
            }
        }
    }

    /**
     * NULL and MISSING are considered equivalent and are less than
     * all other types. Other types are compared in a deterministic
     * but arbitrary way.
     *
     * @param type1 first node type
     * @param type2 second node type
     * @return comparison
     */
    private int compareNodeTypes(JsonNodeType type1, JsonNodeType type2)
    {
        if (type1 == type2)
        {
            return 0;
        }
        if (type1 == JsonNodeType.NULL)
        {
            return (type2 == JsonNodeType.MISSING) ? this.nullMissingComparison : -1;
        }
        if (type1 == JsonNodeType.MISSING)
        {
            return (type2 == JsonNodeType.NULL) ? -this.nullMissingComparison : -1;
        }
        if ((type2 == JsonNodeType.NULL) || (type2 == JsonNodeType.MISSING))
        {
            return 1;
        }

        return type1.compareTo(type2);
    }

    /**
     * Integral numbers are less than non-integral numbers. Integral numbers
     * are compared to each other by value. Floating point numbers are also
     * compared to each other by value.
     *
     * @param node1 first number node
     * @param node2 second number node
     * @return comparison
     */
    private int compareNumbers(JsonNode node1, JsonNode node2)
    {
        if (node1.isIntegralNumber())
        {
            if (!node2.isIntegralNumber())
            {
                return -1;
            }
            if (node1.isBigInteger() || node2.isBigInteger())
            {
                return node1.bigIntegerValue().compareTo(node2.bigIntegerValue());
            }
            return Long.compare(node1.longValue(), node2.longValue());
        }
        if (node2.isIntegralNumber())
        {
            return 1;
        }
        if (node1.isFloatingPointNumber())
        {
            if (!node2.isFloatingPointNumber())
            {
                return -1;
            }
            if (node1.isBigDecimal() || node2.isBigDecimal())
            {
                return node1.decimalValue().compareTo(node2.decimalValue());
            }
            return Double.compare(node1.doubleValue(), node2.doubleValue());
        }
        if (node2.isFloatingPointNumber())
        {
            return 1;
        }
        // This should never happen, but just in case ...
        return node1.toString().compareTo(node2.toString());
    }

    private int compareBinaries(JsonNode node1, JsonNode node2)
    {
        byte[] binary1;
        byte[] binary2;
        try
        {
            binary1 = node1.binaryValue();
            binary2 = node2.binaryValue();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        int length1 = binary1.length;
        int length2 = binary2.length;
        for (int i = 0, minLength = Math.min(length1, length2); i < minLength; i++)
        {
            byte b1 = binary1[i];
            byte b2 = binary2[i];
            if (b1 != b2)
            {
                return b1 - b2;
            }
        }
        return Integer.compare(length1, length2);
    }

    private int compareArrays(JsonNode node1, JsonNode node2)
    {
        int length1 = node1.size();
        int length2 = node2.size();
        for (int i = 0, minLength = Math.min(length1, length2); i < minLength; i++)
        {
            int cmp = compare(node1.get(i), node2.get(i));
            if (cmp != 0)
            {
                return cmp;
            }
        }
        return Integer.compare(length1, length2);
    }

    private int comparePojos(JsonNode node1, JsonNode node2)
    {
        if (node1.equals(node2))
        {
            return 0;
        }
        throw new UnsupportedOperationException("TODO");
    }

    private int compareObjects(JsonNode node1, JsonNode node2)
    {
        return Stream.concat(nodeFieldNameStream(node1), nodeFieldNameStream(node2))
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .mapToInt(f -> compare(node1.get(f), node2.get(f)))
                .filter(cmp -> cmp != 0)
                .findFirst()
                .orElse(0);
    }

    private static Stream<String> nodeFieldNameStream(JsonNode node)
    {
        return StreamSupport.stream(Spliterators.spliterator(node.fieldNames(), node.size(), Spliterator.SIZED | Spliterator.DISTINCT), false);
    }
}

