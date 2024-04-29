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
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;

public class LiteralOrExpressionDeserializer extends JsonDeserializer<LiteralOrExpression<Object>> implements ContextualDeserializer
{
    private JavaType type;

    @SuppressWarnings("UnusedDeclaration")
    public LiteralOrExpressionDeserializer()
    {

    }

    public LiteralOrExpressionDeserializer(JavaType type)
    {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
    {
        JavaType contextualType = ctxt.getContextualType();
        return new LiteralOrExpressionDeserializer(contextualType);
    }

    @Override
    public LiteralOrExpression<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        LiteralOrExpression<Object> literalOrExpression;
        // is object?
        if (p.isExpectedStartObjectToken())
        {
            // read if is value or expression field
            String fieldName = p.nextFieldName();
            if ("value".equals(fieldName))
            {
                // move to value to be able to deserialize it
                p.nextToken();
                Object value = ctxt.readValue(p, this.type.containedType(0));
                literalOrExpression = LiteralOrExpression.literal(value);
                // go to end of object as field has been processed
                p.nextToken();
            }
            else if ("expression".equals(fieldName))
            {
                // move to value and get it as Text - ie expression
                String value = p.nextTextValue();
                literalOrExpression = LiteralOrExpression.expression(value);
                // go to end of object as field has been processed
                p.nextToken();
            }
            else
            {
                Object value = ctxt.readValue(p, this.type.containedType(0));
                literalOrExpression = LiteralOrExpression.literal(value);
            }
        }
        // is just a scalar?
        else
        {
            Object value = ctxt.readValue(p, this.type.containedType(0));
            literalOrExpression = LiteralOrExpression.literal(value);
        }

        return literalOrExpression;
    }
}
