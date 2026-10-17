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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.generated.Package_Impl;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Baseline sub-packages are only ever handed out as memoized overlays: returning a raw baseline package would let
// getOrCreatePackage add children to the baseline.
public class LayeredPackage extends Package_Impl
{
    private final Package baseline;
    private final LayeredPackage parent;
    private final ConcurrentMap<String, LayeredPackage> overlays = new ConcurrentHashMap<>();
    private final MutableList<PackageableElement> own = Lists.mutable.empty();
    private volatile ImmutableList<PackageableElement> baselineChildren;
    private volatile ImmutableList<PackageableElement> children;

    public LayeredPackage(Package baseline)
    {
        this(baseline, null);
    }

    private LayeredPackage(Package baseline, LayeredPackage parent)
    {
        super(baseline._name(), baseline.getSourceInformation(), baseline.getClassifier());
        this.baseline = baseline;
        this.parent = parent;
        this._name(baseline._name());
        this._package(parent);
    }

    public Package getBaseline()
    {
        return this.baseline;
    }

    @Override
    public CoreInstance getClassifier()
    {
        CoreInstance classifier = super.getClassifier();
        return ((classifier == null) && (this.parent != null)) ? this.parent.getClassifier() : classifier;
    }

    @Override
    public RichIterable<? extends PackageableElement> _children()
    {
        ImmutableList<PackageableElement> result = this.children;
        if (result == null)
        {
            synchronized (this.own)
            {
                result = this.children;
                if (result == null)
                {
                    result = baselineChildren().newWithAll(this.own);
                    this.children = result;
                }
            }
        }
        return result;
    }

    @Override
    public Package _childrenAdd(PackageableElement val)
    {
        synchronized (this.own)
        {
            this.own.add(val);
            this.children = null;
        }
        return this;
    }

    @Override
    public Package _childrenAddAll(RichIterable<? extends PackageableElement> val)
    {
        synchronized (this.own)
        {
            this.own.addAllIterable(val);
            this.children = null;
        }
        return this;
    }

    @Override
    public void _reverse_children(PackageableElement val)
    {
        _childrenAdd(val);
    }

    @Override
    public Package _childrenRemove(PackageableElement val)
    {
        synchronized (this.own)
        {
            int index = this.own.detectIndex(e -> e == val);
            if (index >= 0)
            {
                this.own.remove(index);
                this.children = null;
                return this;
            }
        }
        if (baselineChildren().anySatisfy(e -> e == val))
        {
            throw new UnsupportedOperationException("Cannot remove '" + val._name() + "' from package '" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(this) + "': it belongs to the baseline");
        }
        return this;
    }

    @Override
    public void _sever_reverse_children(PackageableElement val)
    {
        _childrenRemove(val);
    }

    @Override
    public Package _children(RichIterable<? extends PackageableElement> val)
    {
        throw new UnsupportedOperationException("The children of a layered package cannot be replaced");
    }

    @Override
    public Package _childrenRemove()
    {
        throw new UnsupportedOperationException("The children of a layered package cannot be cleared");
    }

    @Override
    public Package copy()
    {
        throw new UnsupportedOperationException("A layered package cannot be copied");
    }

    private ImmutableList<PackageableElement> baselineChildren()
    {
        ImmutableList<PackageableElement> result = this.baselineChildren;
        if (result == null)
        {
            result = Lists.immutable.withAll(LazyIterate.collect(this.baseline._children(), this::overlay));
            this.baselineChildren = result;
        }
        return result;
    }

    private PackageableElement overlay(PackageableElement child)
    {
        return (child instanceof Package) ? this.overlays.computeIfAbsent(child._name(), name -> new LayeredPackage((Package) child, this)) : child;
    }
}
