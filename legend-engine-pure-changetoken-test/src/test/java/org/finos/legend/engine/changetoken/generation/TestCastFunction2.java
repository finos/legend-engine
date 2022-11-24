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
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestCastFunction2
{
    private static JsonNode _upcast_to_ftdm_abcdefg456(JsonNode node)
    {
        if (node.isObject())
        {
            ObjectNode object = (ObjectNode) node;
            if (object.get("@type") == null)
            {
                throw new RuntimeException("Missing @type");
            }
            String type = object.get("@type").asText();
            if (type.equals("meta::pure::changetoken::tests::SampleClass"))
            {
                object = object.deepCopy();
                node = object;
                object.put("abc", 100);
            }
            object.put("version", "ftdm:abcdefg456");
        }
        return node;
    }

    public static JsonNode upcast(JsonNode node)
    {
        if (!node.isObject() || !node.has("version"))
        {
            throw new RuntimeException("Missing version");
        }
        String version = node.get("version").asText();
        if (version.equals("ftdm:abcdefg123"))
        {
            node = TestCastFunction2._upcast_to_ftdm_abcdefg456(node);
            version = node.get("version").asText();
        }
        if (!version.equals("ftdm:abcdefg456"))
        {
            throw new RuntimeException("Unexpected version: " + version);
        }
        return node;
    }

    public static JsonNode downcast(JsonNode node, String version)
    {
        return node;
    }
}