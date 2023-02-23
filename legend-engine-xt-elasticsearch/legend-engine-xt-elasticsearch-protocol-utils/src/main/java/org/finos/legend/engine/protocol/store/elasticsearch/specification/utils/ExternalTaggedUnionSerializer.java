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

package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class ExternalTaggedUnionSerializer extends DictionarySerializer
{
    @SuppressWarnings("UnusedDeclaration")
    public ExternalTaggedUnionSerializer()
    {

    }

    public ExternalTaggedUnionSerializer(BeanProperty property)
    {
        super(property);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        return new ExternalTaggedUnionSerializer(property);
    }

    @Override
    protected ObjectNode addToSerialize(ObjectCodec codec, ObjectNode toSerialize, ObjectNode dictionaryEntry)
    {
        // if normal dictionary entry
        if (dictionaryEntry.has("key"))
        {
            String key = dictionaryEntry.get("key").asText();
            JsonNode rawValue = dictionaryEntry.get("value");
            return process(codec, toSerialize, key, rawValue);
        }
        // if additional properties dictionary entry
        else if (this.isAdditionalProperties())
        {
            Map.Entry<String, JsonNode> next = dictionaryEntry.fields().next();
            String key = next.getKey();
            JsonNode rawValue = next.getValue();
            return process(codec, toSerialize, key, rawValue);
        }

        throw new IllegalStateException("Unexpected json to serialize: " + dictionaryEntry);
    }

    private boolean isAdditionalProperties()
    {
        return this.property.getName().equals("additionalProperties");
    }

    private static ObjectNode process(ObjectCodec codec, ObjectNode toSerialize, String key, JsonNode rawValue)
    {
        if (rawValue.isObject())
        {
            ObjectNode value = (ObjectNode) rawValue;
            Pair<String, JsonNode> typeAndValue = extractTypeAndValue(value);
            if (typeAndValue != null)
            {
                return toSerialize.set(typeAndValue.getOne() + '#' + key, typeAndValue.getTwo());
            }
            else
            {
                return toSerialize;
            }
        }
        else
        {
            ArrayNode value = (ArrayNode) rawValue;

            if (value.size() == 0)
            {
                // unknown type, so skip
                return toSerialize;
            }
            else
            {
                ArrayNode unionValues = (ArrayNode) codec.createArrayNode();
                String type = null;
                for (JsonNode node : value)
                {
                    Pair<String, JsonNode> typeAndValue = extractTypeAndValue((ObjectNode) node);
                    if (typeAndValue != null)
                    {
                        String entryType = typeAndValue.getOne();
                        if (type == null)
                        {
                            type = entryType;
                        }
                        else if (!entryType.equals(type))
                        {
                            throw new IllegalStateException("Does not support multiple types of external unions on same name");
                        }
                        unionValues.add(typeAndValue.getTwo());
                    }
                }

                return toSerialize.set(type + '#' + key, unionValues);
            }
        }
    }

    private static Pair<String, JsonNode> extractTypeAndValue(ObjectNode value)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
        List<Map.Entry<String, JsonNode>> notNullFields = Lists.mutable.empty();
        while (fields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isNull())
            {
                notNullFields.add(entry);
            }
        }

        if (notNullFields.size() == 0)
        {
            return null;
        }
        else if (notNullFields.size() == 1)
        {
            Map.Entry<String, JsonNode> nodeEntry = notNullFields.get(0);
            String externalType = nodeEntry.getKey();
            JsonNode unionValue = nodeEntry.getValue();
            return Tuples.pair(externalType, unionValue);
        }
        else
        {
            throw new IllegalStateException("Union with more than one value? " + value);
        }
    }

    protected void getWriteTree(JsonGenerator gen, ObjectNode toSerialize) throws IOException
    {
        // if additional property, we need to flat the structure so keys here are sibling to other keys
        if (this.isAdditionalProperties())
        {
            Iterator<Map.Entry<String, JsonNode>> fields = toSerialize.fields();
            while (fields.hasNext())
            {
                Map.Entry<String, JsonNode> next = fields.next();
                String field = next.getKey();
                JsonNode value = next.getValue();
                gen.writeFieldName(field);
                gen.writeTree(value);
            }
        }
        else
        {
            super.getWriteTree(gen, toSerialize);
        }
    }
}
