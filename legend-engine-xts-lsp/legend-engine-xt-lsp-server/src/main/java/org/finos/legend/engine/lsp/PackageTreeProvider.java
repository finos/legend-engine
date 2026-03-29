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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Provides the Pure logical package tree for the VS Code tree view.
 * Walks the PureRuntime model graph to enumerate packages and elements.
 */
public class PackageTreeProvider
{
    /**
     * Get the children of a package path. Use "" or "::" for root.
     * Returns packages first (sorted), then elements (sorted).
     */
    public static List<PackageChildInfo> getChildren(PureRuntime runtime, UriMapper uriMapper, String packagePath)
    {
        CoreInstance pkg;
        if (packagePath == null || packagePath.isEmpty() || "::".equals(packagePath))
        {
            pkg = runtime.getCoreInstance("::");
        }
        else
        {
            pkg = runtime.getCoreInstance(packagePath);
        }

        if (pkg == null || !(pkg instanceof Package))
        {
            return Collections.emptyList();
        }

        ListIterable<? extends CoreInstance> children = pkg.getValueForMetaPropertyToMany(M3Properties.children);
        if (children == null || children.isEmpty())
        {
            return Collections.emptyList();
        }

        List<PackageChildInfo> packages = new ArrayList<>();
        List<PackageChildInfo> elements = new ArrayList<>();

        for (CoreInstance child : children)
        {
            String name = child.getName();
            if (name == null || name.startsWith("@"))
            {
                continue;
            }

            String qualifiedPath = PackageableElement.getUserPathForPackageableElement(child);
            String classifierName = child.getClassifier().getName();

            if (child instanceof Package)
            {
                // Count children for the label
                ListIterable<? extends CoreInstance> subChildren =
                        child.getValueForMetaPropertyToMany(M3Properties.children);
                int childCount = (subChildren != null) ? subChildren.size() : 0;
                packages.add(new PackageChildInfo(name, qualifiedPath, "Package", true, childCount, null, null));
            }
            else
            {
                SourceInformation si = child.getSourceInformation();
                String uri = null;
                int line = 0;
                if (si != null)
                {
                    uri = uriMapper.toUri(si.getSourceId());
                    line = si.getStartLine();
                }
                elements.add(new PackageChildInfo(name, qualifiedPath, classifierName, false, 0, uri, line > 0 ? line : null));
            }
        }

        // Sort: packages alphabetically, then elements alphabetically
        packages.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        elements.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<PackageChildInfo> result = new ArrayList<>(packages.size() + elements.size());
        result.addAll(packages);
        result.addAll(elements);
        return result;
    }
}
