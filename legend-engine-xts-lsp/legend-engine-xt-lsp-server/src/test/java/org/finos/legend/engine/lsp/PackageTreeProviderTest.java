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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PackageTreeProviderTest
{
    private static LegendPureSession session;
    private static UriMapper uriMapper;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        uriMapper = new UriMapper();

        uriMapper.register("file:///test/tree_test.pure", "tree_test.pure");

        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "tree_test.pure",
                "Class test::tree::Animal { name: String[1]; }\n" +
                "Enum test::tree::Color { Red, Green, Blue }\n" +
                "function test::tree::greet(x: String[1]): String[1] { 'hi ' + $x }\n"
        );
        Assert.assertTrue("Should compile: " +
                (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void getChildren_root_returnsTopLevelPackages()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "::");

        Assert.assertFalse("Root should have children", children.isEmpty());

        // Should contain packages like "meta", "system", "test", etc.
        List<String> packageNames = children.stream()
                .filter(PackageChildInfo::getIsPackage)
                .map(PackageChildInfo::getName)
                .collect(Collectors.toList());

        Assert.assertTrue("Root should contain 'meta' package, found: " + packageNames,
                packageNames.contains("meta"));
        Assert.assertTrue("Root should contain 'test' package, found: " + packageNames,
                packageNames.contains("test"));
    }

    @Test
    public void getChildren_testPackage_findsSubPackage()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "test");

        List<String> names = children.stream()
                .map(PackageChildInfo::getName)
                .collect(Collectors.toList());

        Assert.assertTrue("test should contain 'tree' subpackage, found: " + names,
                names.contains("tree"));
    }

    @Test
    public void getChildren_testTreePackage_findsElements()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "test::tree");

        Assert.assertFalse("test::tree should have children", children.isEmpty());

        List<String> names = children.stream()
                .map(PackageChildInfo::getName)
                .collect(Collectors.toList());

        Assert.assertTrue("Should contain Animal class, found: " + names,
                names.contains("Animal"));
        Assert.assertTrue("Should contain Color enum, found: " + names,
                names.contains("Color"));
    }

    @Test
    public void getChildren_elements_haveCorrectKinds()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "test::tree");

        PackageChildInfo animal = children.stream()
                .filter(c -> "Animal".equals(c.getName()))
                .findFirst().orElse(null);
        Assert.assertNotNull("Should find Animal", animal);
        Assert.assertEquals("Class", animal.getKind());
        Assert.assertFalse(animal.getIsPackage());
        Assert.assertEquals("test::tree::Animal", animal.getQualifiedPath());

        PackageChildInfo color = children.stream()
                .filter(c -> "Color".equals(c.getName()))
                .findFirst().orElse(null);
        Assert.assertNotNull("Should find Color", color);
        Assert.assertEquals("Enumeration", color.getKind());
    }

    @Test
    public void getChildren_packages_haveChildCounts()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "test");

        PackageChildInfo tree = children.stream()
                .filter(c -> "tree".equals(c.getName()))
                .findFirst().orElse(null);
        Assert.assertNotNull("Should find tree package", tree);
        Assert.assertTrue(tree.getIsPackage());
        Assert.assertTrue("Package should report child count > 0, got: " + tree.getChildCount(),
                tree.getChildCount() > 0);
    }

    @Test
    public void getChildren_packagesSortedBeforeElements()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "::");

        // All packages should come before all elements
        boolean seenElement = false;
        for (PackageChildInfo child : children)
        {
            if (!child.getIsPackage())
            {
                seenElement = true;
            }
            if (seenElement && child.getIsPackage())
            {
                Assert.fail("Package '" + child.getName() + "' appears after an element — packages should come first");
            }
        }
    }

    @Test
    public void getChildren_unknownPackage_returnsEmpty()
    {
        List<PackageChildInfo> children = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "nonexistent::package");

        Assert.assertTrue("Unknown package should return empty", children.isEmpty());
    }

    @Test
    public void getChildren_emptyString_treatedAsRoot()
    {
        List<PackageChildInfo> fromEmpty = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "");
        List<PackageChildInfo> fromRoot = PackageTreeProvider.getChildren(
                session.getPureRuntime(), uriMapper, "::");

        Assert.assertEquals("Empty string and :: should return same children",
                fromEmpty.size(), fromRoot.size());
    }
}
