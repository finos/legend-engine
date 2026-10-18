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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestLayeredPureModel
{
    private static PureModel compile(String code, Metadata baseline)
    {
        return compile(code, baseline, "");
    }

    private static PureModel compile(String code, Metadata baseline, String packagePrefix)
    {
        return Compiler.compile(PureGrammarParser.newInstance().parseModel(code), DeploymentMode.TEST, Identity.getAnonymousIdentity().getName(), packagePrefix, baseline);
    }

    private static Metadata metadataOf(PureModel model)
    {
        return model.getExecutionSupport().getMetadata();
    }

    private static PackageableElement pathToElement(PureModel model, String path)
    {
        return Pure.lenientPathToElement(path, "::", null, model.getExecutionSupport());
    }

    private static MutableList<String> childNames(Package pkg)
    {
        return pkg._children().collect(PackageableElement::_name, Lists.mutable.empty());
    }

    @Test
    public void testPureCodeOnTheInnerModelSeesTheDelta()
    {
        PureModel outer = compile("Class demo::A {}", null);

        PureModel inner = compile("Class demo::B extends demo::A {}", metadataOf(outer));

        Assert.assertSame(inner.getPackageableElement_safe("demo::B"), pathToElement(inner, "demo::B"));
        Assert.assertSame(outer.getClass("demo::A"), pathToElement(inner, "demo::A"));
        Assert.assertNull(pathToElement(outer, "demo::B"));
        Assert.assertEquals(Lists.fixedSize.with("A"), childNames((Package) outer.getPackageableElement("demo")));
    }

    @Test
    public void testEachLayerKeepsItsOwnSectionIndex()
    {
        PureModel outer = compile("Class demo::A {}", null);

        PureModel inner = compile("Class demo::B {}", metadataOf(outer));

        PackageableElement innerSectionIndex = pathToElement(inner, "__internal__::SectionIndex");
        Assert.assertNotNull(innerSectionIndex);
        Assert.assertNotSame(pathToElement(outer, "__internal__::SectionIndex"), innerSectionIndex);
        Assert.assertSame(inner.getPackageableElement_safe("__internal__::SectionIndex"), innerSectionIndex);
    }

    @Test
    public void testInnerCompilerResolvesBaselineElementsThatAreNotTypes()
    {
        PureModel outer = compile("Class demo::A {}\n###Mapping\nMapping demo::M\n(\n)\n", null);

        PureModel inner = compile("###Mapping\nMapping demo::N\n(\n  include demo::M\n)\n", metadataOf(outer));

        Assert.assertSame(outer.getMapping("demo::M"), inner.getPackageableElement_safe("demo::M"));
        Assert.assertSame(outer.getMapping("demo::M"), inner.getMapping("demo::N")._includes().getOnly()._included());
    }

    @Test
    public void testPackageLookupByPathShowsBaselineAndDeltaChildren()
    {
        PureModel outer = compile("Class demo::A {}", null);

        PureModel inner = compile("Class demo::B {}", metadataOf(outer));

        Assert.assertEquals(Lists.fixedSize.with("A", "B"), childNames((Package) metadataOf(inner).getMetadata(M3Paths.Package, "demo")));
        Assert.assertEquals(Lists.fixedSize.with("A"), childNames((Package) outer.getPackageableElement("demo")));
    }

    @Test
    public void testPlatformPackageLookupByPathIsTheCompletePlatformPackage()
    {
        String platformPackage = "meta::pure::functions::string";
        PureModel outer = compile("Class demo::A {}", null);
        PureModel inner = compile("Class demo::B {}", metadataOf(outer));

        Assert.assertSame(PureModel.METADATA_LAZY.getMetadata(M3Paths.Package, platformPackage), metadataOf(outer).getMetadata(M3Paths.Package, platformPackage));
        Assert.assertSame(PureModel.METADATA_LAZY.getMetadata(M3Paths.Package, platformPackage), metadataOf(inner).getMetadata(M3Paths.Package, platformPackage));
    }

    @Test
    public void testLayersNest()
    {
        PureModel l0 = compile("Class demo::A {}", null);
        PureModel l1 = compile("Class demo::B extends demo::A {}\n###Mapping\nMapping demo::M\n(\n)\n", metadataOf(l0));

        PureModel l2 = compile("Class demo::C extends demo::B {}\n###Mapping\nMapping demo::N\n(\n  include demo::M\n)\n", metadataOf(l1));

        Lists.fixedSize.with("demo::A", "demo::B", "demo::C", "demo::M", "demo::N").forEach(path -> Assert.assertNotNull(path, pathToElement(l2, path)));
        Assert.assertNull(pathToElement(l1, "demo::C"));
        Assert.assertNull(pathToElement(l1, "demo::N"));
        Class<?> a = l0.getClass("demo::A");
        Class<?> b = l1.getClass("demo::B");
        Assert.assertEquals(Lists.fixedSize.with("B"), l2.getSpecializations(a).collect(g -> g._specific()._name()).toList());
        Assert.assertEquals(Lists.fixedSize.with("C"), l2.getSpecializations(b).collect(g -> g._specific()._name()).toList());
        Assert.assertTrue(l1.getSpecializations(b).isEmpty());
        MutableList<Object> classes = Lists.mutable.withAll(metadataOf(l2).getClassifierInstances(M3Paths.Class));
        Lists.fixedSize.with(a, b, l2.getClass("demo::C")).forEach(c -> Assert.assertTrue(c._name(), classes.anySatisfy(x -> x == c)));
        Assert.assertEquals(Lists.fixedSize.with("A"), childNames((Package) l0.getPackageableElement("demo")));
    }

    @Test
    public void testLayerCallsFunctionsOfTheModelsBelow()
    {
        PureModel l0 = compile("function demo::f(): Integer[1] { 1 }", null);
        PureModel l1 = compile("function demo::g(): Integer[1] { demo::f() }", metadataOf(l0));

        PureModel l2 = compile("function demo::h(): Integer[1] { demo::f() }\nfunction demo::k(): Integer[1] { demo::g() }", metadataOf(l1));

        Assert.assertSame(l0.getConcreteFunctionDefinition("demo::f__Integer_1_", null), calledFunction(l1, "demo::g__Integer_1_", 0));
        Assert.assertSame(l0.getConcreteFunctionDefinition("demo::f__Integer_1_", null), calledFunction(l2, "demo::h__Integer_1_", 0));
        Assert.assertSame(l1.getConcreteFunctionDefinition("demo::g__Integer_1_", null), calledFunction(l2, "demo::k__Integer_1_", 0));
    }

    @Test
    public void testLayerOverloadsMergeWithBaselineOverloads()
    {
        PureModel outer = compile("function demo::f(i: Integer[1]): Integer[1] { $i }", null);

        PureModel inner = compile(
                "function demo::f(s: String[1]): Integer[1] { 0 }\n" +
                        "function demo::g(): Integer[1] { demo::f(1) }\n" +
                        "function demo::h(): Integer[1] { demo::f('a') }\n",
                metadataOf(outer));

        Assert.assertSame(outer.getConcreteFunctionDefinition("demo::f_Integer_1__Integer_1_", null), calledFunction(inner, "demo::g__Integer_1_", 0));
        Assert.assertSame(inner.getConcreteFunctionDefinition("demo::f_String_1__Integer_1_", null), calledFunction(inner, "demo::h__Integer_1_", 0));
        Assert.assertNull(pathToElement(outer, "demo::f_String_1__Integer_1_"));
    }

    private static Object calledFunction(PureModel model, String functionId, int expression)
    {
        return ((SimpleFunctionExpression) model.getConcreteFunctionDefinition(functionId, null)._expressionSequence().toList().get(expression))._func();
    }

    @Test
    public void testPlatformFunctionsWithoutAHandlerStayUnsupported()
    {
        EngineException e = Assert.assertThrows(EngineException.class, () -> compile("function demo::g(): String[0..1] { ['a', 'b']->meta::pure::functions::collection::find({x: String[1] | $x == 'a'}) }", PureModel.METADATA_LAZY));

        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Function does not exist 'meta::pure::functions::collection::find("));
    }

    @Test
    public void testRedefiningABaselineElementIsACompileError()
    {
        PureModel outer = compile("Class demo::A {}", null);

        EngineException e = Assert.assertThrows(EngineException.class, () -> compile("Class demo::A {}", metadataOf(outer)));

        Assert.assertEquals("An element named 'A' already exists in the package 'demo'", e.getMessage());
    }

    @Test
    public void testConcurrentInnerModelsShareOneBaseline()
    {
        PureModel outer = compile("Class demo::A {}", null);
        Metadata baseline = metadataOf(outer);

        MutableList<PureModel> inners = Lists.mutable.withAll(IntStream.range(0, 8).parallel().mapToObj(i -> compile("Class demo::X" + i + " {}", baseline)).collect(Collectors.toList()));

        for (int i = 0; i < inners.size(); i++)
        {
            PureModel inner = inners.get(i);
            Assert.assertNotNull(pathToElement(inner, "demo::X" + i));
            Assert.assertEquals(2, childNames((Package) metadataOf(inner).getMetadata(M3Paths.Package, "demo")).size());
        }
        Assert.assertEquals(Lists.fixedSize.with("A"), childNames((Package) outer.getPackageableElement("demo")));
    }

    @Test
    public void testPackagePrefixWithABaselineIsRejected()
    {
        PureModel outer = compile("Class demo::A {}", null);

        EngineException e = Assert.assertThrows(EngineException.class, () -> compile("Class demo::B {}", metadataOf(outer), "prefix::"));

        Assert.assertEquals("A package prefix is not supported when compiling on top of another model's metadata", e.getMessage());
    }
}
