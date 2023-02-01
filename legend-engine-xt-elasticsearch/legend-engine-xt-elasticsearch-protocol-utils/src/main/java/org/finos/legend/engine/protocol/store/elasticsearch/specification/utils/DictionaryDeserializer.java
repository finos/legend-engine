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
 *
 *  4 use cases to consider
 *    DictionaryEntrySingleValue
 *    List<DictionaryEntrySingleValue>
 *    DictionaryEntryMultiValue
 *    List<DictionaryEntryMultiValue>
 */
public class DictionaryDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    protected JavaType type;

    public DictionaryDeserializer()
    {
        this(null);
    }

    public DictionaryDeserializer(JavaType type)
    {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        return new DictionaryDeserializer(ctxt.getContextualType());
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        boolean singleKey = !this.type.isTypeOrSubTypeOf(List.class);

        JsonNode jsonNode = p.readValueAsTree();

        if (jsonNode.isEmpty() || jsonNode.isNull())
        {
            return singleKey ? null : Collections.emptyList();
        }

        ObjectNode keyValues = (ObjectNode) jsonNode;
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
        TreeTraversingParser nodeParser = new TreeTraversingParser(toDeserialize);
        nodeParser.nextToken();
        return ctxt.readValue(nodeParser, this.type);
    }

    protected JsonNode getEntryNode(DeserializationContext ctxt, Map.Entry<String, JsonNode> entry) throws JsonMappingException
    {
        return ctxt.getNodeFactory().objectNode()
                .<ObjectNode>set("key", ctxt.getNodeFactory().textNode(entry.getKey()))
                .set("value", entry.getValue());
    }
}
