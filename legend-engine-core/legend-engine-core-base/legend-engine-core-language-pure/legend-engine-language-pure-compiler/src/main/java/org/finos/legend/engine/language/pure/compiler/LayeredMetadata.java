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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.LayeredPackage;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
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
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        RichIterable<CoreInstance> own = getOwnElementsOfClassifier(classifier);
        if (own.isEmpty())
        {
            return this.baseline.getMetadata(classifier);
        }
        MutableMap<String, CoreInstance> result = Maps.mutable.empty();
        try
        {
            this.baseline.getMetadata(classifier).forEachKeyValue(result::put);
        }
        catch (RuntimeException ignore)
        {
            // a baseline that cannot list its instances, such as an interpreted runtime, contributes none
        }
        own.forEach(e -> result.put(PackageableElement.getUserPathForPackageableElement(e), e));
        return result;
    }

    @Override
    public RichIterable<CoreInstance> getClassifierInstances(String classifier)
    {
        RichIterable<CoreInstance> own = getOwnElementsOfClassifier(classifier);
        if (own.isEmpty())
        {
            return this.baseline.getClassifierInstances(classifier);
        }
        RichIterable<CoreInstance> base;
        try
        {
            base = this.baseline.getClassifierInstances(classifier);
        }
        catch (RuntimeException ignore)
        {
            base = Lists.immutable.empty();
        }
        return LazyIterate.concatenate(base, own);
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        try
        {
            return this.baseline.getEnum(enumerationName, enumName);
        }
        catch (RuntimeException e)
        {
            CoreInstance enumeration = this.pureModel.getTypeFromIndex(stripRoot(enumerationName));
            CoreInstance value = (enumeration instanceof Enumeration) ? org.finos.legend.pure.m3.navigation.enumeration.Enumeration.findEnum(enumeration, enumName) : null;
            if (value == null)
            {
                throw e;
            }
            return value;
        }
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

    // A baseline that is a model compiled without a baseline does not list its own elements, so the first layer lists them.
    private RichIterable<CoreInstance> getOwnElementsOfClassifier(String classifier)
    {
        MutableList<CoreInstance> result = Lists.mutable.empty();
        PureModel unlayeredBaseline = ((this.baseline instanceof MetadataWrapper) && !(this.baseline instanceof LayeredMetadata)) ? ((MetadataWrapper) this.baseline).getPureModel() : null;
        Lists.mutable.with(unlayeredBaseline, this.pureModel).select(m -> m != null).forEach(model -> model.getOwnedPackageableElements().forEach(e ->
        {
            if (classifier.equals(classifierPath(e)))
            {
                result.add(e);
            }
        }));
        return result;
    }

    private static String classifierPath(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element)
    {
        GenericType classifierGenericType = element._classifierGenericType();
        CoreInstance classifier = ((classifierGenericType != null) && (classifierGenericType._rawType() != null)) ? classifierGenericType._rawType() : element.getClassifier();
        return (classifier == null) ? null : PackageableElement.getUserPathForPackageableElement(classifier);
    }

    private static String stripRoot(String id)
    {
        return id.startsWith("Root::") ? id.substring(6) : id;
    }
}
