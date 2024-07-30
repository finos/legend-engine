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

package org.finos.legend.engine.protocol.pure.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SourceInformation))
        {
            return false;
        }
        SourceInformation that = (SourceInformation) o;
        return startLine == that.startLine && startColumn == that.startColumn && endLine == that.endLine && endColumn == that.endColumn && Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceId, startLine, startColumn, endLine, endColumn);
    }

    public String toString()
    {
        return this.appendMessage(new StringBuilder("<SourceInformation ")).append('>').toString();
    }

    public StringBuilder appendMessage(StringBuilder appendable)
    {
        appendable.append(this.sourceId).append(':');
        if (this.startLine == this.endLine)
        {
            appendable.append(this.startLine);
            if (this.startColumn == this.endColumn)
            {
                appendable.append('c').append(this.startColumn);
            }
            else
            {
                appendable.append("cc").append(this.startColumn).append('-').append(this.endColumn);
            }
        }
        else
        {
            appendable.append(this.startLine).append('c').append(this.startColumn).append('-').append(this.endLine).append('c').append(this.endColumn);
        }

        return appendable;
    }

    public static SourceInformation getUnknownSourceInformation()
    {
        return SourceInformation.unknown;
    }
}
