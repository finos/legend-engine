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
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides go-to-definition by delegating to Source.navigate(),
 * the same API that PureIdeLight's Concept.java uses.
 */
public class NavigationProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationProvider.class);
    /**
     * Find the definition location for the element at the given position.
     * Returns null if no navigable element is found at that position.
     *
     * Line and column are 1-based (PureRuntime convention).
     */
    public static Location definition(PureRuntime runtime, UriMapper uriMapper, String sourceId, int line, int column)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return null;
        }

        CoreInstance raw = source.navigate(line, column, runtime.getProcessorSupport());
        if (raw == null)
        {
            return null;
        }

        // Resolve ImportStubs to their actual targets so we navigate to the definition, not the reference
        CoreInstance found = ImportStub.withImportStubByPass(raw, runtime.getProcessorSupport());
        if (found == null)
        {
            return null;
        }

        SourceInformation defLocation = found.getSourceInformation();
        if (defLocation == null)
        {
            return null;
        }

        String defUri = uriMapper.toUri(defLocation.getSourceId());
        if (defUri == null)
        {
            LOGGER.warn("Cannot resolve source ID '{}' to filesystem URI", defLocation.getSourceId());
            LOGGER.info("Go-to-definition: cannot resolve source '"
                    + defLocation.getSourceId() + "' to filesystem URI. Check workspace root and repo scanner.");
            return null;
        }

        return SourceInfoUtil.toLocation(defLocation, defUri);
    }
}
