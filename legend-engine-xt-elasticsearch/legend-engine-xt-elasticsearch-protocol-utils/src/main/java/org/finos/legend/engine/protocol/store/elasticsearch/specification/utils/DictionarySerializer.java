package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;

/**
 * To simplify pure, we have pairs of key/values on the protocol classes.
 * In some cases, is a set of these key/values (ie List on protocol)
 * Elasticsearch expect all these as maps
 *
 *  4 use cases to consider
 *    DictionaryEntrySingleValue
 *    List<DictionaryEntrySingleValue>
 *    DictionaryEntryMultiValue
 *    List<DictionaryEntryMultiValue>
 */
public class DictionarySerializer extends JsonSerializer<Object>
{
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        ObjectCodec codec = gen.getCodec();
        TokenBuffer tokenBuffer = new TokenBuffer(codec, false);
        serializers.defaultSerializeValue(value, tokenBuffer);
        JsonNode asNode = tokenBuffer.asParser().readValueAs(JsonNode.class);

        ObjectNode toSerialize = (ObjectNode) codec.createObjectNode();
        if (asNode.isArray())
        {
            ArrayNode asArray = (ArrayNode) asNode;
            for (JsonNode node : asArray)
            {
                ObjectNode dictionaryEntry = (ObjectNode) node;
                toSerialize = addToSerialize(codec, toSerialize, dictionaryEntry);
            }
        }
        else if (asNode.isObject())
        {
            ObjectNode dictionaryEntry = (ObjectNode) asNode;
            toSerialize = addToSerialize(codec, toSerialize, dictionaryEntry);
        }

        gen.writeTree(toSerialize);
    }

    protected ObjectNode addToSerialize(ObjectCodec codec, ObjectNode toSerialize, ObjectNode dictionaryEntry)
    {
        return toSerialize.set(dictionaryEntry.get("key").asText(), dictionaryEntry.get("value"));
    }

}
