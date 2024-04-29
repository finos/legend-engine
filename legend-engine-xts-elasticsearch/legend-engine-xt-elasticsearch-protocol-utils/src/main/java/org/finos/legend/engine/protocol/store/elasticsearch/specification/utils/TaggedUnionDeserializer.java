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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;

/**
 * Elasticsearch have 3 union variants: externally tagged, internally tagged, and simple union
 * Only one field of on the union should be not-null
 * During serialization, we pick the non-null value, and serialize it
 * We need to find which field we need to assign the value to during deserialization
 * <p>
 * This variant is one with multiple heterogeneous fields (no relation between them), usually with a catch all String
 */
public class TaggedUnionDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    private JavaType type;
    private List<NonObjectFieldHandler> nonObjectFieldHandlers;

    @SuppressWarnings("UnusedDeclaration")
    public TaggedUnionDeserializer()
    {

    }

    public TaggedUnionDeserializer(DeserializationConfig config, JavaType type, List<BeanPropertyDefinition> objectFields, List<NonObjectFieldHandler> nonObjectFieldHandlers)
    {
        this.type = type;
        this.nonObjectFieldHandlers = nonObjectFieldHandlers;
        this.subtypeFingerprints = this.buildFingerprintsForDeduction(config, objectFields);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        JavaType contextualType = ctxt.getContextualType();
        BeanDescription beanDescription = ctxt.getConfig().introspect(ctxt.getContextualType());

        List<BeanPropertyDefinition>  fields = beanDescription.findProperties();

        List<BeanPropertyDefinition> objectFields = Lists.mutable.empty();
        List<NonObjectFieldHandler> nonObjectFieldHandlers = Lists.mutable.empty();

        for (BeanPropertyDefinition f : fields)
        {
            // disambiguate primitives + lists (they don't occur twice in unions)
            JavaType primaryType = f.getPrimaryType();
            Class<?> rawClass = primaryType.getRawClass();
            if (rawClass.equals(LiteralOrExpression.class))
            {
                rawClass = primaryType.containedType(0).getRawClass();
            }

            if (rawClass.equals(String.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isTextual));
            }
            else if (rawClass.equals(Long.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isIntegralNumber));
            }
            else if (Number.class.isAssignableFrom(rawClass))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isFloatingPointNumber));
            }
            else if (rawClass.equals(Boolean.class))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isBoolean));
            }
            else if (List.class.isAssignableFrom(rawClass))
            {
                nonObjectFieldHandlers.add(new NonObjectFieldHandler(f, JsonNode::isArray));
            }
            else
            {
                objectFields.add(f);
            }
        }

        return new TaggedUnionDeserializer(ctxt.getConfig(), contextualType, objectFields, nonObjectFieldHandlers);
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
            Class<?> rawClass = this.type.getRawClass();
            Object union = rawClass.getDeclaredConstructor().newInstance();

            if (p.isExpectedStartObjectToken())
            {
                if (this.subtypeFingerprints.valuesView().size() == 1)
                {
                    BeanPropertyDefinition field = this.subtypeFingerprints.valuesView().getOnly();
                    JavaType javaType = field.getPrimaryType();
                    Object value = ctxt.readValue(p, javaType);
                    field.getField().getAnnotated().set(union, value);
                }
                else
                {
                    deserializeObject(union, p, ctxt);
                }
            }
            else
            {
                JsonNode node = p.readValueAsTree();
                List<NonObjectFieldHandler> matches = this.nonObjectFieldHandlers.stream().filter(x -> x.nodeCheck.test(node)).collect(Collectors.toList());

                if (matches.size() == 1)
                {
                    BeanPropertyDefinition field = matches.get(0).field;
                    JavaType javaType = field.getPrimaryType();
                    TreeTraversingParser parserForType = new TreeTraversingParser(node, p.getCodec());
                    parserForType.nextToken();
                    Object value = ctxt.readValue(parserForType, javaType);
                    field.getField().getAnnotated().set(union, value);
                }
                else
                {
                    throw ctxt.instantiationException(rawClass, "Cannot find unique field.  Union fields: " + matches.stream().map(x -> x.field.getName()).collect(Collectors.joining(", ")) + "; found on union for " + node);
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
        private final BeanPropertyDefinition field;
        private final Predicate<JsonNode> nodeCheck;

        private NonObjectFieldHandler(BeanPropertyDefinition field, Predicate<JsonNode> nodeCheck)
        {
            this.field = field;
            this.nodeCheck = nodeCheck;
        }
    }

    private final Map<String, Integer> fieldBitIndex = new HashMap<>();
    private MutableListMultimap<BitSet, BeanPropertyDefinition> subtypeFingerprints;

    private MutableListMultimap<BitSet, BeanPropertyDefinition> buildFingerprintsForDeduction(DeserializationConfig config, Collection<BeanPropertyDefinition> subtypes)
    {
        int nextField = 0;
        MutableListMultimap<BitSet, BeanPropertyDefinition> fingerprints = Multimaps.mutable.list.empty();

        for (BeanPropertyDefinition subtype : subtypes)
        {
            JavaType subtyped = subtype.getPrimaryType();
            List<BeanPropertyDefinition> properties = config.introspect(subtyped).findProperties();

            BitSet fingerprint = new BitSet(nextField + properties.size());
            for (BeanPropertyDefinition property : properties)
            {
                String name = property.getName();
                Integer bitIndex = this.fieldBitIndex.get(name);
                if (bitIndex == null)
                {
                    bitIndex = nextField;
                    this.fieldBitIndex.put(name, nextField++);
                }
                fingerprint.set(bitIndex);
            }

            fingerprints.put(fingerprint, subtype);
        }
        return fingerprints;
    }

    private void deserializeObject(Object union, JsonParser p, DeserializationContext ctxt) throws IOException, ReflectiveOperationException
    {
        Set<BitSet> candidates = this.subtypeFingerprints.keysView().toSet();
        TokenBuffer tb = new TokenBuffer(p, ctxt);
        for (JsonToken t = p.nextToken(); t == JsonToken.FIELD_NAME; t = p.nextToken())
        {
            String name = p.currentName();
            tb.copyCurrentStructure(p);
            Integer bit = this.fieldBitIndex.get(name);
            if (bit != null)
            {
                candidates.removeIf(bitSet -> !bitSet.get(bit));
                if (candidates.size() == 1)
                {
                    BitSet matching = candidates.iterator().next();
                    p.clearCurrentToken();
                    p = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
                    p.nextToken();

                    MutableList<BeanPropertyDefinition> fields = this.subtypeFingerprints.get(matching);
                    if (fields.size() == 1)
                    {
                        BeanPropertyDefinition field = fields.get(0);
                        field.getField().getAnnotated().set(union, ctxt.readValue(p, field.getPrimaryType()));
                        return;
                    }
                }
            }
        }

        throw ctxt.instantiationException(this.type.getRawClass(), String.format("Cannot deduce unique type for union %s (%d candidates match)", ClassUtil.getTypeDescription(this.type), candidates.stream().mapToLong(x -> this.subtypeFingerprints.get(x).size()).sum()));
    }
}
