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

package org.finos.legend.engine.protocol.store.elasticsearch.v7.specification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.stream.Stream;
import org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.LiteralOrExpressionDeserializer;
import org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.TaggedUnionUtils;

@JsonDeserialize(using = LiteralOrExpressionDeserializer.class)
public class LiteralOrExpression<T>
{
    public T value;
    public String expression;

    public Object unionValue()
    {
        return Stream.<Object>of(this.value, this.expression)
                .filter(TaggedUnionUtils::nonEmpty)
                .findFirst()
                .orElse(null);
    }

    public static <T> LiteralOrExpression<T> literal(T value)
    {
        LiteralOrExpression<T> literalOrExpression = new LiteralOrExpression<>();
        literalOrExpression.value = value;
        return literalOrExpression;
    }

    public static <T> LiteralOrExpression<T> expression(String expression)
    {
        LiteralOrExpression<T> literalOrExpression = new LiteralOrExpression<>();
        literalOrExpression.expression = expression;
        return literalOrExpression;
    }

    @JsonIgnore
    public T getLiteral()
    {
        if (this.value != null)
        {
            return this.value;
        }
        else
        {
            throw new IllegalStateException("Expected literal value");
        }
    }

    @Override
    public String toString()
    {
        return this.unionValue().toString();
    }
}