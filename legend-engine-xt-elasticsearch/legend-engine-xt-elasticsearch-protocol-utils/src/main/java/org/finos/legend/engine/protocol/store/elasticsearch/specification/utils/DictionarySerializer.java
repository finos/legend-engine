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
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import java.util.List;

/**
 * To simplify pure, we have pairs of key/values on the protocol classes.
 * In some cases, is a set of these key/values (ie List on protocol)
 * Elasticsearch expect all these as maps
 * <p>
 *  4 use cases to consider
 *    DictionaryEntrySingleValue
 *    List of DictionaryEntrySingleValue
 *    DictionaryEntryMultiValue
 *    List of DictionaryEntryMultiValue
 */
public class DictionarySerializer extends JsonSerializer<Object> implements ContextualSerializer
{

    protected BeanProperty property;

    @SuppressWarnings("UnusedDeclaration")
    public DictionarySerializer()
    {

    }

    public DictionarySerializer(BeanProperty property)
    {
        this.property = property;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        return new DictionarySerializer(property);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value)
    {
        if (value instanceof List)
        {
            return ((List<?>) value).isEmpty();
        }

        return super.isEmpty(provider, value);
    }

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

        if (toSerialize.isEmpty())
        {
            gen.writeNull();
        }
        else
        {
            getWriteTree(gen, toSerialize);
        }
    }

    protected void getWriteTree(JsonGenerator gen, ObjectNode toSerialize) throws IOException
    {
        gen.writeTree(toSerialize);
    }

    protected ObjectNode addToSerialize(ObjectCodec codec, ObjectNode toSerialize, ObjectNode dictionaryEntry)
    {
        return toSerialize.set(dictionaryEntry.get("key").asText(), dictionaryEntry.get("value"));
    }

}
