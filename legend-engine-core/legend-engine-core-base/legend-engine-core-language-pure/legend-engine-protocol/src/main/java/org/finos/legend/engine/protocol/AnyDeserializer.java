// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AnyDeserializer extends JsonDeserializer<Object>
{
    private final List<Class<?>> classes;

    public AnyDeserializer(List<Class<?>> classes)
    {
        this.classes = classes;
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
    {
        JsonNode node = jsonParser.readValueAsTree();

        Object value = deserialize(jsonParser.getCodec(), node);

        if (value == null && !(node instanceof NullNode))
        {
            throw new IllegalArgumentException("json:" + node.toString() + " does not match with any valid type");
        }

        return value;
    }

    private List<Object> deserialize(ObjectCodec codec, ArrayNode node)
    {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.elements(), Spliterator.ORDERED), false).map(node1 -> deserialize(codec, node1)).collect(Collectors.toList());
    }

    private Object deserialize(ObjectCodec codec, JsonNode node)
    {
        if (node instanceof ArrayNode)
        {
            return deserialize(codec, (ArrayNode) node);
        }

        if (node instanceof NullNode)
        {
            return null;
        }

        for (Class<?> clazz : this.classes)
        {
            Object value = tryDeserialize(codec, node, clazz);
            if (value != null)
            {
                return value;
            }
        }

        throw new RuntimeException(String.format("Failed to deserialize '%s' to types [%s]", node, this.classes));
    }

    private Object tryDeserialize(ObjectCodec codec, JsonNode node, Class<?> clazz)
    {
        try
        {
            if (clazz == LocalDate.class && node instanceof TextNode)
            {
                return LocalDate.parse(node.textValue());
            }
            if (clazz == LocalDateTime.class && node instanceof TextNode)
            {
                return LocalDateTime.parse(node.textValue());
            }
            // ObjectMapper deserializes to incorrect types without this check.
            if (
                    (clazz == String.class && !(node instanceof TextNode))
                            || (clazz == Integer.class && !(node instanceof IntNode))
                            || (clazz == Float.class && !(node instanceof FloatNode))
                            || (clazz == Double.class && !(node instanceof DoubleNode))
                            || (clazz == Long.class && !(node instanceof IntNode || node instanceof LongNode))
                            || (clazz == Boolean.class && !(node instanceof BooleanNode))
            )
            {
                return null;
            }
            return codec.treeToValue(node, clazz);

        }
        catch (Exception e)
        {
            return null;
        }
    }
}