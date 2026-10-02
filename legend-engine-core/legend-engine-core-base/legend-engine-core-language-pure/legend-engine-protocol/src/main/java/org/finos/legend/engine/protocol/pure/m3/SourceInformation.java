// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.m3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SourceInformation
{
    public String sourceId;
    public int startLine;
    public int startColumn;
    public int endLine;
    public int endColumn;

    @JsonIgnore
    private static final SourceInformation unknown = new SourceInformation("X", 0, 0, 0, 0);

    public SourceInformation()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public SourceInformation(String sourceId, int startLine, int startColumn, int endLine, int endColumn)
    {
        this.sourceId = sourceId;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    /**
     * Reads the compact form a position can also take in JSON, as the Rust grammar parser writes it:
     * {@code sourceId:line:column-column} when it starts and ends on one line, {@code sourceId:line:column-line:column}
     * otherwise. Lines and columns are 1-based and the end column is inclusive, as in the object form. A source id may
     * itself hold ':' and '-', so the text is read from its end, where the numbers are.
     *
     * @throws IllegalArgumentException if the text is not in that form
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SourceInformation fromString(String text)
    {
        CompactReader reader = new CompactReader(text);
        int endColumn = reader.number();
        int startLine;
        int startColumn;
        int endLine;
        if (reader.take('-'))
        {
            // line:column-column
            startColumn = reader.number();
            reader.expect(':');
            startLine = reader.number();
            endLine = startLine;
        }
        else
        {
            // line:column-line:column
            reader.expect(':');
            endLine = reader.number();
            reader.expect('-');
            startColumn = reader.number();
            reader.expect(':');
            startLine = reader.number();
        }
        reader.expect(':');
        return new SourceInformation(reader.rest(), startLine, startColumn, endLine, endColumn);
    }

    /**
     * Reads a compact position from its end.
     */
    private static final class CompactReader
    {
        private final String text;
        private int end;

        CompactReader(String text)
        {
            this.text = text;
            this.end = text.length();
        }

        /**
         * Takes the number the text left ends with.
         */
        int number()
        {
            int start = this.end;
            while (start > 0 && this.text.charAt(start - 1) >= '0' && this.text.charAt(start - 1) <= '9')
            {
                start--;
            }
            if (start == this.end || this.end - start > 10)
            {
                throw invalid();
            }
            long value = 0;
            for (int i = start; i < this.end; i++)
            {
                value = value * 10 + (this.text.charAt(i) - '0');
            }
            if (value > Integer.MAX_VALUE)
            {
                throw invalid();
            }
            this.end = start;
            return (int) value;
        }

        /**
         * Takes the separator the text left ends with, if it is that one.
         */
        boolean take(char separator)
        {
            if (this.end > 0 && this.text.charAt(this.end - 1) == separator)
            {
                this.end--;
                return true;
            }
            return false;
        }

        void expect(char separator)
        {
            if (!take(separator))
            {
                throw invalid();
            }
        }

        String rest()
        {
            return this.text.substring(0, this.end);
        }

        private IllegalArgumentException invalid()
        {
            return new IllegalArgumentException("Not a source information: '" + this.text + "' (expected sourceId:line:column-column or sourceId:line:column-line:column)");
        }
    }

    @JsonIgnore
    public String getMessage()
    {
        StringBuilder builder = new StringBuilder(this.sourceId.length() + 16);
        builder.append('[');
        if (this.startLine == this.endLine)
        {
            builder.append(this.startLine);
            builder.append(':');
            if (this.startColumn == this.endColumn)
            {
                builder.append(this.startColumn);
            }
            else
            {
                builder.append(this.startColumn);
                builder.append('-');
                builder.append(this.endColumn);
            }
            builder.append(']');
        }
        else
        {
            builder.append(this.startLine);
            builder.append(':');
            builder.append(this.startColumn);
            builder.append('-');
            builder.append(this.endLine);
            builder.append(':');
            builder.append(this.endColumn);
            builder.append(']');
        }
        return builder.toString();
    }

    public static SourceInformation getUnknownSourceInformation()
    {
        return SourceInformation.unknown;
    }
}
