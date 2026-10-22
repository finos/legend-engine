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
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

public class TestPureModelReverseReferences
{
    private static PureModel compile(String code, Metadata baseline)
    {
        return Compiler.compile(PureGrammarParser.newInstance().parseModel(code), DeploymentMode.TEST, Identity.getAnonymousIdentity().getName(), "", baseline);
    }

    @Test
    public void testSubclassOfBaselineClassLeavesTheBaselineClassUntouched()
    {
        PureModel outer = compile("Class demo::A { name: String[1]; }", null);
        Class<?> outerA = outer.getClass("demo::A");

        PureModel inner = compile("Class demo::B extends demo::A {}", outer.getExecutionSupport().getMetadata());

        Assert.assertTrue(outerA._specializations().isEmpty());
        Assert.assertSame(outerA, inner.getClass("demo::B")._generalizations().getOnly()._general()._rawType());
    }

    @Test
    public void testAssociationToBaselineClassIsKeptInTheLayer()
    {
        PureModel outer = compile("Class demo::A { name: String[1]; }", null);
        Class<?> outerA = outer.getClass("demo::A");

        PureModel inner = compile(
                "Class demo::B {}\n" +
                        "Association demo::AB\n" +
                        "{\n" +
                        "  a: demo::A[1];\n" +
                        "  b: demo::B[*];\n" +
                        "  firstB() { $this.b->at(0) }: demo::B[1];\n" +
                        "}\n",
                outer.getExecutionSupport().getMetadata());

        Assert.assertTrue(outerA._propertiesFromAssociations().isEmpty());
        Assert.assertTrue(outerA._qualifiedPropertiesFromAssociations().isEmpty());
        Assert.assertEquals(Lists.fixedSize.with("b"), inner.getPropertiesFromAssociations(outerA).collect(AbstractProperty::_name).toList());
        Assert.assertEquals(Lists.fixedSize.with("firstB"), inner.getQualifiedPropertiesFromAssociations(outerA).collect(AbstractProperty::_name).toList());
        Assert.assertEquals(Lists.fixedSize.with("a"), inner.getClass("demo::B")._propertiesFromAssociations().collect(AbstractProperty::_name).toList());
    }

    @Test
    public void testInnerFunctionNavigatesAssociationPropertyOfBaselineClass()
    {
        PureModel outer = compile("Class demo::A { name: String[1]; }", null);

        PureModel inner = compile(
                "Class demo::B {}\n" +
                        "Association demo::AB { a: demo::A[1]; b: demo::B[1]; }\n" +
                        "function demo::f(a: demo::A[1]): demo::B[1] { $a.b }\n",
                outer.getExecutionSupport().getMetadata());

        Assert.assertNotNull(inner.getConcreteFunctionDefinition_safe("demo::f_A_1__B_1_"));
        Assert.assertTrue(outer.getClass("demo::A")._propertiesFromAssociations().isEmpty());
    }

    @Test
    public void testInnerMappingOfBaselineClassMapsAssociationPropertyFromTheLayer()
    {
        PureModel outer = compile("Class demo::A { name: String[1]; }", null);

        PureModel inner = compile(
                "Class demo::B { id: Integer[1]; }\n" +
                        "Association demo::AB { a: demo::A[1]; b: demo::B[1]; }\n" +
                        "###Mapping\n" +
                        "Mapping demo::M\n" +
                        "(\n" +
                        "  *demo::B: Pure\n" +
                        "  {\n" +
                        "    ~src demo::B\n" +
                        "    id: $src.id\n" +
                        "  }\n" +
                        "  *demo::A: Pure\n" +
                        "  {\n" +
                        "    ~src demo::A\n" +
                        "    name: $src.name,\n" +
                        "    b[demo_B]: $src.b\n" +
                        "  }\n" +
                        ")\n",
                outer.getExecutionSupport().getMetadata());

        Assert.assertNotNull(inner.getMapping_safe("demo::M"));
        Assert.assertTrue(outer.getClass("demo::A")._propertiesFromAssociations().isEmpty());
    }

    @Test
    public void testPropertyLookupByClassPathSeesAssociationPropertyFromTheLayer()
    {
        PureModel outer = compile("Class demo::A { name: String[1]; }", null);

        PureModel inner = compile(
                "Class demo::B {}\n" +
                        "Association demo::AB { a: demo::A[1]; b: demo::B[1]; }\n",
                outer.getExecutionSupport().getMetadata());

        Assert.assertEquals("b", inner.getProperty("demo::A", "b", SourceInformation.getUnknownSourceInformation(), SourceInformation.getUnknownSourceInformation())._name());
        Assert.assertEquals("b", HelperModelBuilder.getOwnedAppliedProperty(outer.getClass("demo::A"), "b", SourceInformation.getUnknownSourceInformation(), inner.getExecutionSupport())._name());
        Assert.assertEquals("b", HelperModelBuilder.getAllOwnedAppliedProperty(outer.getClass("demo::A"), "b", SourceInformation.getUnknownSourceInformation(), inner.getExecutionSupport())._name());
    }

    @Test
    public void testAssociationToPlatformClassLeavesPlatformClassUntouched()
    {
        String platformClassPath = "meta::pure::metamodel::extension::Profile";
        Class<?> platformClass = (Class<?>) PureModel.METADATA_LAZY.getElementByPath(platformClassPath);

        PureModel model = compile(
                "Class demo::A {}\n" +
                        "Association demo::AP { a: demo::A[*]; profile: " + platformClassPath + "[*]; }\n" +
                        "function demo::f(p: " + platformClassPath + "[1]): demo::A[*] { $p.a }\n",
                null);

        Assert.assertTrue(platformClass._propertiesFromAssociations().isEmpty());
        Assert.assertEquals(Lists.fixedSize.with("a"), model.getPropertiesFromAssociations(platformClass).collect(AbstractProperty::_name).toList());
    }

    @Test
    public void testAssociationOwnedByTheModelIsWrittenToItsClasses()
    {
        PureModel model = compile(
                "Class demo::A {}\n" +
                        "Class demo::B extends demo::A {}\n" +
                        "Association demo::AB { a: demo::A[1]; b: demo::B[*]; }\n",
                null);
        Class<?> a = model.getClass("demo::A");

        Assert.assertEquals(Lists.fixedSize.with("b"), a._propertiesFromAssociations().collect(AbstractProperty::_name).toList());
        Assert.assertSame(model.getClass("demo::B"), a._specializations().getOnly()._specific());
    }

    @Test
    public void testDeltaAssociationIsNotMilestonedOnTheBaselineSide()
    {
        PureModel outer = compile("Class demo::A {}", null);
        Class<?> outerA = outer.getClass("demo::A");

        PureModel inner = compile(
                "Class <<temporal.businesstemporal>> demo::B {}\n" +
                        "Association demo::AB { a: demo::A[*]; b: demo::B[*]; }\n",
                outer.getExecutionSupport().getMetadata());

        // Milestoning only transforms classes compiled in this model, so the baseline end keeps the raw property.
        Assert.assertEquals(Lists.fixedSize.with("b"), inner.getPropertiesFromAssociations(outerA).collect(AbstractProperty::_name).toList());
        Assert.assertTrue(inner.getQualifiedPropertiesFromAssociations(outerA).isEmpty());
    }
}
