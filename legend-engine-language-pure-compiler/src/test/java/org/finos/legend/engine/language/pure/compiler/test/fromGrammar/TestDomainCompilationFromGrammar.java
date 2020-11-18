// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

import org.junit.Assert;
import org.junit.Test;

public class TestDomainCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [3:1-34]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testDuplicatedDomainElements()
    {
        String initialGraph = "Class anything::class\n" +
                "{\n" +
                "  ok : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse\n" +
                "(\n" +
                ")\n";
        // Class
        test(initialGraph +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
        // Profile
        test(initialGraph +
                "###Pure\n" +
                "Profile anything::somethingelse\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
        // Enumeration
        test(initialGraph +
                "###Pure\n" +
                "Enum anything::somethingelse\n" +
                "{\n" +
                " A\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
        // Association
        test(initialGraph +
                "###Pure\n" +
                "Class test::B\n" +
                "{\n" +
                "  good : Integer[0..1];\n" +
                "}\n" +
                "Association anything::somethingelse\n" +
                "{\n" +
                "  b1 : anything::class[1];\n" +
                "  b2 : test::B[1];\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
        // Function
        test(initialGraph +
                "\n###Pure\n" +
                "function anything::somethingelse(a:String[1]):String[1]" +
                "{" +
                "   'hiiii'" +
                "}\n", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
        // Measure
        test(initialGraph +
                "\n###Pure\n" +
                "Measure anything::somethingelse\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}", "COMPILATION error at [6:1-8:1]: Duplicated element 'anything::somethingelse'"
        );
    }

    @Test
    public void testCycleClassSuperType()
    {
        test("Class test::A extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::A"
        );
        test("Class test::A extends test::B\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::B -> test::A"
        );
        test("Class test::A extends test::B\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::C\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::C extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::B -> test::C -> test::A"
        );
    }

    @Test
    public void testSuperTypeDuplication()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::A, test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [5:1-8:1]: Duplicated super type 'test::A' in class 'test::B'"
        );
    }

    @Test
    public void testSimpleClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "   test : Integer[0..1];\n" +
                "}\n" +
                "Class test::B \n" +
                "{\n" +
                " good : Integer[0..1];" +
                "}\n"
        );
    }

    @Test
    public void testPackageWithUnderscore()
    {
        test("function my::functionParent():String[1]\n" +
                "{\n" +
                "    my::package_with_underscore::functionName();\n" +
                "}\n" +
                "\n" +
                "function my::package_with_underscore::functionName():String[1]\n" +
                "{\n" +
                " 'result';\n" +
                "}");
    }

    @Test
    public void testElementDefinitionWithoutPackage()
    {
        test("Class A\n" +
                "{\n" +
                "}\n", "COMPILATION error at [1:1-3:1]: Element package is required"
        );
    }

    @Test
    public void testMeasureDefinition()
    {
        test("Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}"
        );
    }

    @Test
    public void testNonConvertibleMeasureDefinition()
    {
        test("Measure test::NewNonConvertibleMeasure\n" +
                "{\n" +
                "   UnitOne;\n" +
                "   UnitTwo;\n" +
                "   UnitThree;\n" +
                "}"
        );
    }

    @Test
    public void testClassWithUnitTypeProperty()
    {
        String newMeasure = "Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitOne : test::NewMeasure~UnitOne[0..1];\n" +
                "   unitTwo : test::NewMeasure~UnitTwo[0..1];\n" +
                "}\n"
        );
    }

    @Test
    public void testClassWithNonConvertibleUnitTypeProperty()
    {
        String newMeasure = "Measure test::NewNonConvertibleMeasure\n" +
                "{\n" +
                "   UnitOne;\n" +
                "   UnitTwo;\n" +
                "   UnitThree;\n" +
                "}";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitOne : test::NewNonConvertibleMeasure~UnitOne[0..1];\n" +
                "   unitTwo : test::NewNonConvertibleMeasure~UnitTwo[0..1];\n" +
                "}\n"
        );
    }

    @Test
    public void testClassWithMissingUnitType()
    {
        String newMeasure = "Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}";
        String expectedErrorMessage = "COMPILATION error at [8:15-39]: Can't find type 'test::NewMeasure~UnitFour'";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitFour : test::NewMeasure~UnitFour[0..1];\n" +
                "}\n", expectedErrorMessage
        );
    }

    @Test
    public void testMissingProfile()
    {
        test("Class <<NoProfile.NoKey>> test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:9-17]: Can't find profile 'NoProfile'");
    }

    @Test
    public void testMissingTaggedValue()
    {
        test("Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "   stereotypes: [tests];\n" +
                "   tags: [doc, todo];\n" +
                "}\n" +
                "Class test::A\n" +
                "{\n" +
                "<<meta::pure::profiles::doc.imMissing>> ok: Integer[0..1];\n" +
                "}\n", "COMPILATION error at [8:3-37]: Can't find stereotype 'imMissing' in profile 'meta::pure::profiles::doc'");
    }

    @Test
    public void testMissingStereoType()
    {
        test("Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "   stereotypes: [tests];\n" +
                "   tags: [doc, todo];\n" +
                "}\n" +
                "Class {meta::pure::profiles::doc.imMissing = 'imMissing'} test::A\n" +
                "{\n" +
                "ok: Integer[0..1];\n" +
                "}\n", "COMPILATION error at [6:34-42]: Can't find tag 'imMissing' in profile 'meta::pure::profiles::doc'");
    }

    @Test
    public void testMissingSuperType()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::B extends NotHere\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Can't find type 'NotHere'"
        );
    }

    @Test
    public void testFaultyClassSuperType()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   A, B , C\n" +
                "}\n" +
                "\n" +
                "Class test::B extends test::A\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Invalid supertype: 'B' cannot extend 'test::A' as it is not a class."
        );
    }

    @Test
    public void testMissingPropertyType()
    {
        test("Class test::A\n" +
                "{\n" +
                "   good: String[0..1];\n" +
                "   notGood : MissingProp[1];\n" +
                "}\n", "COMPILATION error at [4:14-24]: Can't find type 'MissingProp'"
        );
    }

    @Test
    public void testFaultyAssociation()
    {
        test("Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : String[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [1:1-4:1]: Expected 2 properties for an association 'test::FaultyAssociation'"
        );
        test("Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : String[1];\n" +
                "   b : String[1];\n" +
                "   c : String[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [1:1-6:1]: Expected 2 properties for an association 'test::FaultyAssociation'"
        );
    }


    @Test
    public void testPrimitive()
    {
        test("Class test::A\n" +
                "[\n" +
                "  constraint1: $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false),\n" +
                "  constraint3: $this.anyValue->instanceOf(String) || $this.anyValue->instanceOf(test::AEnum)\n" +
                "]\n" +
                "{\n" +
                "  name: String[45..*];\n" +
                "  name1: Boolean[45..*];\n" +
                "  name13: Binary[45..*];\n" +
                "  ok: Integer[1..2];\n" +
                "  ok1: Number[1..2];\n" +
                "  ok2: Decimal[1..2];\n" +
                "  ok3: Float[1..2];\n" +
                "  ok4: Date[1..2];\n" +
                "  ok5: StrictDate[1..2];\n" +
                "  ok6: DateTime[1..2];\n" +
                "  ok7: LatestDate[1..2];\n" +
                "  anyValue: meta::pure::metamodel::type::Any[1];\n" +
                "}\n" +
                "\n" +
                "Enum test::AEnum\n" +
                "{\n" +
                "  B\n" +
                "}\n");
    }

    @Test
    public void testComplexConstraint()
    {
        test("Class test::A\n" +
                "[\n" +
                "  constraint1\n" +
                "  (" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: if($this.ok == 'ok', |true, |false)\n" +
                "    ~enforcementLevel: Warn\n" +
                "    ~message: $this.ok + ' is not ok'\n" +
                "  )\n" +
                "]\n" +
                "{\n" +
                "  ok: Integer[1..2];\n" +
                "}\n");
    }

    @Test
    public void testFunctionOrLambdaWithUnknownToken()
    {
        test("Class test::A\n" +
                        "{\n" +
                        "   name : String[*];\n" +
                        "   xza(z:String[1]){ok}:String[1];\n" +
                        "}\n",
                "COMPILATION error at [4:21-22]: Can't find type 'ok'");
        test("Class test::A\n" +
                "[" +
                "   ok" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [2:5-6]: Can't find type 'ok'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}" +
                "Class test::A\n" +
                "[" +
                "   test::a" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [5:5-11]: Can't find type 'test::a'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}" +
                "Class test::A\n" +
                "[" +
                "   test::b" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [5:5-11]: Constraint must be of type 'Boolean'");
    }

    @Test
    public void testFunctionOrLambdaWithUnknownEnumValue()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "   xza(z:String[1]){ok.a}:String[1];\n" +
                "}\n", "COMPILATION error at [4:21-22]: Can't find enumeration 'ok'");
        test("Enum test::b\n" +
                "{\n" +
                "   names" +
                "}" +
                "Class test::A\n" +
                "[" +
                "   test::b.c" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [4:13]: Can't find enum value 'c' in enumeration 'test::b'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}" +
                "Class test::A\n" +
                "[" +
                "   test::b.c" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [5:5-11]: Can't find enumeration 'test::b'");
    }

    @Test
    public void testMissingAssociationProperty()
    {
        test("Class test::A {\n" +
                "}\n" +
                "Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : test::A[1];\n" +
                "   b : someClass[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [6:4-20]: Can't find class 'someClass'");
    }

    @Test
    public void testQualifiedProperty()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "   xza(s:z::k::B[1]){$s + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [4:8-19]: Can't find type 'z::k::B'");
    }

    @Test
    public void testMissingAnyAppliedProperty()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   xza(s: test::A[1]){$s.x + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [7:26]: Can't find property 'x' in class 'test::A'");
    }

    @Test
    public void testMissingEnumValueInConstraint()
    {
        test("Enum test::PriceExpressionEnum {\n" +
                "   AbsoluteTerms,\n" +
                "   ParcentageOfNotional\n" +
                "}\n" +
                "\n" +
                "Class ui::String {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class ui::b extends ui::String\n" +
                "[\n" +
                "   if($this.priceExpression == test::PriceExpressionEnum.PercetateOfNotionall, |$this.currency->isEmpty(), |true)\n" +
                "]\n" +
                "{\n" +
                "  priceExpression: test::PriceExpressionEnum[1];\n" +
                "  currency: String[*];\n" +
                "}", "COMPILATION error at [12:58-77]: Can't find enum value 'PercetateOfNotionall' in enumeration 'test::PriceExpressionEnum'");
    }

    @Test
    public void testMissingEnumValueInDerivedProperty()
    {
        test("Enum test::PriceExpressionEnum {\n" +
                "   AbsoluteTerms,\n" +
                "   ParcentageOfNotional\n" +
                "}\n" +
                "\n" +
                "Class ui::String {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class ui::b extends ui::String\n" +
                "{\n" +
                "  priceExpression: test::PriceExpressionEnum[1];\n" +
                "  currency: String[*];\n" +
                "  clingy() {\n" +
                "    test::PriceExpressionEnum.PercetateOfNotionall;\n" +
                "    'sad';\n" +
                "  }: String[*];\n" +
                "}", "COMPILATION error at [15:31-50]: Can't find enum value 'PercetateOfNotionall' in enumeration 'test::PriceExpressionEnum'");
    }

    @Test
    public void testMissingLoopedAnyAppliedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   pet: test::Dog[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   xza(s: test::B[1]){$s.pet.dogMissing + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [11:30-39]: Can't find property 'dogMissing' in class 'test::Dog'");
    }

    @Test
    public void testGoodLoopedQualified()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   pet: test::Dog[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   xza(s: test::B[1]){$s.pet.name->at(0) + 'ok'}:String[1];\n" +
                "}\n");
    }

    @Test
    public void testGoodQualifiedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "    funcDog(){'my Name is Bobby';}:String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "    funcB(s: test::Dog[1]){$s.funcDog()}:String[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   test(s: test::B[1], d: test::Dog[1]){$s.funcB($d) + '!'}:String[1];\n" +
                "}\n");
    }

    @Test
    public void testFailedToFindQuailifiedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "    funcDog(){'my Name is Bobby';}:String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "    funcB(s: test::Dog[1]){$s.WhoopsfuncDog()}:String[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   test(s: test::B[1], d: test::Dog[1]){$s.funcB($d) + '!'}:String[1];\n" +
                "}\n", "COMPILATION error at [7:31-43]: Can't find property 'WhoopsfuncDog' in class 'test::Dog'");
    }

    @Test
    public void testMissingVariableName()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   A, a \n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   xza(s: String[1]){$src}:String[1];\n" +
                "}\n", "COMPILATION error at [7:22-25]: Can't find variable class for variable 'src' in the graph");
    }

    @Test
    public void testMapLambdaInferenceWithPrimitive()
    {
        test("Class test::A" +
                "{" +
                "   p(){[1,2]->map(a|$a+1)}:Integer[*];" +
                "}"
        );
        test("Class test::A" +
                "{" +
                "   p(){[1,2]->map(a|$a+'1')}:String[1];" +
                "}", "COMPILATION error at [1:37-40]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testMapLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->map(a|$a.name)}:String[*];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->map(a|$a.nam)}:String[*];" +
                "}", "COMPILATION error at [1:81-83]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testSortByLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->sortBy(a|$a.name)}:test::A[*];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->sortBy(a|$a.nam)}:test::A[*];" +
                "}", "COMPILATION error at [1:84-86]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testFilterLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(a|$a.name == 'yeah')}:test::A[*];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(a|$a.nam == 'ohoh')}:test::A[*];" +
                "}", "COMPILATION error at [1:84-86]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testGroupByLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.name, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupBy(a|$a.nae, agg(x|$x.name, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:85-87]: Can't find property 'nae' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.nae, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:100-102]: Can't find property 'nae' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.name, z|$z->map(k|$k+1)), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:120-121]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testGroupByWithWindowLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupByWithWindowSubset(a|$a.name, agg(x|$x.name, z|$z->count()), ['a', 'b'], ['a'], ['b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->groupByWithWindowSubset(a|$a.name, agg(x|$x.namex, z|$z->count()), ['a', 'b'], ['a'], ['b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:116-120]: Can't find property 'namex' in class 'test::A'");
    }

    @Test
    public void testProjectInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([a|$a.name], ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "   g(){test::A.all()->project(a|$a.name, ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([a|$a.nawme], ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:86-90]: Can't find property 'nawme' in class 'test::A'");
        test("Class  test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project(a|$a.nawme, ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:86-90]: Can't find property 'nawme' in class 'test::A'");
    }

    @Test
    public void testProjectColInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.name, 'a')])}:meta::pure::tds::TabularDataSet[1];" +
                "   y(){test::A.all()->project(col(a|$a.name, 'a'))}:meta::pure::tds::TabularDataSet[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.naxme, 'a')])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:90-94]: Can't find property 'naxme' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project(col(a|$a.naxme, 'a'))}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:89-93]: Can't find property 'naxme' in class 'test::A'");
    }

    @Test
    public void testProjectWithSubsetColInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->projectWithColumnSubset([col(a|$a.name, 'a')], ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "   y(){test::A.all()->projectWithColumnSubset(col(a|$a.name, 'a'), ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "   h(){test::A.all()->projectWithColumnSubset([a|$a.name], 'a', ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "   j(){test::A.all()->projectWithColumnSubset(a|$a.name, 'a' , ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}");

        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->projectWithColumnSubset([col(a|$a.xname, 'a')], ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:106-110]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   y(){test::A.all()->projectWithColumnSubset(col(a|$a.xname, 'a'), ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:105-109]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   h(){test::A.all()->projectWithColumnSubset([a|$a.xname], 'a', ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:102-106]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   j(){test::A.all()->projectWithColumnSubset(a|$a.xname, 'a' , ['a','b'])}:meta::pure::tds::TabularDataSet[1];" +
                "}", "COMPILATION error at [1:101-105]: Can't find property 'xname' in class 'test::A'");
    }

    @Test
    public void testExistsLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->exists(a|$a.name == 'yeah')}:Boolean[1];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->exists(a|$a.nam == 'ohoh')}:test::A[*];" +
                "}", "COMPILATION error at [1:84-86]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testTDSContainsInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(a|$a->tdsContains([p|$p.name], test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];" +
                "   k(){test::A.all()->filter(a|$a->tdsContains(p|$p.name, test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(a|$a->tdsContains([p|$p.xname], test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];" +
                "}", "COMPILATION error at [1:103-107]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   k(){test::A.all()->filter(a|$a->tdsContains(p|$p.xname, test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];" +
                "}", "COMPILATION error at [1:102-106]: Can't find property 'xname' in class 'test::A'");
    }

    @Test
    public void testTDSContainsWithLambdaInferenceWithClass()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "   z(){test::A.all()->filter(v|$v->tdsContains(p|$p.name, ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.ncame, 'ww')), {a,b | $a.isNotXNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}", "COMPILATION error at [1:149-153]: Can't find property 'ncame' in class 'test::A'");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotXNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}", "COMPILATION error at [1:173-182]: Can't find property 'isNotXNull' in class 'meta::pure::tds::TDSRow'");
    }

    @Test
    public void testGroupByTDS()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->groupBy('prodName', agg('sum', x|$x.getFloat('quantity')*$x.getInteger('quantity'), y| $y->sum()))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->groupBy('prodName', agg('sum', x|$x.getwFloat('quantity')*$x.getInteger('quantity'), y| $y->sum()))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [1:149-157]: Can't find property 'getwFloat' in class 'meta::pure::tds::TDSRow'");
    }

    @Test
    public void testMultiplicityErrorInCollection()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names->at(0) + 'ok'} : String[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Fourth Pass, Qualified Property prop, Applying plus], multiplicity:[*]");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[0..1];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Fourth Pass, Qualified Property prop, Applying plus], multiplicity:[0..1]");
    }

    @Test
    public void testConstraint()
    {
        test("Class test::A\n" +
                "[" +
                "   $this.names->isNotEmpty()" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}");
        test("Class test::A\n" +
                "[" +
                "   $this.names->at(0)" +
                "]" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [2:18-19]: Constraint must be of type 'Boolean'");
    }

    @Test
    public void testReturnTypeErrorInQualifier()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {'1'} : String[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {1} : String[1];\n" +
                "}", "COMPILATION error at [4:12]: Error in derived property 'A.prop' - Type error: 'Integer' is not a subtype of 'String'");
    }

    @Test
    public void testReturnMultiplicityErrorInQualifier()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {['a','b']} : String[*];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Error in derived property 'A.prop' - Multiplicity error: [1] doesn't subsumes [*]");
    }

    @Test
    public void testEval()
    {
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){ {a|$a+1}->eval(1);}:Integer[1];\n" +
                "}");
        test("Class test::A" +
                "{" +
                "   name : String[1];" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   z(){ {a|$a+'1'}->eval(1);}:Integer[1];\n" +
                "}", "COMPILATION error at [1:63-66]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testPropertyPostFunction()
    {
        test("Class test::Firm" +
                "{" +
                "   employees:test::Person[*];" +
                "   emp(){$this.employees->first().lastName}:String[0..1];" +
                "}" +
                "" +
                "Class test::Person" +
                "{" +
                "   lastName : String[1];" +
                "}");
    }

    @Test
    public void testUnknownFunction()
    {
        test("Class test::Person[$this.lastName->ranDoMFuncTion()]{lastName:String[1];}",
                "COMPILATION error at [1:36-49]: Can't resolve the builder for function 'ranDoMFuncTion' - stack:[Class 'test::Person' Fourth Pass, Constraint 0, new lambda, Applying ranDoMFuncTion]");
    }

    @Test
    public void testEnum()
    {
        test("Enum test::A" +
                "{" +
                "   A,B" +
                "}" +
                "" +
                "Class test::B" +
                "{" +
                "   e:test::A[1];" +
                "   z(){ $this.e.name}:String[1];\n" +
                "}"
        );
    }

    @Test
    public void testFunction()
    {
        PureModel model = test("Class test::A" +
                "{" +
                "   s:String[1];" +
                "}" +
                "" +
                "function test::f(a:test::A[1]):String[1]" +
                "{" +
                "   $a.s;" +
                "}"
        ).getTwo();

        Function<?> f = model.getConcreteFunctionDefinition("test::f_test::A_1__String_1_", null);
        Assert.assertTrue(f instanceof ConcreteFunctionDefinition);
        ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) f;
        Assert.assertEquals("f_A_1__String_1_", cfd._name());
    }

    @Test
    public void testUserDefinedFunctionMatching()
    {
        test("Class test::A" +
                "{" +
                "   s:String[1];" +
                "}" +
                "function test::other(a:test::A[1]):String[1]" +
                "{" +
                "   test::f($a)" +
                "}" +
                "" +
                "function test::f(a:test::A[1]):String[1]" +
                "{" +
                "   $a.s;" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingError()
    {
        test("Class test::A" +
                        "{" +
                        "   s:String[1];" +
                        "}" +
                        "function test::other(a:test::A[1]):String[1]" +
                        "{" +
                        "   test::f('test')" +
                        "}" +
                        "" +
                        "function test::f(a:test::A[1]):String[1]" +
                        "{" +
                        "   $a.s;" +
                        "}",
                "COMPILATION error at [1:79-85]: Can't find a match for function 'test::f(String[1])'"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingInheritance()
    {
        test("Class test::B" +
                "{" +
                "   s:String[1];" +
                "}" +
                "" +
                "Class test::A extends test::B" +
                "{" +
                "}" +
                "function test::other(a:test::A[1]):String[1]" +
                "{" +
                "   test::f($a)" +
                "}" +
                "" +
                "function test::f(a:test::B[1]):String[1]" +
                "{" +
                "   $a.s;" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingInheritanceError()
    {
        test("Class test::B" +
                "{" +
                "   s:String[1];" +
                "}" +
                "" +
                "Class test::A extends test::B" +
                "{" +
                "}" +
                "function test::other(a:test::B[1]):String[1]" +
                "{" +
                "   test::f($a)" +
                "}" +
                "" +
                "function test::f(a:test::A[1]):String[1]" +
                "{" +
                "   $a.s;" +
                "}", "COMPILATION error at [1:110-116]: Can't find a match for function 'test::f(B[1])'"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingMultiplicity()
    {
        test("Class test::A" +
                "{" +
                "}" +
                "function test::other(a:test::A[1]):String[1]" +
                "{" +
                "   test::f($a)" +
                "}" +
                "" +
                "function test::f(a:test::A[*]):String[1]" +
                "{" +
                "   'bogus';" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingMultiplicityError()
    {
        test("Class test::A" +
                "{" +
                "}" +
                "function test::other(a:test::A[*]):String[1]" +
                "{" +
                "   test::f($a)" +
                "}" +
                "" +
                "function test::f(a:test::A[1]):String[1]" +
                "{" +
                "   'yo';" +
                "}", "COMPILATION error at [1:64-70]: Can't find a match for function 'test::f(A[*])'"
        );
    }

    @Test
    public void testFunctionReturnError()
    {
        test("Class test::A" +
                "{" +
                "   s:String[1];" +
                "}" +
                "" +
                "function test::f(a:test::A[1]):String[1]" +
                "{" +
                "   $a;" +
                "}", "COMPILATION error at [1:75-76]: Error in function 'test::f_test::A_1__String_1_' - Type error: 'test::A' is not a subtype of 'String'"
        );
    }

    @Test
    public void testFunctionReferenceBeforeFunctionDefinition()
    {
        test("function b::myFunction():String[1]" +
                "{" +
                "   z::otherFunction();" +
                "}" +
                "function z::otherFunction():String[1]" +
                "{" +
                "   'ok';" +
                "}"
        );
    }

    @Test
    public void testDeepfetch()
    {
        test("Class test::Person" +
                "{" +
                "   firstName:String[1];" +
                "   lastName:String[1];" +
                "}" +
                "Class test::Firm" +
                "{" +
                "   employees:test::Person[*];" +
                "}" +
                "Class test::Test" +
                "{" +
                "   x(){test::Person.all()->graphFetch(#{test::Person{firstName,lastName}}#);true;}:Boolean[1];" +
                "}");
    }

    @Test
    public void testDeepfetchPropertyError()
    {
        test("Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                // intentionally mess up the spacing in the deep fetch to see if we send the full string (with whitespaces) to the graph fetch tree parser
                "   x(){test::Person.all()->graphFetch(#{\n" +
                "       test::Person{\n" +
                "                first}}#);true;}:Boolean[1];\n" +
                "}\n", "COMPILATION error at [14:17-21]: Can't find property 'first' in [Person, Any]");
    }

    @Test
    public void testDeepfetchTypeError()
    {
        test("Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                "   x(){test::Person.all()->graphFetch(#{test::Peron{first}}#);true;}:Boolean[1];\n" +
                "}\n", "COMPILATION error at [12:41-51]: Can't find class 'test::Peron'");
    }

    @Test
    public void testMatchMaxFunction()
    {
        test("function example::testMaxString():Any[0..1]\n" + 
                "{\n"+
                "   ['string1', 'string2']->max();"+
                "}\n" +
                "function example::testMaxInteger():Any[0..1]\n" + 
                "{\n"+
                "   [1,2]->max();"+
                "}\n"+
                "function example::testMaxFloat():Any[0..1]\n" +
                "{\n"+
                "   [1.0,2.0]->max();"+
                "}\n"+
                "function example::testMaxDate():Any[0..1]\n" +
                "{\n"+
                "   [%1999-01-01,%2000-01-01]->max();"+
                "}\n"
        );
    }
    
    @Test
    public void testMatchWithImport()
    {
        test("Class example::MyTest\n" +
                "{\n" +
                "p:String[1];\n" +
                "}\n" +
                "###Pure\n" +
                "import example::*;\n" +
                "function example::testMatch(test:MyTest[1]): MyTest[1]\n" +
                "{\n" +
                "  $test->match([ a:MyTest[1]|$a ]);\n" +
                "}");
    }

    @Test
    public void testAutoImports()
    {
        // TODO: we probably should test more types here based on the list of auto-imports
        test("Class {doc.doc = 'test'} test::doc {\n" +
                "\n" +
                "}\n" +
                "\n" +
                "Class test2::doc {\n" +
                "prop: Any[1];\n" +
                "}\n" +
                "\n");
        test("###Pure\n" +
                "import meta::pure::profiles::*;\n" +
                "import meta::pure::tests::model::simple::*;\n" +
                "Enum meta::pure::tests::model::simple::GeographicEntityType\n" +
                "{\n" +
                "    {doc.doc = 'A city, town, village, or other urban area.'} CITY,\n" +
                "    <<doc.deprecated>> COUNTRY,\n" +
                "    {doc.doc = 'Any geographic entity other than a city or country.'} REGION\n" +
                "}");
    }

    @Test
    public void testImportResolutionPrecedence()
    {
        // NOTE: notice that in PURE, we only validate the type of the reference during validation
        // so this test will also list `meta::pure::profiles::doc` in the list of matching resolved paths
        test("import test2::*;\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::doc {}\n" +
                "Class test2::doc {}\n" +
                "\n" +
                "Class test::mewo {\n" +
                "   prop1: doc[1];\n" +
                "}", "COMPILATION error at [8:11-13]: Can't resolve element with path 'doc' - multiple matches found [test::doc, test2::doc]");
        test("import test2::*;\n" +
                "import test::*;\n" +
                "\n" +
                "Profile test::doc {}\n" +
                "Profile test2::doc {}\n" +
                "\n" +
                "Class <<doc.doc>> test::mewo {\n" +
                "}", "COMPILATION error at [7:9-11]: Can't resolve element with path 'doc' - multiple matches found [meta::pure::profiles::doc, test::doc, test2::doc]");
        // NOTE: since we disallow specifying having elements without a package
        // we can't test that primitive types and special types have precedence over
        // user defined elements at root package
    }

    @Test
    public void testDuplicatedImports()
    {
        // duplicated imports especially those that are similar to auto-imports are tolerated
        test("import meta::pure::profiles::*;\n" +
                "import meta::pure::profiles::*;\n" +
                "import meta::pure::profiles::*;\n" +
                "import random::path::*;\n" +
                "import random::path::*;\n" +
                "\n" +
                "Class {doc.doc = 'test'} test::doc {\n" +
                "\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testMissingProperty()
    {
        test("import anything::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){$this.name2->toOne()}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [4:23-27]: Can't find property 'name2' in class 'test::trial'");
        test("import anything::*;\n" +
                "import test::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){$this.name->toOne()}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "Class test::trial2 {\n" +
                "   name: trial[*];\n" +
                "   anotherOne(){$this.name->toOne().name2}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [10:37-41]: Can't find property 'name2' in class 'test::trial'");
    }

    @Test
    public void testMissingEnumValue()
    {
        test("import anything::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){ritual.theGoodOne1}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [4:24-34]: Can't find enum value 'theGoodOne1' in enumeration 'ritual'");
    }

    @Test
    public void testMissingStereotype()
    {
        test("import anything::*;\n" +
                "Class <<goes.businesstemporal>> {goes.doc = 'bla'} anything::A extends B, B\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n", "COMPILATION error at [2:9-29]: Can't find stereotype 'businesstemporal' in profile 'goes'");
    }

    @Test
    public void testMissingTag()
    {
        test("import anything::*;\n" +
                "Class <<goes.test>> {goes.todo2 = 'bla'} anything::A extends B, B\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n", "COMPILATION error at [2:27-31]: Can't find tag 'todo2' in profile 'goes'");
    }

    @Test
    public void testClassWithImport()
    {
        test("import anything::*;\n" +
                // Class stereotypes, tagged values and supertypes
                "Class <<goes.test>> {doc.doc = 'bla'} anything::A extends B\n" +
                "[\n" +
                "  $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false)\n" +
                "]\n" +
                "{\n" +
                // simple property stereotypes, tagged values and type
                "  {goes.todo = 'bla'} name: B[*];\n" +
                "  {goes.doc = 'bla'} ok: A[1..2];\n" +
                "  dance: enumGoes[1..2];\n" +
                // derived property parameter and return type
                "  <<goes.test>> {goes.doc = 'bla'} xza(s:B[1]) {$s.z + 'ok'}:String[1];\n" +
                "  anotherOne() {$this.name->toOne()}: B[1];\n" +
                "}\n" +
                "\n" +
                "Class anything::B\n" +
                "{\n" +
                "  z: String[1];\n" +
                "}" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "Enum anything::enumGoes\n" +
                "{\n" +
                "  c\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testEnumerationWithImport()
    {
        test("import anything::*;\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                // Enumeration tagged values and stereotypes
                "Enum <<goes.test>> {goes.doc = 'bla'} son::myEnum\n" +
                "{\n" +
                // Enum value tagged values and stereotypes
                "  <<goes.test>> {goes.doc = 'Tag Value for enum Value'} a,\n" +
                "  <<goes.test, goes.test>> {goes.doc = 'Tag Value for enum Value'} b,\n" +
                "  c\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testAssociationWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "Association <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} ahh::myAsso\n" +
                "{\n" +
                // `String` won't be valid here as we specifically look for a class
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} a: String[1];\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} b: goes2[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [11:3-69]: Can't find class 'String'");
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                // Association tagged values and stereotypes
                "Association <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} ahh::myAsso\n" +
                "{\n" +
                // Association property tagged values, stereotypes, and type
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} a: goes2[1];\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} b: goes2[1];\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testFunctionWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n" +
                // Function stereotypes, tagged values, parameter types, and return type
                "function <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} anything::f(s: goes2[1], s1:goes2[1]): goes2[1]\n" +
                "{\n" +
                "   f($s1, $s)\n" +
                "}");
    }


    @Test
    public void testBlockAny()
    {
        test("Class my::Class\n" +
                "{\n" +
                "\n" +
                "}\n" +
                "\n" +
                "Association my::association\n" +
                "{\n" +
                "\n" +
                "toAny:Any[1];\n" +
                "toClass:my::Class[1]; \n" +
                "\n" +
                "}", "COMPILATION error at [6:1-12:1]: Associations to Any are not allowed. Found in 'my::association'");

    }

    @Test
    public void testClassWithPath()
    {
        test("Class model::Person\n" +
                "{\n" +
                "    firstName: String[1];\n" +
                "    lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Firm\n" +
                "{\n" +
                "    legalName : String[1];\n" +
                "    employees: model::Person[*];\n" +
                "    employeesWithAddressNameSorted(){\n" +
                "       $this.employees->sortBy(#/model::Person/lastName#).lastName->joinStrings('')\n" +
                "    }:String[0..1];\n" +
                "}");
    }

    @Test
    public void testClassWithStrictDate()
    {
        test("Class apps::Trade\n" +
                "{\n" +
                "   date : StrictDate[1];\n" +
                "   testStrictDate(){\n" +
                "       $this.date == %2020-01-01;\n" +
                "   } : Boolean[1];\n" +
                "}\n");
    }
    @Test
    public void testClassWithBusinessTemporalMilesoning()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("Class apps::Employee \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "  firm: apps::Firm[1]; \n" +
                        "}\n\n" +
                        "Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Firm \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "} \n" +
                        "Association apps::Employee_Firm \n" +
                        "{ \n" +
                        "  worksFor: apps::Firm[*]; \n" +
                        "  employs: apps::Employee[*]; \n" +
                        "} \n");
        PureModel model = modelWithInput.getTwo();
        Type clazz = model.getType("apps::Employee", SourceInformation.getUnknownSourceInformation());
        Root_meta_pure_metamodel_type_Class_Impl<?> type = (Root_meta_pure_metamodel_type_Class_Impl<?>) clazz;
        org.eclipse.collections.api.block.function.Function<Class, RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property>> originalMilestonedPropertiesGetter = org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassAccessor::_originalMilestonedProperties;
        RichIterable<? extends Property> firmProperty = originalMilestonedPropertiesGetter.valueOf(type).select(p -> p.getName().equals("firm"));
        Assert.assertTrue("Missing firm property in _originalMilestonedProperties", firmProperty.size() == 1);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property> worksForProperty = originalMilestonedPropertiesGetter.valueOf(type).select(p -> p.getName().equals("worksFor"));
        Assert.assertTrue("Missing worksFor property in _originalMilestonedProperties", worksForProperty.size() == 1);
    }
}
