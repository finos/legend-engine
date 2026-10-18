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

package org.finos.legend.engine.language.pure.compiler;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.LayeredPackage;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class LayeredMetadata extends MetadataWrapper
{
    private final Package root;
    private final Metadata baseline;
    private final PureModel pureModel;

    public LayeredMetadata(LayeredPackage root, Metadata baseline, PureModel pureModel)
    {
        super(root, baseline, pureModel);
        this.root = root;
        this.baseline = baseline;
        this.pureModel = pureModel;
    }

    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        if (M3Paths.Package.equals(classifier))
        {
            return getPackage(id);
        }
        CoreInstance own = getOwnElement(stripRoot(id));
        if (own != null)
        {
            return own;
        }
        try
        {
            return this.baseline.getMetadata(classifier, id);
        }
        catch (RuntimeException e)
        {
            throw new PureExecutionException(e);
        }
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        CoreInstance enumeration = this.pureModel.getTypeFromIndex(stripRoot(enumerationName));
        CoreInstance value = (enumeration instanceof Enumeration) ? org.finos.legend.pure.m3.navigation.enumeration.Enumeration.findEnum(enumeration, enumName) : null;
        return (value != null) ? value : this.baseline.getEnum(enumerationName, enumName);
    }

    // A package this layer added children to is answered from the layer. Any other package is answered by the baseline first:
    // a model compiled without a baseline only keeps a partial copy of platform packages under its root.
    private CoreInstance getPackage(String id)
    {
        if (M3Paths.Root.equals(id))
        {
            return this.root;
        }
        Package own = findPackage(stripRoot(id));
        if ((own instanceof LayeredPackage) && ((LayeredPackage) own).hasOwnChildren())
        {
            return own;
        }
        try
        {
            CoreInstance base = this.baseline.getMetadata(M3Paths.Package, id);
            if (base != null)
            {
                return base;
            }
        }
        catch (RuntimeException e)
        {
            if (own == null)
            {
                throw new PureExecutionException(e);
            }
        }
        return own;
    }

    private Package findPackage(String path)
    {
        Package current = this.root;
        for (String name : PackageableElement.splitUserPath(path))
        {
            CoreInstance child = current._children().detect(c -> (c instanceof Package) && name.equals(c._name()));
            if (child == null)
            {
                return null;
            }
            current = (Package) child;
        }
        return current;
    }

    private CoreInstance getOwnElement(String path)
    {
        CoreInstance type = this.pureModel.getTypeFromIndex(path);
        return (type != null) ? type : this.pureModel.getPackageableElementFromIndex(path);
    }

    private static String stripRoot(String id)
    {
        return id.startsWith("Root::") ? id.substring(6) : id;
    }
}
