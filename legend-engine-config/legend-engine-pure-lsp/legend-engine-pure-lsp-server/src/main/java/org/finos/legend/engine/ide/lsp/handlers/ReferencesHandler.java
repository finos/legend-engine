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

package org.finos.legend.engine.ide.lsp.handlers;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceParams;
import org.finos.legend.engine.ide.lsp.converters.LocationConverter;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles find-references requests.
 */
public class ReferencesHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesHandler.class);

    private final LSPSession session;

    public ReferencesHandler(LSPSession session)
    {
        this.session = session;
    }

    /**
     * Handle textDocument/references request.
     */
    public List<? extends Location> getReferences(ReferenceParams params)
    {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();

        LOGGER.debug("References request for {} at line {}, column {}",
            uri, position.getLine(), position.getCharacter());

        String sourceId = URIUtils.uriToSourceId(uri);
        int pureLine = PositionUtils.lspPositionToPureLine(position);
        int pureColumn = PositionUtils.lspPositionToPureColumn(position);

        MutableList<Location> locations = Lists.mutable.empty();

        try
        {
            // First navigate to get the element at the cursor
            CoreInstance found = session.navigate(sourceId, pureLine, pureColumn);

            if (found == null)
            {
                LOGGER.debug("No element found at position");
                return locations;
            }

            // Get the element's qualified name for searching
            String searchTerm = getSearchTerm(found);
            if (searchTerm == null || searchTerm.isEmpty())
            {
                return locations;
            }

            LOGGER.debug("Searching for references to: {}", searchTerm);

            // Search for references using the source registry
            RichIterable<SourceCoordinates> results = session.getPureRuntime()
                .getSourceRegistry()
                .find(searchTerm, true, null);

            // Convert to LSP locations
            if (results != null && results.notEmpty())
            {
                Multimap<String, SourceCoordinates> bySource = results.groupBy(SourceCoordinates.SOURCE_ID);

                bySource.forEachKeyMultiValues((srcId, coordinates) ->
                {
                    for (SourceCoordinates coord : coordinates)
                    {
                        Location location = LocationConverter.toLocation(
                            srcId,
                            coord.getStartLine(),
                            coord.getStartColumn(),
                            coord.getEndLine(),
                            coord.getEndColumn()
                        );
                        locations.add(location);
                    }
                });
            }

            // Include declaration if requested
            if (params.getContext().isIncludeDeclaration() && found.getSourceInformation() != null)
            {
                Location declarationLocation = LocationConverter.toLocation(found.getSourceInformation());
                if (!locations.contains(declarationLocation))
                {
                    locations.add(0, declarationLocation);
                }
            }

            LOGGER.debug("Found {} references", locations.size());
        }
        catch (Exception e)
        {
            LOGGER.error("Error finding references", e);
        }

        return locations;
    }

    private String getSearchTerm(CoreInstance element)
    {
        // Get the simple name for searching
        String path = PackageableElement.getUserPathForPackageableElement(element);
        if (path != null && !path.isEmpty())
        {
            // Use the last part of the path (simple name)
            int lastSep = path.lastIndexOf("::");
            if (lastSep >= 0)
            {
                return path.substring(lastSep + 2);
            }
            return path;
        }

        // Try to get name property
        try
        {
            CoreInstance name = element.getValueForMetaPropertyToOne("name");
            if (name != null)
            {
                return name.getName();
            }
        }
        catch (Exception e)
        {
            // Element doesn't have name property
        }

        return null;
    }
}
