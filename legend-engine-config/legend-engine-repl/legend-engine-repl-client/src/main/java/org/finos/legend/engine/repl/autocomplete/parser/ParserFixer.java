// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.autocomplete.parser;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.utility.StringIterate;

public class ParserFixer
{
    public static String magicToken = "MaGiCToKeN";

    public static String fixCode(String value)
    {
        value = fixComma(value);
        value = fixTilde(value);
        value = fixPlus(value);
        value = fixMinus(value);
        value = fixMul(value);
        value = fixDiv(value);
        value = fixExclamation(value);
        value = fixColon(value);
        value = fixDot(value);
        value = fixPipe(value);
        value = fixIsland(value);
        value = fixArrow(value);
        value = fixParenthesis(value);
        return value;
    }


    public static String fixIncomplete(String value, String token)
    {
        return fixIncomplete(value, token, magicToken);
    }

    public static String fixIncomplete(String value, String token, String add)
    {
        String tail = value.substring(value.lastIndexOf(token) + 1).trim();
        if (tail.isEmpty())
        {
            value = value + add;
        }
        return value;
    }

    public static String fixColon(String value)
    {
        return fixIncomplete(value, ":", "x|" + magicToken);
    }

    public static String fixComma(String value)
    {
        return fixIncomplete(value, ",");
    }

    public static String fixDot(String value)
    {
        return fixIncomplete(value, ".");
    }

    public static String fixPlus(String value)
    {
        return fixIncomplete(value, "+");
    }

    public static String fixMinus(String value)
    {
        return fixIncomplete(value, "-");
    }

    public static String fixMul(String value)
    {
        return fixIncomplete(value, "*");
    }

    public static String fixDiv(String value)
    {
        return fixIncomplete(value, "/");
    }

    public static String fixExclamation(String value)
    {
        return fixIncomplete(value, "!");
    }

    public static String fixTilde(String value)
    {
        String tail = value.substring(value.lastIndexOf("~") + 1).trim();
        if (tail.isEmpty() || tail.equals("["))
        {
            value = value + magicToken;
        }
        return value;
    }

    public static String fixPipe(String value)
    {
        return fixIncomplete(value, "|");
    }

    public static String fixArrow(String value)
    {
        if (value.lastIndexOf("->") != -1)
        {
            String tail = value.substring(value.lastIndexOf("->") + 2);
            if (tail.lastIndexOf("(") == -1)
            {
                if (value.endsWith(">"))
                {
                    value = value + magicToken + "()";
                }
                else
                {
                    value = value + "()";
                }
            }
        }
        return value;
    }

    public static String fixParenthesis(String value)
    {
        MutableStack<Character> stack = Stacks.mutable.empty();
        MutableStack<Boolean> quoteState = Stacks.mutable.empty();
        StringIterate.forEachChar(value, c ->
        {
            switch (c)
            {
                case '\'':
                    if (quoteState.isEmpty())
                    {
                        stack.push('\'');
                        quoteState.push(true);
                    }
                    else
                    {
                        stack.pop();
                        quoteState.pop();
                    }
                    break;
                case '{':
                    stack.push('}');
                    break;
                case '(':
                    stack.push(')');
                    break;
                case '[':
                    stack.push(']');
                    break;
                case ']':
                case ')':
                case '}':
                    if (!stack.isEmpty())
                    {
                        stack.pop();
                    }
                    break;
            }
        });
        return value + stack.makeString("");
    }

    public static String fixIsland(String value)
    {
        boolean buffer = false;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);
            if (c == '#' && !buffer)
            {
                buffer = true;
                builder.append(value.charAt(i));
            }
            else if (buffer)
            {
                if (c == '#')
                {
                    buffer = false;
                    builder = new StringBuilder();
                }
                else
                {
                    builder.append(value.charAt(i));
                }
            }
        }
        if (buffer)
        {
            String content = builder.toString();
            if (content.equals("#"))
            {
                return value + magicToken + "{}#";
            }
            if (content.contains("{") && !content.contains("}"))
            {
                return (value.contains(magicToken) ? value : value + magicToken) + "}#";
            }
            if (content.contains("{") && content.contains("}"))
            {
                return value + "#";
            }
            return value + "{" + magicToken + "}#";
        }
        return value;

    }
}
