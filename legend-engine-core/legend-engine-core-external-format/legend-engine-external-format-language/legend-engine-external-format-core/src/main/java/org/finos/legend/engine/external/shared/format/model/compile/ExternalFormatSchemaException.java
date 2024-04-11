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

package org.finos.legend.engine.external.shared.format.model.compile;

import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;

/**
 * Implementations of {@link ExternalFormatExtension} can throw this exception to indicate an
 * error with the external schema text supplied in the schema. Positions used to do so are one-based
 * line/column numbers of the external schema text.
 */
public class ExternalFormatSchemaException extends RuntimeException
{
    public ExternalFormatSchemaException(String message)
    {
        super("Error in schema content: " + message);
    }

    public ExternalFormatSchemaException(String message, int startLineNumber, int startColumnNumber)
    {
        super("Error in schema content " + positionText(startLineNumber, startColumnNumber, startLineNumber, startColumnNumber) + ": " + message);
    }

    public ExternalFormatSchemaException(String message, int startLineNumber, int startColumnNumber, int endLineNumber, int endColumnNumber)
    {
        super("Error in schema content " + positionText(startLineNumber, startColumnNumber, endLineNumber, endColumnNumber) + ": " + message);
    }

    private static String positionText(int startLine, int startColumn, int endLine, int endColumn)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (startLine == endLine)
        {
            builder.append(startLine).append(':').append(startColumn);
            if (startColumn != endColumn)
            {
                builder.append('-').append(endColumn);
            }
        }
        else
        {
            builder.append(startLine).append(':').append(startColumn).append('-').append(endLine).append(':').append(endColumn);
        }
        return builder.append(']').toString();
    }

}
