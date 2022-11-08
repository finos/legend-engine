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