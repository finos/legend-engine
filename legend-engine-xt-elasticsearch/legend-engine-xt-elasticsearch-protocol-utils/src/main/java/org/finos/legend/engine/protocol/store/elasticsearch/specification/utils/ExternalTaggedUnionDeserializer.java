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
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Map;

/**
 * Elasticsearch have 3 union variants: externally tagged, internally tagged, and simple union
 * Only one field of on the union should be not-null
 * During serialization, we pick the non-null value, and serialize it
 * We need to find which field we need to assign the value to during deserialization
 * <p>
 * This variant expect the value inside a map, and the key is "name#type"
 */

public class ExternalTaggedUnionDeserializer extends DictionaryDeserializer
{
    public ExternalTaggedUnionDeserializer(JavaType contextualType, BeanProperty property)
    {
        super(contextualType, property);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ExternalTaggedUnionDeserializer()
    {

    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        return new ExternalTaggedUnionDeserializer(ctxt.getContextualType(), property);
    }

    private boolean isAdditionalProperties()
    {
        return this.property.getName().equals("additionalProperties");
    }

    @Override
    protected ObjectNode getNodeToProcess(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        ObjectNode nodeToProcess = super.getNodeToProcess(p, ctxt);
        if (this.isAdditionalProperties())
        {
            String key = p.getParsingContext().getCurrentName();
            ObjectNode jsonNodes = ctxt.getNodeFactory().objectNode();

            return jsonNodes.set(key, nodeToProcess);
        }
        else
        {
            return nodeToProcess;
        }
    }

    @Override
    protected JsonNode getEntryNode(DeserializationContext ctxt, Map.Entry<String, JsonNode> entry) throws JsonMappingException
    {
        String key = entry.getKey();
        JsonNode value = entry.getValue();

        int hashPos = key.indexOf('#');
        if (hashPos == -1)
        {
            throw ctxt.instantiationException(this.type.getRawClass(), "Property name for externally tagged '" + key + "' is not in the 'type#name' format. Make sure the request has 'typed_keys' set.");
        }

        String type = key.substring(0, hashPos);
        String name = key.substring(hashPos + 1);

        JsonNode unionNode;

        if (value.isObject())
        {
            unionNode = ctxt.getNodeFactory().objectNode().set(type, value);
        }
        else
        {
            unionNode = ctxt.getNodeFactory().arrayNode();
            ArrayNode values = (ArrayNode) value;
            for (JsonNode v : values)
            {
                ((ArrayNode) unionNode).add(ctxt.getNodeFactory().objectNode().set(type, v));
            }
        }

        if (this.isAdditionalProperties())
        {
            return unionNode;
        }
        else
        {
            return ctxt.getNodeFactory().objectNode()
                    .<ObjectNode>set("key", ctxt.getNodeFactory().textNode(name))
                    .set("value", unionNode);
        }
    }
}
