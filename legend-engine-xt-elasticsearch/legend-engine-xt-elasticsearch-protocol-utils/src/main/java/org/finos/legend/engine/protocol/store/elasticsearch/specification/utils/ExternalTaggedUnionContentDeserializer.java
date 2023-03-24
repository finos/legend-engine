// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.IOException;

/**
 * Elasticsearch have 3 union variants: externally tagged, internally tagged, and simple union
 * Only one field of on the union should be not-null
 * During serialization, we pick the non-null value, and serialize it
 * We need to find which field we need to assign the value to during deserialization
 * <p>
 * This variant expect the value inside a map, and the key is "name#type"
 */

public class ExternalTaggedUnionContentDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    private final JavaType contextualType;

    @SuppressWarnings("UnusedDeclaration")
    public ExternalTaggedUnionContentDeserializer()
    {
        this(null);
    }

    public ExternalTaggedUnionContentDeserializer(JavaType contextualType)
    {
        this.contextualType = contextualType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
    {
        return new ExternalTaggedUnionContentDeserializer(ctxt.getContextualType());
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String key = p.getParsingContext().getCurrentName();

        if (key == null)
        {
            key = p.getParsingContext().getParent().getCurrentName();
        }

        int hashPos = key.indexOf('#');
        if (hashPos == -1)
        {
            throw ctxt.instantiationException(this.contextualType.getRawClass(), "Property name for externally tagged '" + key + "' is not in the 'type#name' format. Make sure the request has 'typed_keys' set.");
        }

        JsonNode unionNode;

        String type = key.substring(0, hashPos);
        JsonNode value = p.readValueAsTree();

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

        TreeTraversingParser nodeParser = new TreeTraversingParser(unionNode, p.getCodec());
        nodeParser.nextToken();
        return ctxt.readValue(nodeParser, this.contextualType);
    }
}
