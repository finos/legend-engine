//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class TestCastFunction2
{
    private static ArrayNode _upcast_to_ftdm_abcdefg456_array(ArrayNode arrayNode)
    {
        for (int i = 0; i < arrayNode.size(); ++i)
        {
            if (arrayNode.get(i).isArray())
            {
                arrayNode.set(i, _upcast_to_ftdm_abcdefg456_array((ArrayNode) arrayNode.get(i)));
            }
            else if (arrayNode.get(i).isObject())
            {
                arrayNode.set(i, _upcast_to_ftdm_abcdefg456_object((ObjectNode) arrayNode.get(i)));
            }
        }
        return arrayNode;
    }

    private static ObjectNode _upcast_to_ftdm_abcdefg456_object(ObjectNode objectNode)
    {
        if (objectNode.get("@type") == null)
        {
            throw new RuntimeException("Missing @type");
        }
        String type = objectNode.get("@type").asText();
        if (type.equals("meta::pure::changetoken::tests::SampleClass"))
        {
            objectNode.put("abc", 100);
        }

        // recurse
        Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
        while (it.hasNext())
        {
            Map.Entry<String, JsonNode> en = it.next();
            if (en.getValue().isObject())
            {
                ObjectNode innerObjectNode = TestCastFunction2._upcast_to_ftdm_abcdefg456_object((ObjectNode) en.getValue());
                ObjectNode newInnerObjectNode = objectNode.putObject(en.getKey());
                newInnerObjectNode.setAll(innerObjectNode);
            }
            else if (en.getValue().isArray())
            {
                ArrayNode arrayNode = TestCastFunction2._upcast_to_ftdm_abcdefg456_array((ArrayNode) en.getValue());
                ArrayNode newArrayNode = objectNode.putArray(en.getKey());
                newArrayNode.addAll(arrayNode);
            }
        }

        return objectNode;
    }

    private static ObjectNode _upcast_to_ftdm_abcdefg456(ObjectNode objectNode)
    {
        objectNode.put("version", "ftdm:abcdefg456");
        return TestCastFunction2._upcast_to_ftdm_abcdefg456_object(objectNode);
    }

    public static JsonNode upcast(JsonNode node)
    {
        if (!node.isObject() || !node.has("version"))
        {
            throw new RuntimeException("Missing version");
        }
        ObjectNode objectNode = node.deepCopy();

        String version = objectNode.get("version").asText();
        if (version.equals("ftdm:abcdefg123"))
        {
            objectNode = TestCastFunction2._upcast_to_ftdm_abcdefg456(objectNode);
            version = objectNode.get("version").asText();
        }
        if (!version.equals("ftdm:abcdefg456"))
        {
            throw new RuntimeException("Unexpected version: " + version);
        }
        return objectNode;
    }

    public static JsonNode downcast(JsonNode node, String version)
    {
        return node;
    }
}