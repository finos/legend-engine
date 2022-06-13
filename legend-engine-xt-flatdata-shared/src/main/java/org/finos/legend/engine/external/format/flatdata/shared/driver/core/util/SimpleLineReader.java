//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;

import java.util.function.LongSupplier;

public class SimpleLineReader implements LineReader
{
    protected final CharCursor cursor;
    private final LongSupplier lineNumberSupplier;
    private final EolCheck isEol;
    private final Runnable consumeEol;
    private boolean lastLineEndedInEol = false;
    private boolean lastBlankRowReturned = false;

    public SimpleLineReader(CharCursor cursor, String eol, LongSupplier lineNumberSupplier)
    {
        this.cursor = cursor;
        this.lineNumberSupplier = lineNumberSupplier;

        if (eol == null)
        {
            isEol = from -> cursor.peek(from + 1) == '\n' || cursor.peek(from + 1) == '\r';
            consumeEol = () ->
            {
                int ch1 = cursor.advance();
                if (ch1 == '\r' && cursor.peek(1) == '\n')
                {
                    cursor.advance();
                }
            };
        }
        else if (eol.length() == 1)
        {
            char eolChar = eol.charAt(0);
            isEol = from -> cursor.peek(from + 1) == eolChar;
            consumeEol = cursor::advance;
        }
        else if (eol.length() == 2)
        {
            char eolChar0 = eol.charAt(0);
            char eolChar1 = eol.charAt(1);
            isEol = from -> cursor.peek(from + 1) == eolChar0 && cursor.peek(from + 2) == eolChar1;
            consumeEol = () -> cursor.advance(2);
        }
        else
        {
            throw new IllegalStateException("Specified EOL must be 1 or 2 chars only");
        }
    }

    @Override
    public SimpleLine readLine()
    {
        if (cursor.isEndOfData())
        {
            if (lastLineEndedInEol && !lastBlankRowReturned)
            {
                lastBlankRowReturned = true;
                return new SimpleLine(lineNumberSupplier.getAsLong(), "");
            }
            throw new IllegalStateException("Unexpected EOF Reached");
        }

        int chars = 0;
        while (cursor.peek(chars + 1) != CharCursor.END_OF_DATA && !isEol.check(chars))
        {
            chars++;
        }
        String text = new String(cursor.advance(chars));
        if (isEol.check(0))
        {
            lastLineEndedInEol = true;
            consumeEol.run();
        }
        return new SimpleLine(lineNumberSupplier.getAsLong(), text);
    }

    private interface EolCheck
    {
        boolean check(int from);
    }

}
