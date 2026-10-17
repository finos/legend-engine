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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.LayeredPackage;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Package_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.IntStream;

public class TestLayeredPackage
{
    private final Package baselineRoot = new Package_Impl(M3Paths.Root)._name(M3Paths.Root);
    private final Package baselineDemo = new Package_Impl("demo")._name("demo")._package(this.baselineRoot);
    private final PackageableElement baselineA = new Root_meta_pure_metamodel_type_Class_Impl<>("A")._name("A")._package(this.baselineDemo);

    {
        this.baselineRoot._childrenAdd(this.baselineDemo);
        this.baselineDemo._childrenAdd(this.baselineA);
    }

    private static LayeredPackage child(Package pkg, String name)
    {
        return (LayeredPackage) pkg._children().detect(c -> name.equals(c._name()));
    }

    @Test
    public void testBaselineSubPackagesAreMemoizedOverlays()
    {
        LayeredPackage root = new LayeredPackage(this.baselineRoot);

        LayeredPackage demo = child(root, "demo");

        Assert.assertNotSame(this.baselineDemo, demo);
        Assert.assertSame(this.baselineDemo, demo.getBaseline());
        Assert.assertSame(demo, child(root, "demo"));
        Assert.assertSame(root, demo._package());
        Assert.assertNull(root._package());
        Assert.assertEquals("demo", demo.getName());
        Assert.assertSame(this.baselineA, demo._children().getOnly());
    }

    @Test
    public void testOwnChildrenFollowBaselineChildrenWithoutTouchingTheBaseline()
    {
        LayeredPackage demo = child(new LayeredPackage(this.baselineRoot), "demo");
        PackageableElement b = new Root_meta_pure_metamodel_type_Class_Impl<>("B")._name("B")._package(demo);

        demo._childrenAdd(b);

        Assert.assertEquals(Lists.fixedSize.with("A", "B"), demo._children().collect(PackageableElement::_name).toList());
        Assert.assertSame(this.baselineA, this.baselineDemo._children().getOnly());
    }

    @Test
    public void testOwnChildrenCanBeRemovedButBaselineChildrenCannot()
    {
        LayeredPackage root = new LayeredPackage(this.baselineRoot);
        LayeredPackage demo = child(root, "demo");
        PackageableElement b = new Root_meta_pure_metamodel_type_Class_Impl<>("B")._name("B")._package(demo);
        demo._childrenAdd(b);

        demo._childrenRemove(b);

        Assert.assertSame(this.baselineA, demo._children().getOnly());
        Assert.assertThrows(UnsupportedOperationException.class, () -> demo._childrenRemove(this.baselineA));
        Assert.assertThrows(UnsupportedOperationException.class, () -> root._childrenRemove(demo));
        Assert.assertThrows(UnsupportedOperationException.class, demo::_childrenRemove);
        Assert.assertThrows(UnsupportedOperationException.class, () -> demo._children(Lists.immutable.empty()));
        Assert.assertThrows(UnsupportedOperationException.class, demo::copy);
    }

    @Test
    public void testReflectiveNavigationSeesTheUnion()
    {
        LayeredPackage root = new LayeredPackage(this.baselineRoot);
        LayeredPackage demo = child(root, "demo");
        PackageableElement b = new Root_meta_pure_metamodel_type_Class_Impl<>("B")._name("B")._package(demo);
        demo._childrenAdd(b);

        Assert.assertSame(this.baselineA, _Package.getByUserPath("demo::A", name -> root));
        Assert.assertSame(b, _Package.getByUserPath("demo::B", name -> root));
        Assert.assertNull(_Package.getByUserPath("demo::B", name -> this.baselineRoot));
        Assert.assertEquals(2, demo.getValueForMetaPropertyToMany("children").size());
    }

    @Test
    public void testOverlaysAreSharedAcrossThreads()
    {
        LayeredPackage root = new LayeredPackage(this.baselineRoot);
        Set<LayeredPackage> seen = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

        IntStream.range(0, 64).parallel().forEach(i -> seen.add(child(root, "demo")));

        Assert.assertEquals(1, seen.size());
    }

    @Test
    public void testLayersOverPlatformMetadata()
    {
        Package platformRoot = (Package) PureModel.METADATA_LAZY.getMetadata(M3Paths.Package, M3Paths.Root);
        LayeredPackage root = new LayeredPackage(platformRoot);

        Assert.assertSame(PureModel.METADATA_LAZY.getElementByPath(M3Paths.Class), _Package.getByUserPath(M3Paths.Class, name -> root));
        Package metamodelType = (Package) _Package.getByUserPath("meta::pure::metamodel::type", name -> root);
        Assert.assertTrue(metamodelType instanceof LayeredPackage);
        Assert.assertSame(platformRoot.getClassifier(), metamodelType.getClassifier());
    }
}
