package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class ExternalTaggedUnionSerializer extends DictionarySerializer
{
    @Override
    protected ObjectNode addToSerialize(ObjectCodec codec, ObjectNode toSerialize, ObjectNode dictionaryEntry)
    {
        String key = dictionaryEntry.get("key").asText();
        JsonNode rawValue = dictionaryEntry.get("value");
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
                // cannot know type
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
}
