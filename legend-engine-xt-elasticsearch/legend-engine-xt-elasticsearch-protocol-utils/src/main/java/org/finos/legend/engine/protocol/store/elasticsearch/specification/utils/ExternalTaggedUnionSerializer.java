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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ExternalTaggedUnionSerializer extends JsonSerializer<Map<String, ?>> implements ContextualSerializer
{
    private final Field[] types;
    private final boolean additionalProperties;

    @SuppressWarnings("UnusedDeclaration")
    public ExternalTaggedUnionSerializer()
    {
        this(false, null);
    }

    public ExternalTaggedUnionSerializer(boolean additionalProperties, Field[] types)
    {
        this.additionalProperties = additionalProperties;
        this.types = types;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
    {
        JavaType contentType = property.getType();
        if (contentType.isMapLikeType())
        {
            contentType = contentType.getContentType();
        }
        if (contentType.isCollectionLikeType())
        {
            contentType = contentType.getContentType();
        }
        return new ExternalTaggedUnionSerializer(property.getName().equals("additionalProperties"), contentType.getRawClass().getDeclaredFields());
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Map<String, ?> value)
    {
        return value == null || value.isEmpty();
    }

    @Override
    public void serialize(Map<String, ?> map, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (!this.additionalProperties)
        {
            gen.writeStartObject();
        }

        for (Map.Entry<String, ?> entry : map.entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            process(serializers, gen, key, value);
        }

        if (!this.additionalProperties)
        {
            gen.writeEndObject();
        }
    }

    private void process(SerializerProvider serializerProvider, JsonGenerator gen, String key, Object rawValue) throws IOException
    {
        try
        {
            if (rawValue instanceof List)
            {
                Field type = null;

                for (Field field : this.types)
                {
                    for (Object arrayElement : (List<?>) rawValue)
                    {
                        Object unionValue = field.get(arrayElement);
                        if (unionValue != null)
                        {
                            if (type == null)
                            {
                                type = field;
                                gen.writeArrayFieldStart(type.getName() + '#' + key);
                            }

                            serializerProvider.defaultSerializeValue(unionValue, gen);
                        }
                    }

                    if (type != null)
                    {
                        gen.writeEndArray();
                        break;
                    }
                }
            }
            else
            {
                for (Field field : this.types)
                {
                    Object union = field.get(rawValue);
                    if (union != null)
                    {
                        serializerProvider.defaultSerializeField(field.getName() + '#' + key, union, gen);
                        break;
                    }
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }
}
