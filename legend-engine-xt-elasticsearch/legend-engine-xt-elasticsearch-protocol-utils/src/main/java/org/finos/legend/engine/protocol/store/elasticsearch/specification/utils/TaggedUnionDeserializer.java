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
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;

/**
 * Elasticsearch have 3 union variants: externally tagged, internally tagged, and simple union
 * Only one field of on the union should be not-null
 * During serialization, we pick the non-null value, and serialize it
 * We need to find which field we need to assign the value to during deserialization
 *
 * This variant is one with multiple heterogeneous fields (no relation between them), usually with a catch all String
 */
public class TaggedUnionDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    private JavaType type;
    private List<Field> objectFields;
    private List<NonObjectFieldHandler> nonObjectFieldHandlers;

    public TaggedUnionDeserializer()
    {

    }

    public TaggedUnionDeserializer(JavaType type, List<Field> objectFields, List<NonObjectFieldHandler> nonObjectFieldHandlers)
    {
        this.type = type;
        this.objectFields = objectFields;
        this.nonObjectFieldHandlers = nonObjectFieldHandlers;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        JavaType contextualType = ctxt.getContextualType();

        Field[] fields = contextualType.getRawClass().getFields();

        List<Field> objectFields = Lists.mutable.empty();
        List<NonObjectFieldHandler> nonObjectFieldHandlers = Lists.mutable.empty();

        for (Field f : fields)
        {
            // disambiguate primitives + lists (they don't occur twice in unions)
            if (f.getType().equals(String.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isTextual));
            }
            else if (f.getType().equals(Long.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isIntegralNumber));
            }
            else if (f.getType().equals(Double.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isDouble));
            }
            else if (f.getType().equals(Boolean.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isBoolean));
            }
            else if(List.class.isAssignableFrom(f.getType()))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isArray));
            }
            else
            {
                objectFields.add(f);
            }
        }

        return new TaggedUnionDeserializer(contextualType, objectFields, nonObjectFieldHandlers);
    }



    @Override
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException
    {
        try
        {
            return this.type.getRawClass().getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw ctxt.instantiationException(this.type.getRawClass(), e);
        }
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        try
        {
            JsonNode node = p.readValueAsTree();
            Class<?> rawClass = this.type.getRawClass();
            Object union = rawClass.getDeclaredConstructor().newInstance();

            if (node.isObject())
            {
                // AsDeductionTypeDeserializer
                throw new UnsupportedEncodingException("need jackson 2.13 to use deduction from properties");
            }
            else
            {
                List<NonObjectFieldHandler> matches = this.nonObjectFieldHandlers.stream()
                        .filter(x -> x.nodeCheck.test(node))
                        .collect(Collectors.toList());

                if (matches.size() == 1)
                {
                    Field field = matches.get(0).field;
                    JavaType javaType = ctxt.getTypeFactory().constructType(field.getGenericType());
                    TreeTraversingParser parserForType = new TreeTraversingParser(node);
                    parserForType.nextToken();
                    Object value = ctxt.readValue(parserForType, javaType);
                    field.set(union, value);
                }
                else if (matches.size() == 0)
                {
                    throw ctxt.instantiationException(rawClass, "No field on union for " + node);
                }
                else
                {
                    throw ctxt.instantiationException(rawClass, "Multiple fields: " + matches.stream().map(x -> x.field.getName()).collect(Collectors.joining(", ")) + "; found on union for " + node);
                }
            }

            return union;
        }
        catch (ReflectiveOperationException e)
        {
            throw ctxt.instantiationException(this.type.getRawClass(), e);
        }
    }

    private static class NonObjectFieldHandler
    {
        private final Field field;
        private final Predicate<JsonNode> nodeCheck;

        private NonObjectFieldHandler(Field field, Predicate<JsonNode> nodeCheck)
        {
            this.field = field;
            this.nodeCheck = nodeCheck;
        }
    }
}
