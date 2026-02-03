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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.finos.legend.engine.ide.lsp.converters.LocationConverter;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles go-to-definition requests.
 */
public class DefinitionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefinitionHandler.class);

    private final LSPSession session;

    public DefinitionHandler(LSPSession session)
    {
        this.session = session;
    }

    /**
     * Handle textDocument/definition request.
     */
    public Either<List<? extends Location>, List<? extends LocationLink>> getDefinition(DefinitionParams params)
    {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();

        LOGGER.debug("Definition request for {} at line {}, column {}",
            uri, position.getLine(), position.getCharacter());

        String sourceId = URIUtils.uriToSourceId(uri);
        int pureLine = PositionUtils.lspPositionToPureLine(position);
        int pureColumn = PositionUtils.lspPositionToPureColumn(position);

        try
        {
            CoreInstance found = session.navigate(sourceId, pureLine, pureColumn);

            if (found == null)
            {
                LOGGER.debug("No element found at position");
                return Either.forLeft(Lists.mutable.empty());
            }

            SourceInformation sourceInfo = found.getSourceInformation();
            if (sourceInfo == null)
            {
                LOGGER.debug("Element has no source information: {}", found);
                return Either.forLeft(Lists.mutable.empty());
            }

            Location location = LocationConverter.toLocation(sourceInfo);
            MutableList<Location> locations = Lists.mutable.with(location);

            LOGGER.debug("Found definition at {}:{}:{}",
                sourceInfo.getSourceId(), sourceInfo.getLine(), sourceInfo.getColumn());

            return Either.forLeft(locations);
        }
        catch (Exception e)
        {
            LOGGER.error("Error finding definition", e);
            return Either.forLeft(Lists.mutable.empty());
        }
    }
}
