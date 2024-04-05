// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.to.visitors;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValueVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DateTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FloatTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValuePair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LongTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.VariableTypeValue;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.ComposerUtility.convertToStringWithQuotes;

public class BaseTypeValueVisitorImpl implements BaseTypeValueVisitor<String>
{
    @Override
    public String visit(ArrayTypeValue val)
    {
        List<String> arrayItemString = val.items.stream().map(x -> visitBaseTypeValue(x)).collect(Collectors.toList());

        return "[" + String.join(",", arrayItemString) + "]";
    }

    @Override
    public String visit(BoolTypeValue val)
    {
        return String.valueOf(val.value);
    }

    @Override
    public String visit(DecimalTypeValue val)
    {
        return String.valueOf(val.value);
    }

    @Override
    public String visit(IntTypeValue val)
    {
        return String.valueOf(val.value);
    }

    @Override
    public String visit(LongTypeValue val)
    {
        // not used
        return String.valueOf(val);
    }

    @Override
    public String visit(FloatTypeValue val)
    {
        return String.valueOf(val.value);
    }

    @Override
    public String visit(NullTypeValue val)
    {
        return null;
    }

    @Override
    public String visit(ObjectTypeValue val)
    {
        List<String> objPairString = val.keyValues.stream()
                .map(x -> visitKeyValuePair(x)).collect(Collectors.toList());
        return "{" + String.join(",", objPairString) + "}";
    }

    @Override
    public String visit(StringTypeValue val)
    {
        return convertToStringWithQuotes(String.valueOf(val.value));
    }

    @Override
    public String visit(VariableTypeValue val)
    {
        return String.valueOf(val.value);
    }

    @Override
    public String visit(DateTypeValue val)
    {
        return "new ISODate(\"" + val.value.toString() + "\")";
    }

    private String visitKeyValuePair(KeyValuePair pair)
    {
        String field = convertToStringWithQuotes(pair.key);
        String value = visitBaseTypeValue(pair.value);
        return field + " : " + value;
    }

    public String visitBaseTypeValue(BaseTypeValue value)
    {
        return value.accept(new BaseTypeValueVisitorImpl());
    }

}