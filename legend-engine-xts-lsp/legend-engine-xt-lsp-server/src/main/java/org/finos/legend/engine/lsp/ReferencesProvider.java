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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides find-all-references by reading the referenceUsages property
 * that the Pure compiler populates on every element during compilation.
 */
public class ReferencesProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProvider.class);
    /**
     * Find all references to the element at the given position.
     * Line and column are 1-based (PureRuntime convention).
     */
    private static final int MAX_REFERENCES = 1000;

    public static List<Location> references(PureRuntime runtime, UriMapper uriMapper,
                                            String sourceId, int line, int column,
                                            boolean includeDeclaration)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        CoreInstance raw = source.navigate(line, column, runtime.getProcessorSupport());
        if (raw == null)
        {
            return Collections.emptyList();
        }

        // Resolve ImportStubs to get the actual element
        CoreInstance element = ImportStub.withImportStubByPass(raw, runtime.getProcessorSupport());
        if (element == null)
        {
            return Collections.emptyList();
        }

        List<Location> locations = new ArrayList<>();

        // Optionally include the declaration itself
        if (includeDeclaration)
        {
            SourceInformation defSi = element.getSourceInformation();
            if (defSi != null)
            {
                Location defLoc = toLocation(defSi, uriMapper);
                if (defLoc != null)
                {
                    locations.add(defLoc);
                }
            }
        }

        // Get all reference usages from the compiler
        ListIterable<? extends CoreInstance> refUsages =
                element.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
        if (refUsages != null)
        {
            for (CoreInstance refUsage : refUsages)
            {
                if (locations.size() >= MAX_REFERENCES)
                {
                    LspLog.debug("references: truncated at " + MAX_REFERENCES + " results");
                    break;
                }
                CoreInstance owner = refUsage.getValueForMetaPropertyToOne(M3Properties.owner);
                if (owner == null)
                {
                    continue;
                }

                SourceInformation ownerSi = owner.getSourceInformation();
                if (ownerSi == null)
                {
                    continue;
                }

                Location loc = toLocation(ownerSi, uriMapper);
                if (loc != null)
                {
                    locations.add(loc);
                }
            }
        }

        return locations;
    }

    private static Location toLocation(SourceInformation si, UriMapper uriMapper)
    {
        String uri = uriMapper.toUri(si.getSourceId());
        if (uri == null)
        {
            return null;
        }

        return SourceInfoUtil.toLocation(si, uri);
    }
}
