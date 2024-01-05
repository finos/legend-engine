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

package org.finos.legend.engine.repl.client.jline3;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;

import java.util.List;

public class JLine3Parser implements Parser
{
    @Override
    public ParsedLine parse(String value, int i, ParseContext parseContext) throws SyntaxError
    {
        MutableList<String> words = split(value);

        return new ParsedLine()
        {
            @Override
            public String word()
            {
                return words.get(wordIndex());
            }

            @Override
            public int wordCursor()
            {
                return word().length();
            }

            @Override
            public int wordIndex()
            {
                return words.size() - 1;
            }

            @Override
            public List<String> words()
            {
                return words;
            }

            @Override
            public String line()
            {
                return value;
            }

            @Override
            public int cursor()
            {
                return value.length();
            }
        };
    }

    @Override
    public ParsedLine parse(String line, int cursor) throws SyntaxError
    {
        throw new RuntimeException("");
    }

    @Override
    public boolean isEscapeChar(char ch)
    {
        return false;
    }

    @Override
    public boolean validCommandName(String name)
    {
        return false;
    }

    @Override
    public boolean validVariableName(String name)
    {
        return false;
    }

    @Override
    public String getCommand(String line)
    {
        return null;
    }

    @Override
    public String getVariable(String line)
    {
        return null;
    }


    public static MutableList<String> split(String content)
    {
        StringBuilder buffer = new StringBuilder();
        MutableList<String> result = Lists.mutable.empty();
        boolean lastCharactersIsToken = false;
        for (int i = 0; i < content.length(); i++)
        {
            char c = content.charAt(i);
            String token = shouldBreak(content, c, i);
            if (token != null)
            {
                flush(buffer, result);
                result.add(token);
                lastCharactersIsToken = true;
                buffer = new StringBuilder();
                i = i + token.length() - 1;
            }
            else
            {
                lastCharactersIsToken = false;
                buffer.append(c);
            }
        }
        flush(buffer, result);
        if (lastCharactersIsToken)
        {
            result.add("");
        }
        return result;
    }

    public static void flush(StringBuilder buffer, MutableList<String> result)
    {
        String value = buffer.toString();
        if (!value.isEmpty())
        {
            result.add(value);
        }
    }

    private static MutableCharSet block = CharSets.mutable.with('(', ')', ' ', '#', '.', ' ');

    public static String shouldBreak(String content, char c, int i)
    {
        if (block.contains(c))
        {
            return String.valueOf(c);
        }
        if (i < content.length() + 1 && c == '-' && content.charAt(i + 1) == '>')
        {
            return "->";
        }
        return null;
    }
}
