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

package org.finos.legend.engine.language.mongodb.schema.grammar.to;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;

public class ComposerUtility
{
    public static final String TAB = "  ";

    public static int getTabSize()
    {
        return TAB.length();
    }

    public static int getTabSize(int repeat)
    {
        return repeat * getTabSize();
    }

    public static String getTabString()
    {
        return TAB;
    }

    public static String getTabString(int repeat)
    {
        return (repeat == 1) ? getTabString() : appendTabString(new StringBuilder(getTabSize(repeat)), repeat).toString();
    }

    public static StringBuilder appendTabString(StringBuilder builder)
    {
        return builder.append(getTabString());
    }

    /**
     * NOTE: This is a more efficient way than just chaining append() as it lessens the potential number of resize to the internal
     * array maintained by StringBuilder
     */
    public static StringBuilder appendTabString(StringBuilder builder, int repeat)
    {
        if (repeat == 1)
        {
            return appendTabString(builder);
        }
        builder.ensureCapacity(builder.length() + getTabSize(repeat));
        for (int i = 0; i < repeat; i++)
        {
            appendTabString(builder);
        }
        return builder;
    }


    public static String convertToStringWithQuotes(String val)
    {
        StringBuilder builder = new StringBuilder();
        appendStringWithQuotes(builder, val);
        return builder.toString();
    }

    public static void appendStringWithQuotes(StringBuilder builder, String val)
    {
        builder.append("\"");
        builder.append(val);
        builder.append("\"");
    }


    public static void appendJsonKey(StringBuilder builder, String val)
    {
        appendStringWithQuotes(builder, val);
        builder.append(": ");
    }

    public static String lowerCaseOperatorAndAddDollar(Operator operator)
    {
        return "$" + operator.toString().toLowerCase();
    }
}
