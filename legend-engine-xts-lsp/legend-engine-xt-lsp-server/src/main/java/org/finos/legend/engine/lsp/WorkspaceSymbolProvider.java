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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides workspace symbol search with an upfront index built after PureRuntime initialization.
 * The index stores lightweight entries (path, kind, source info) so searches are fast
 * substring matches against pre-computed strings — no tree walking at query time.
 */
public class WorkspaceSymbolProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceSymbolProvider.class);

    private final List<IndexEntry> index = new CopyOnWriteArrayList<>();

    /**
     * Build the index by walking the entire Pure package tree.
     * Call once after PureRuntime initializes, and again after reindex.
     */
    public void buildIndex(PureRuntime runtime)
    {
        long start = System.currentTimeMillis();
        List<IndexEntry> entries = new ArrayList<>();
        CoreInstance root = runtime.getCoreInstance("::");
        if (root instanceof Package)
        {
            walkPackage((Package) root, entries);
        }
        this.index.clear();
        this.index.addAll(entries);
        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("Symbol index built: {} entries in {}ms", entries.size(), elapsed);
    }

    /**
     * Search the index for symbols matching the query (case-insensitive substring).
     */
    public List<SymbolInformation> search(UriMapper uriMapper, String query, int maxResults)
    {
        String lowerQuery = (query != null) ? query.toLowerCase() : "";
        List<SymbolInformation> results = new ArrayList<>();

        for (IndexEntry entry : this.index)
        {
            if (results.size() >= maxResults)
            {
                break;
            }
            if (lowerQuery.isEmpty() || entry.lowerSearchName.contains(lowerQuery))
            {
                SymbolInformation symbol = entry.toSymbolInformation(uriMapper);
                if (symbol != null)
                {
                    results.add(symbol);
                }
            }
        }
        return results;
    }

    /**
     * Get the number of indexed entries.
     */
    public int size()
    {
        return this.index.size();
    }

    public void clear()
    {
        this.index.clear();
    }

    private static void walkPackage(Package pkg, List<IndexEntry> entries)
    {
        ListIterable<? extends CoreInstance> children = pkg.getValueForMetaPropertyToMany(M3Properties.children);
        for (CoreInstance child : children)
        {
            if (child instanceof Package)
            {
                walkPackage((Package) child, entries);
            }
            else
            {
                SourceInformation si = child.getSourceInformation();
                if (si != null)
                {
                    String qualifiedPath = PackageableElement.getUserPathForPackageableElement(child);
                    String classifierName = child.getClassifier().getName();
                    // For functions, extract the simple name from M3Properties.functionName
                    // so developers can search by "compileLegendGrammar" instead of the
                    // mangled name "compileLegendGrammar_String_1__PackageableElement_MANY_"
                    String simpleName = null;
                    if ("ConcreteFunctionDefinition".equals(classifierName)
                            || "NativeFunction".equals(classifierName))
                    {
                        CoreInstance fnName = child.getValueForMetaPropertyToOne(M3Properties.functionName);
                        if (fnName != null)
                        {
                            simpleName = fnName.getName();
                        }
                    }
                    entries.add(new IndexEntry(
                            qualifiedPath,
                            classifierName,
                            simpleName,
                            si.getSourceId(),
                            si.getStartLine(),
                            si.getStartColumn(),
                            si.getEndLine(),
                            si.getEndColumn()
                    ));
                }
            }
        }
    }

    static SymbolKind toSymbolKind(String classifierName)
    {
        if (classifierName == null)
        {
            return SymbolKind.Object;
        }
        switch (classifierName)
        {
            case "Class":
                return SymbolKind.Class;
            case "Enumeration":
                return SymbolKind.Enum;
            case "ConcreteFunctionDefinition":
            case "NativeFunction":
                return SymbolKind.Function;
            case "Profile":
                return SymbolKind.Interface;
            case "Association":
                return SymbolKind.Struct;
            case "Measure":
            case "Unit":
                return SymbolKind.Number;
            default:
                return SymbolKind.Object;
        }
    }

    /**
     * Lightweight index entry — no reference to CoreInstance (avoids holding the graph in memory twice).
     */
    static class IndexEntry
    {
        final String qualifiedPath;
        final String lowerPath;
        final String classifierName;
        final String simpleFunctionName; // null for non-functions
        final String lowerSearchName;    // includes simple name for function search
        final String sourceId;
        final int startLine;
        final int startColumn;
        final int endLine;
        final int endColumn;

        IndexEntry(String qualifiedPath, String classifierName, String simpleFunctionName,
                   String sourceId, int startLine, int startColumn, int endLine, int endColumn)
        {
            this.qualifiedPath = qualifiedPath;
            this.lowerPath = qualifiedPath.toLowerCase();
            this.classifierName = classifierName;
            this.simpleFunctionName = simpleFunctionName;
            // For functions, search matches against the simple name (e.g. "compileLegendGrammar")
            // as well as the qualified path. This lets developers search naturally.
            if (simpleFunctionName != null)
            {
                this.lowerSearchName = (qualifiedPath + " " + simpleFunctionName).toLowerCase();
            }
            else
            {
                this.lowerSearchName = this.lowerPath;
            }
            this.sourceId = sourceId;
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
        }

        /**
         * Get the display name: for functions, use the simple name;
         * for other elements, use the last segment of the qualified path.
         */
        String getDisplayName()
        {
            if (this.simpleFunctionName != null)
            {
                return this.simpleFunctionName;
            }
            int lastSep = this.qualifiedPath.lastIndexOf("::");
            if (lastSep > 0)
            {
                return this.qualifiedPath.substring(lastSep + 2);
            }
            return this.qualifiedPath;
        }

        SymbolInformation toSymbolInformation(UriMapper uriMapper)
        {
            String uri = uriMapper.toUri(this.sourceId);
            if (uri == null)
            {
                return null;
            }

            SymbolKind kind = toSymbolKind(this.classifierName);
            String containerName = "";
            int lastSep = this.qualifiedPath.lastIndexOf("::");
            if (lastSep > 0)
            {
                containerName = this.qualifiedPath.substring(0, lastSep);
            }

            Location location = new Location(
                    uri,
                    new Range(
                            new Position(Math.max(0, this.startLine - 1), Math.max(0, this.startColumn - 1)),
                            new Position(Math.max(0, this.endLine - 1), Math.max(0, this.endColumn))
                    )
            );

            SymbolInformation info = new SymbolInformation();
            info.setName(getDisplayName());
            info.setKind(kind);
            info.setContainerName(containerName);
            info.setLocation(location);
            return info;
        }
    }
}
