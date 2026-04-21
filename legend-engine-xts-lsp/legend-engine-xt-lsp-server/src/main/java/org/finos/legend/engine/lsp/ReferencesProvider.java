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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * Provides find-all-references using the same approach as PureIdeLight's findUsages:
 * - For functions: reads both {@code applications} (call sites) and
 *   {@code referenceUsages} (structural type references)
 * - For other elements: reads {@code referenceUsages}
 *
 * See {@code pure_ide/findUsage.pure} in legend-engine for the reference implementation.
 */
public class ReferencesProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProvider.class);
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

        CoreInstance element = ImportStub.withImportStubByPass(raw, runtime.getProcessorSupport());
        if (element == null)
        {
            return Collections.emptyList();
        }

        String classifierName = element.getClassifier() != null
                ? element.getClassifier().getName() : "";

        // Deduplicate by source location string (sourceId:startLine:startCol)
        Set<String> seen = new HashSet<>();
        List<Location> locations = new ArrayList<>();

        // Optionally include the declaration itself
        if (includeDeclaration)
        {
            SourceInformation defSi = element.getSourceInformation();
            if (defSi != null)
            {
                addLocation(locations, seen, defSi, uriMapper);
            }
        }

        // For functions: read 'applications' (call sites) — same as PureIdeLight's
        // $f.applications->evaluateAndDeactivate()
        // Note: we check the classifier name, NOT Java instanceof, because Pure's
        // runtime graph objects are often plain CoreInstance and don't implement
        // the PackageableFunction Java interface.
        if ("ConcreteFunctionDefinition".equals(classifierName)
                || "NativeFunction".equals(classifierName))
        {
            ListIterable<? extends CoreInstance> applications =
                    element.getValueForMetaPropertyToMany(M3Properties.applications);
            if (applications != null)
            {
                for (CoreInstance app : applications)
                {
                    if (locations.size() >= MAX_REFERENCES)
                    {
                        break;
                    }
                    SourceInformation appSi = app.getSourceInformation();
                    if (appSi != null)
                    {
                        addLocation(locations, seen, appSi, uriMapper);
                    }
                }
            }
        }

        // For all elements: read 'referenceUsages' (structural type references)
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

                // PureIdeLight: if sourceInformation is empty on the refUsage itself,
                // fall back to the owner's sourceInformation
                SourceInformation refSi = refUsage.getSourceInformation();
                if (refSi != null)
                {
                    addLocation(locations, seen, refSi, uriMapper);
                }
                else
                {
                    CoreInstance owner = refUsage.getValueForMetaPropertyToOne(M3Properties.owner);
                    if (owner != null)
                    {
                        SourceInformation ownerSi = owner.getSourceInformation();
                        if (ownerSi != null)
                        {
                            addLocation(locations, seen, ownerSi, uriMapper);
                        }
                    }
                }
            }
        }

        return locations;
    }

    private static void addLocation(List<Location> locations, Set<String> seen,
                                    SourceInformation si, UriMapper uriMapper)
    {
        // Deduplicate by exact source position
        String key = si.getSourceId() + ":" + si.getStartLine() + ":" + si.getStartColumn();
        if (!seen.add(key))
        {
            return;
        }

        String uri = uriMapper.toUri(si.getSourceId());
        if (uri != null)
        {
            locations.add(SourceInfoUtil.toLocation(si, uri));
        }
    }
}
