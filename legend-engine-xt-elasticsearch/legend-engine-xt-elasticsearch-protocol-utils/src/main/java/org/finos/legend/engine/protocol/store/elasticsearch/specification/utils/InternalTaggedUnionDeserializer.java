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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Elasticsearch have 3 union variants: externally tagged, internally tagged, and simple union
 * Only one field of on the union should be not-null
 * During serialization, we pick the non-null value, and serialize it
 * We need to find which field we need to assign the value to during deserialization.
 * <p>
 * This variant is internally tagged - meaning the object itself contains the type name
 */
public class InternalTaggedUnionDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer
{
    private String typeField;

    private JavaType type;

    @SuppressWarnings("UnusedDeclaration")
    public InternalTaggedUnionDeserializer()
    {

    }

    public InternalTaggedUnionDeserializer(String typeField, JavaType type)
    {
        this.type = type;
        this.typeField = typeField;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
    {
        BeanDescription beanDescription = ctxt.getConfig().introspectClassAnnotations(ctxt.getContextualType());
        String typeField = ctxt.getConfig().getAnnotationIntrospector().findTypeName(beanDescription.getClassInfo());
        return new InternalTaggedUnionDeserializer(typeField, ctxt.getContextualType());
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        Class<?> rawClass = this.type.getRawClass();
        try
        {
            // read as node
            ObjectNode node = p.readValueAsTree();

            // get the field value
            String type = node.get(this.typeField).asText();

            // find field
            Field field = rawClass.getField(type);

            // convert json to actual type
            JavaType javaType = ctxt.getTypeFactory().constructType(field.getGenericType());
            TreeTraversingParser parserForType = new TreeTraversingParser(node, p.getCodec());
            parserForType.nextToken();
            Object value = ctxt.readValue(parserForType, javaType);

            // create union class and assign deserialize value
            Object union = rawClass.getDeclaredConstructor().newInstance();
            field.set(union, value);

            return union;
        }
        catch (ReflectiveOperationException e)
        {
            throw ctxt.instantiationException(rawClass, e);
        }
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException
    {
        Class<?> rawClass = this.type.getRawClass();

        try
        {
            return rawClass.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException e)
        {
            throw ctxt.instantiationException(rawClass, e);
        }
    }
}
