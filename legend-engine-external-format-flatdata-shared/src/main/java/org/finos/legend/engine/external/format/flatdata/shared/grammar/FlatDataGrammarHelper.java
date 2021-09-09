// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.grammar;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;

import java.util.List;

public class FlatDataGrammarHelper
{
    public static String toGrammar(FlatData flatData)
    {
        StringBuilder builder = new StringBuilder();
        flatData.getSections().forEach(s -> builder.append(toGrammar(s)));
        return builder.toString();
    }

    public static String toGrammar(FlatDataSection section)
    {
        StringBuilder builder = new StringBuilder("section ").append(section.getName()).append(": ").append(section.getDriverId()).append("\n{");
        for (FlatDataProperty property : section.getSectionProperties())
        {
            builder.append("\n  ").append(property.getName());
            if (property.getValue() instanceof String)
            {
                builder.append(": ").append(convertString((String) property.getValue()));
            }
            builder.append(";");
        }
        if (section.getRecordType() != null)
        {
            builder.append("\n\n  Record\n  {");
            for (FlatDataRecordField field : section.getRecordType().getFields())
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
        builder.append(field.getLabel());
        if (field.getAddress() != null)
        {
            builder.append(" {").append(field.getAddress()).append("}");
        }
        builder.append(": ").append(toGrammar(field.getType()));
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
            if (bool.getTrueString() != null)
            {
                attributes.add("trueString=" + convertString(bool.getTrueString()));
            }
            if (bool.getFalseString() != null)
            {
                attributes.add("falseString=" + convertString(bool.getFalseString()));
            }
        }
        else if (type instanceof FlatDataDateTime)
        {
            builder.append("DATETIME");
            FlatDataDateTime dateTime = (FlatDataDateTime) type;
            if (dateTime.getFormat() != null)
            {
                attributes.add("format=" + convertString(dateTime.getFormat()));
            }
            if (dateTime.getTimeZone() != null)
            {
                attributes.add("timeZone=" + convertString(dateTime.getTimeZone()));
            }
        }
        else if (type instanceof FlatDataDate)
        {
            builder.append("DATE");
            FlatDataDate date = (FlatDataDate) type;
            if (date.getFormat() != null)
            {
                attributes.add("format=" + convertString(date.getFormat()));
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
        if (type.isOptional())
        {
            attributes.add("optional");
        }
        if (!attributes.isEmpty())
        {
            builder.append("(").append(String.join(", ", attributes)).append(")");
        }
        return builder.toString();
    }

    public static String convertString(String val)
    {
        StringBuilder builder = (new StringBuilder()).append("'");
        val = StringEscapeUtils.escapeJava(val);
        val = val.replace("'", "\\'");
        val = val.replace("\\\"", "\"");
        builder.append(val).append("'");
        return builder.toString();
    }
}
