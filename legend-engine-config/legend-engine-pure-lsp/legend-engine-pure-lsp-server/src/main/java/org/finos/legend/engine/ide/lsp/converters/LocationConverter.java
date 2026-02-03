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

package org.finos.legend.engine.ide.lsp.converters;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Converter for Pure SourceInformation to LSP Location types.
 */
public class LocationConverter
{
    private LocationConverter()
    {
        // Utility class - no instantiation
    }

    /**
     * Convert Pure SourceInformation to an LSP Location.
     */
    public static Location toLocation(SourceInformation sourceInfo)
    {
        if (sourceInfo == null)
        {
            return null;
        }

        String uri = URIUtils.sourceIdToUri(sourceInfo.getSourceId());
        Range range = PositionUtils.sourceInfoToRange(sourceInfo);
        return new Location(uri, range);
    }

    /**
     * Create an LSP Location from source ID and Pure coordinates.
     */
    public static Location toLocation(String sourceId, int line, int column)
    {
        String uri = URIUtils.sourceIdToUri(sourceId);
        Range range = PositionUtils.purePositionToRange(line, column);
        return new Location(uri, range);
    }

    /**
     * Create an LSP Location from source ID and Pure range coordinates.
     */
    public static Location toLocation(String sourceId, int startLine, int startColumn, int endLine, int endColumn)
    {
        String uri = URIUtils.sourceIdToUri(sourceId);
        Range range = PositionUtils.pureToLspRange(startLine, startColumn, endLine, endColumn);
        return new Location(uri, range);
    }

    /**
     * Convert Pure SourceInformation to an LSP LocationLink.
     * The origin selection range represents the text that triggered the navigation.
     */
    public static LocationLink toLocationLink(SourceInformation targetSourceInfo, Range originSelectionRange)
    {
        if (targetSourceInfo == null)
        {
            return null;
        }

        String targetUri = URIUtils.sourceIdToUri(targetSourceInfo.getSourceId());
        Range targetRange = PositionUtils.sourceInfoToRange(targetSourceInfo);
        Range targetSelectionRange = PositionUtils.pureToLspRange(
            targetSourceInfo.getLine(),
            targetSourceInfo.getColumn(),
            targetSourceInfo.getLine(),
            targetSourceInfo.getColumn()
        );

        LocationLink link = new LocationLink();
        link.setTargetUri(targetUri);
        link.setTargetRange(targetRange);
        link.setTargetSelectionRange(targetSelectionRange);
        link.setOriginSelectionRange(originSelectionRange);
        return link;
    }

    /**
     * Create an LSP LocationLink from individual components.
     */
    public static LocationLink toLocationLink(
        String targetSourceId,
        int targetStartLine,
        int targetStartColumn,
        int targetEndLine,
        int targetEndColumn,
        Range originSelectionRange)
    {
        String targetUri = URIUtils.sourceIdToUri(targetSourceId);
        Range targetRange = PositionUtils.pureToLspRange(targetStartLine, targetStartColumn, targetEndLine, targetEndColumn);
        Range targetSelectionRange = PositionUtils.purePositionToRange(targetStartLine, targetStartColumn);

        LocationLink link = new LocationLink();
        link.setTargetUri(targetUri);
        link.setTargetRange(targetRange);
        link.setTargetSelectionRange(targetSelectionRange);
        link.setOriginSelectionRange(originSelectionRange);
        return link;
    }
}
