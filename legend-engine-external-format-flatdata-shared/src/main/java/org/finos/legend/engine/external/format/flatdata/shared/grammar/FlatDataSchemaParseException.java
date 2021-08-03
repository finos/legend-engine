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

package org.finos.legend.engine.external.format.flatdata.shared.grammar;

public class FlatDataSchemaParseException extends RuntimeException
{

    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;

    public FlatDataSchemaParseException(String message, int lineNumber, int columnNumber)
    {
        this(message, lineNumber, columnNumber, lineNumber, columnNumber);
    }

    public FlatDataSchemaParseException(String message, int startLine, int startColumn, int endLine, int endColumn)
    {
        super(message);
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public int getStartLine()
    {
        return startLine;
    }

    public int getStartColumn()
    {
        return startColumn;
    }

    public int getEndLine()
    {
        return endLine;
    }

    public int getEndColumn()
    {
        return endColumn;
    }
}
