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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
public class DictionaryDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    protected BeanProperty property;
    protected JavaType type;
    private boolean singleKey;

    @SuppressWarnings("UnusedDeclaration")
    public DictionaryDeserializer()
    {

    }

    public DictionaryDeserializer(JavaType type, BeanProperty property)
    {
        this.type = type;
        this.singleKey = !this.type.isTypeOrSubTypeOf(List.class);
        this.property = property;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        return new DictionaryDeserializer(ctxt.getContextualType(), property);
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException
    {
        return this.singleKey ? null : Collections.emptyList();
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        ObjectNode keyValues = getNodeToProcess(p, ctxt);
        if (singleKey && keyValues.size() != 1)
        {
            throw new IllegalStateException("Expected only one key");
        }

        ArrayNode arrayNode = ctxt.getNodeFactory().arrayNode(keyValues.size());
        Iterator<Map.Entry<String, JsonNode>> fields = keyValues.fields();
        while (fields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode entryNode = getEntryNode(ctxt, entry);
            arrayNode.add(entryNode);
        }

        JsonNode toDeserialize = singleKey ? arrayNode.get(0) : arrayNode;
        TreeTraversingParser nodeParser = new TreeTraversingParser(toDeserialize, p.getCodec());
        nodeParser.nextToken();
        return ctxt.readValue(nodeParser, this.type);
    }

    protected ObjectNode getNodeToProcess(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        return p.readValueAsTree();
    }

    protected JsonNode getEntryNode(DeserializationContext ctxt, Map.Entry<String, JsonNode> entry) throws JsonMappingException
    {
        return ctxt.getNodeFactory().objectNode()
                .<ObjectNode>set("key", ctxt.getNodeFactory().textNode(entry.getKey()))
                .set("value", entry.getValue());
    }
}
