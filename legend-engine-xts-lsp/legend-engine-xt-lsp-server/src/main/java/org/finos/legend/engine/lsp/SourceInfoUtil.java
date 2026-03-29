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

package org.finos.legend.engine.lsp;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Utility for converting Pure SourceInformation (1-based) to LSP positions (0-based).
 * Eliminates duplication across NavigationProvider, ReferencesProvider,
 * DiagnosticsPublisher, and WorkspaceSymbolProvider.
 */
public class SourceInfoUtil
{
    public static Range toRange(SourceInformation si)
    {
        return new Range(
                new Position(Math.max(0, si.getStartLine() - 1), Math.max(0, si.getStartColumn() - 1)),
                new Position(Math.max(0, si.getEndLine() - 1), Math.max(0, si.getEndColumn()))
        );
    }

    public static Location toLocation(SourceInformation si, String uri)
    {
        return new Location(uri, toRange(si));
    }
}
