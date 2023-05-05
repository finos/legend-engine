// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.external.format.flatdata.grammar.toPure;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;

public class FlatDataSchemaComposer
{
    public static String toGrammar(FlatData flatData)
    {
        StringBuilder builder = new StringBuilder();
        flatData.sections.forEach(s -> builder.append(toGrammar(s)));
        return builder.toString();
    }

    public static String toGrammar(FlatDataSection section)
    {
        StringBuilder builder = new StringBuilder("section ").append(section.name).append(": ").append(section.driverId).append("\n{");
        for (FlatDataProperty property : section.sectionProperties)
        {
            builder.append("\n  ").append(property.name);
            if (property.values.size() == 1)
            {
                Object value = property.values.get(0);
                if (value instanceof List && ((List<?>) value).size() == 1)
                {
                    value = ((List<?>) value).get(0);
                }

                if (value instanceof String)
                {
                    builder.append(": ").append(convertString((String) value, true));
                }
                else if (value instanceof Long)
                {
                    builder.append(": ").append(value);
                }
                else if (value instanceof Boolean)
                {
                    // Do nothing
                }
                else if (value instanceof List && ((List<?>) value).isEmpty())
                {
                    builder.append(": []");
                }
                else if (value instanceof List && ((List<?>) value).get(0) instanceof String)
                {
                    builder.append(": [").append(((List<?>) value).stream().map(v -> convertString((String) v, true)).collect(Collectors.joining(", "))).append("]");
                }
                else if (value instanceof List && ((List<?>) value).get(0) instanceof Long)
                {
                    builder.append(": [").append(((List<?>) value).stream().map(Object::toString).collect(Collectors.joining(", "))).append("]");
                }
                else
                {
                    throw new IllegalStateException("Unrecognized property value type: " + value.getClass().getSimpleName());
                }
            }
            else if (property.values.isEmpty())
            {
                builder.append(": []");
            }
            else
            {
                builder.append(": [");
                Object value = property.values.get(0);
                if (value instanceof String)
                {
                    builder.append(": ").append(convertString((String) value, true));
                }
                else if (value instanceof Long)
                {
                    builder.append(": ").append(value);
                }
                else
                {
                    throw new IllegalStateException("Unrecognized property array value type: " + value.getClass().getSimpleName());
                }
                for (int i = 1; i < property.values.size(); i++)
                {
                    Object nextValue = property.values.get(i);
                    if (!value.getClass().isInstance(nextValue))
                    {
                        throw new IllegalStateException("Inconsistent property array value types");
                    }
                    builder.append(", ").append(nextValue instanceof String ? convertString((String) value, true) : value);
                }

                builder.append("]");
            }
            builder.append(";");
        }
        if (section.recordType != null)
        {
            builder.append("\n\n  Record\n  {");
            for (FlatDataRecordField field : section.recordType.fields)
            {
                builder.append("\n    ").append(toGrammar(field)).append(";");
            }
            builder.append("\n  }");
        }
        return builder.append("\n}").toString();
    }


    public static String toGrammar(FlatDataRecordField field)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(field.label);
        if (field.address != null)
        {
            builder.append(" {").append(field.address).append("}");
        }
        builder.append(": ").append(toGrammar(field.type));
        return builder.toString();
    }

    public static String toGrammar(FlatDataDataType type)
    {
        StringBuilder builder = new StringBuilder();
        List<String> attributes = Lists.mutable.empty();
        if (type instanceof FlatDataBoolean)
        {
            builder.append("BOOLEAN");
            FlatDataBoolean bool = (FlatDataBoolean) type;
            if (bool.trueString != null)
            {
                attributes.add("trueString=" + convertString(bool.trueString, true));
            }
            if (bool.falseString != null)
            {
                attributes.add("falseString=" + convertString(bool.falseString, true));
            }
        }
        else if (type instanceof FlatDataDateTime)
        {
            builder.append("DATETIME");
            FlatDataDateTime dateTime = (FlatDataDateTime) type;
            if (dateTime.format != null && !dateTime.format.isEmpty())
            {
                attributes.add("format=" + convertValues(dateTime.format));
            }
            if (dateTime.timeZone != null)
            {
                attributes.add("timeZone=" + convertString(dateTime.timeZone, true));
            }
        }
        else if (type instanceof FlatDataDate)
        {
            builder.append("DATE");
            FlatDataDate date = (FlatDataDate) type;
            if (date.format != null && !date.format.isEmpty())
            {
                attributes.add("format=" + convertValues(date.format));
            }
        }
        else if (type instanceof FlatDataString)
        {
            builder.append("STRING");
        }
        else if (type instanceof FlatDataInteger)
        {
            builder.append("INTEGER");
        }
        else if (type instanceof FlatDataDecimal)
        {
            builder.append("DECIMAL");
        }
        if (type.optional)
        {
            attributes.add("optional");
        }
        if (!attributes.isEmpty())
        {
            builder.append("(").append(String.join(", ", attributes)).append(")");
        }
        return builder.toString();
    }

    public static String convertValue(Object value)
    {
        return value instanceof String ? convertString((String) value, true) : value.toString();
    }

    public static String convertValues(List<? extends Object> values)
    {
        if (values.size() == 1)
        {
            return convertValue(values.get(0));
        }
        else
        {
            return "[" + values.stream().map(FlatDataSchemaComposer::convertValue).collect(Collectors.joining(", ")) + "]";
        }
    }
}
