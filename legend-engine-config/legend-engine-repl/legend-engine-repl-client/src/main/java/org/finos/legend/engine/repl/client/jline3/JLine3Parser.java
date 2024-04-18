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
    public ParsedLine parse(String value, int cursor, ParseContext parseContext) throws SyntaxError
    {
        return new MyParsedLine(split(value, cursor));
    }

    public static class MyParsedLine implements ParsedLine
    {
        private ParserResult result;

        public MyParsedLine(ParserResult result)
        {
            this.result = result;
        }

        @Override
        public String word()
        {
            int index = wordIndex();
            if (result.words.size() > index)
            {
                return result.words.get(index);
            }
            else
            {
                return "";
            }
        }

        @Override
        public int wordCursor()
        {
            return result.wordCursor;
        }

        @Override
        public int wordIndex()
        {
            return result.currentWordIndex;
        }

        @Override
        public List<String> words()
        {
            return result.words;
        }

        @Override
        public String line()
        {
            return result.line;
        }

        @Override
        public int cursor()
        {
            return result.cursor;
        }
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


    public static ParserResult split(String content, int cursor)
    {
        StringBuilder buffer = new StringBuilder();
        MutableList<String> result = Lists.mutable.empty();
        boolean lastCharactersIsToken = false;
        int currentWordIndex = -1;
        int currentWordCursor = -1;
        int wordCursor = -1;
        boolean inQuote = false;
        for (int i = 0; i < content.length(); i++)
        {
            char c = content.charAt(i);
            inQuote = (c == '\'') != inQuote;
            String token = null;
            if (!inQuote)
            {
                token = shouldBreak(content, c, i);
            }
            if (token != null)
            {
                flush(buffer, result);
                result.add(token);
                wordCursor = 0;
                lastCharactersIsToken = true;
                buffer = new StringBuilder();
                i = i + token.length() - 1;
            }
            else
            {
                lastCharactersIsToken = false;
                buffer.append(c);
                wordCursor++;
            }
            if (cursor == i + 1)
            {
                currentWordIndex = result.size();
                currentWordCursor = wordCursor;
            }
        }
        flush(buffer, result);
        if (lastCharactersIsToken)
        {
            result.add("");
        }

        return new ParserResult(content, cursor, result, currentWordIndex, currentWordCursor);
    }

    public static void flush(StringBuilder buffer, MutableList<String> result)
    {
        String value = buffer.toString();
        if (!value.isEmpty())
        {
            result.add(value);
        }
    }

    private static MutableCharSet block = CharSets.mutable.with('(', ')', ' ', '#', '.', ' ', '~', ',', '[', ']');

    public static String shouldBreak(String content, char c, int i)
    {
        if (block.contains(c))
        {
            return String.valueOf(c);
        }
        if (i < (content.length() - 1) && c == '-' && content.charAt(i + 1) == '>')
        {
            return "->";
        }
        return null;
    }

    public static class ParserResult
    {
        MutableList<String> words;
        int currentWordIndex;
        int wordCursor;
        String line;
        int cursor;

        public ParserResult(String line, int cursor, MutableList<String> words, int currentWordIndex, int wordCursor)
        {
            this.line = line;
            this.cursor = cursor;
            this.words = words;
            this.currentWordIndex = currentWordIndex;
            this.wordCursor = wordCursor;
        }

        public ParserResult(String line, MutableList<String> words)
        {
            this.line = line;
            this.cursor = line.length();
            this.words = words;
            this.currentWordIndex = words.size() - 1;
            this.wordCursor = words.get(this.currentWordIndex).length();
        }
    }
}
