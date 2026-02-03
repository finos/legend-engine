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

package org.finos.legend.engine.ide.lsp.utils;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Utility class for converting between Pure positions (1-based) and LSP positions (0-based).
 *
 * Pure uses 1-based line and column numbers (first line is 1, first column is 1).
 * LSP uses 0-based line and column numbers (first line is 0, first column is 0).
 */
public class PositionUtils
{
    private PositionUtils()
    {
        // Utility class - no instantiation
    }

    /**
     * Convert Pure line number (1-based) to LSP line number (0-based).
     */
    public static int pureLineToLsp(int pureLine)
    {
        return pureLine - 1;
    }

    /**
     * Convert Pure column number (1-based) to LSP column number (0-based).
     */
    public static int pureColumnToLsp(int pureColumn)
    {
        return pureColumn - 1;
    }

    /**
     * Convert LSP line number (0-based) to Pure line number (1-based).
     */
    public static int lspLineToPure(int lspLine)
    {
        return lspLine + 1;
    }

    /**
     * Convert LSP column number (0-based) to Pure column number (1-based).
     */
    public static int lspColumnToPure(int lspColumn)
    {
        return lspColumn + 1;
    }

    /**
     * Convert LSP Position (0-based) to Pure line number (1-based).
     */
    public static int lspPositionToPureLine(Position position)
    {
        return lspLineToPure(position.getLine());
    }

    /**
     * Convert LSP Position (0-based) to Pure column number (1-based).
     */
    public static int lspPositionToPureColumn(Position position)
    {
        return lspColumnToPure(position.getCharacter());
    }

    /**
     * Create an LSP Position from Pure line and column (converting from 1-based to 0-based).
     */
    public static Position pureToLspPosition(int pureLine, int pureColumn)
    {
        return new Position(pureLineToLsp(pureLine), pureColumnToLsp(pureColumn));
    }

    /**
     * Create an LSP Range from Pure source information.
     */
    public static Range sourceInfoToRange(SourceInformation sourceInfo)
    {
        if (sourceInfo == null)
        {
            return new Range(new Position(0, 0), new Position(0, 0));
        }
        Position start = pureToLspPosition(sourceInfo.getStartLine(), sourceInfo.getStartColumn());
        Position end = pureToLspPosition(sourceInfo.getEndLine(), sourceInfo.getEndColumn() + 1);
        return new Range(start, end);
    }

    /**
     * Create an LSP Range from Pure line and column information.
     */
    public static Range pureToLspRange(int startLine, int startColumn, int endLine, int endColumn)
    {
        Position start = pureToLspPosition(startLine, startColumn);
        Position end = pureToLspPosition(endLine, endColumn + 1);
        return new Range(start, end);
    }

    /**
     * Create a single-character LSP Range at the given Pure position.
     */
    public static Range purePositionToRange(int pureLine, int pureColumn)
    {
        Position pos = pureToLspPosition(pureLine, pureColumn);
        return new Range(pos, new Position(pos.getLine(), pos.getCharacter() + 1));
    }
}
