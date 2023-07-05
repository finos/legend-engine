// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.sqldom.utils;

public class SqlGenUtils
{

    public static final String WHITE_SPACE = " ";
    public static final String COMMA = ",";
    public static final String OPEN_PARENTHESIS = "(";
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String EMPTY = "";
    public static final String QUOTE_IDENTIFIER = "\"%s\"";
    public static final String SINGLE_QUOTE_IDENTIFIER = "'%s'";
    public static final String BACK_QUOTE_IDENTIFIER = "`%s`";
    public static final String ASSIGNMENT_OPERATOR = "=";
    public static final String DOT = ".";

    public static String getQuotedField(String columnName, String quoteIdentifier)
    {
        return String.format(quoteIdentifier, columnName);
    }

    public static String getQuotedField(String columnName)
    {
        return String.format(QUOTE_IDENTIFIER, columnName);
    }

    public static String singleQuote(Object val)
    {
        return String.format(SINGLE_QUOTE_IDENTIFIER, val);
    }

}
