// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.repl.core;

/**
 * Exception thrown by the Pure REPL for various error conditions.
 */
public class ReplException extends Exception
{
    private final String source;
    private final Integer line;
    private final Integer column;
    private final String errorType;

    public ReplException(String message)
    {
        this(message, null, null, null, null, null);
    }

    public ReplException(String message, Throwable cause)
    {
        this(message, cause, null, null, null, null);
    }

    public ReplException(String message, String source, Integer line, Integer column)
    {
        this(message, null, source, line, column, null);
    }

    public ReplException(String message, Throwable cause, String source, Integer line, Integer column, String errorType)
    {
        super(message, cause);
        this.source = source;
        this.line = line;
        this.column = column;
        this.errorType = errorType;
    }

    public String getSource()
    {
        return source;
    }

    public Integer getLine()
    {
        return line;
    }

    public Integer getColumn()
    {
        return column;
    }

    public String getErrorType()
    {
        return errorType != null ? errorType : getClass().getSimpleName();
    }

    /**
     * Returns a formatted error message with source information if available.
     */
    public String getFormattedMessage()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        if (source != null || line != null || column != null)
        {
            sb.append(" (");
            if (source != null)
            {
                sb.append(source);
            }
            if (line != null)
            {
                if (source != null)
                {
                    sb.append(":");
                }
                sb.append("line ").append(line);
            }
            if (column != null)
            {
                sb.append(", column ").append(column);
            }
            sb.append(")");
        }

        return sb.toString();
    }
}
