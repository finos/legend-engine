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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDomainGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testClass()
    {
        test("Class <<temporal.businesstemporal>> {doc.doc = 'bla'} A extends B\n" +
                "{\n" +
                "  <<equality.Key>> {doc.doc = 'bla'} name: e::R[*];\n" +
                "  {doc.doc = 'bla'} ok: Integer[1..2];\n" +
                "  <<devStatus.inProgress>> q(s: String[1]) {$s + 'ok'}: c::d::R[1];\n" +
                "  {doc.doc = 'bla'} xza(s: z::k::B[1]) {$s + 'ok'}: String[1];\n" +
                "}\n" +
                "\n" +
                "Class z::k::B\n" +
                "{\n" +
                "  z: String[1];\n" +
                "}\n");
    }

    @Test
    public void testComplexClass()
    {
        test("Class 'A-Z'\n" +
                "[\n" +
                "  constraint1: $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false),\n" +
                "  'constraint-3': $this.anyValue->instanceOf(String) || $this.anyValue->instanceOf(AEnum)\n" +
                "]\n" +
                "{\n" +
                "  name: String[45..*];\n" +
                "  ok: Integer[1..2];\n" +
                "  anyValue: Any[1];\n" +
                "  'maybe or maybe not!': Boolean[1];\n" +
                // Handle well new line and quotes inside of string
                "  xza(s: String[1]) {$s + 'ok\\n{\"\"}\\'\\''}: String[1];\n" +
                "  'I\\'m derived'('#String': String[1]) {$s + 'ok\\n{\"\"}\\'\\''}: String[1];\n" +
                "}\n" +
                "\n" +
                "Enum AEnum\n" +
                "{\n" +
                "  B\n" +
                "}\n");
    }

    @Test
    public void testUnNamedConstraintsAndEmptyProfile()
    {
        test("Class A\n" +
                "[\n" +
                "  $this.ok->toOne() == 1,\n" +
                "  named: if($this.ok == 'ok', |true, |false),\n" +
                "  $this.ok->toOne()->toString() == $this.name\n" +
                "]\n" +
                "{\n" +
                "  name: String[45..*];\n" +
                "  ok: Integer[1..2];\n" +
                "  xza(s: String[1]) {$s + 'ok'}: String[1];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "}\n");
    }

    @Test
    public void testComplexConstraints()
    {
        test("Class A\n" +
                "[\n" +
                "  c1\n" +
                "  (\n" +
                "    ~function: if($this.'O.K.' == 'ok', |true, |false)\n" +
                "    ~enforcementLevel: Warn\n" +
                "  ),\n" +
                "  c2\n" +
                "  (\n" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: if($this.'O.K.' == 'ok', |true, |false)\n" +
                "  ),\n" +
                "  c3\n" +
                "  (\n" +
                "    ~function: if($this.'O.K.' == 'ok', |true, |false)\n" +
                "    ~message: $this.'O.K.' + ' is not ok'\n" +
                "  ),\n" +
                "  c4\n" +
                "  (\n" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: if($this.'O.K.' == 'ok', |true, |false)\n" +
                "    ~enforcementLevel: Warn\n" +
                "    ~message: $this.'O.K.' + ' is not ok'\n" +
                "  )\n" +
                "]\n" +
                "{\n" +
                "  name: String[45..*];\n" +
                "  'O.K.': Integer[1..2];\n" +
                "  xza(s: String[1]) {$s + 'ok'}: String[1];\n" +
                "}\n");
    }

    @Test
    public void testClassWithMultipleTaggedAndStereotypes()
    {
        test("Class <<temporal.businesstemporal, taggedValue.Number2>> {doc.test1 = 'up1', doc.test2 = 'up2'} meta::this::class::has::path::A extends B\n" +
                "{\n" +
                "  <<equality.Key, taggedValue.test>> {doc.doc = 'Borrowers date of birth'} name: e::R[*];\n" +
                "  {Descriptor.descriptionA = 'test1', Descriptor.descriptionB = 'test2'} ok: Integer[1..2];\n" +
                "  <<devStatus.inProgress>> q(s: String[1]) {$s + 'ok'}: c::d::R[1];\n" +
                "  {doc.test1 = 'test1', doc.test2 = 'test2'} xza(s: z::k::B[1]) {$s + 'ok'}: String[1];\n" +
                "}\n");
    }

    @Test
    public void testClassWithQuotedTagsAndStereotypes()
    {
        test("Class <<temporal.businesstemporal, taggedValue.Number2>> {doc.test1 = 'up1', doc.test2 = 'up2'} meta::this::class::has::path::A extends B\n" +
                "{\n" +
                "  <<'a profile'.'1>stereo'>> {'a profile'.'2>tag' = 'Borrowers date of birth'} name: e::R[*];\n" +
                "  {Descriptor.descriptionA = 'test1', Descriptor.descriptionB = 'test2'} ok: Integer[1..2];\n" +
                "}\n");
    }

    @Test
    public void testTaggedValuesSpecialChar()
    {
        test("Class <<temporal.businesstemporal, taggedValue.Number2>> {doc.test1 = 'test1\\'s', doc.test2 = 'm\\'s test'} meta::this::class::has::path::A\n" +
                "{\n" +
                "  <<equality.Key, taggedValue.test>> {doc.doc = 'uyaguari\\'s test', doc.test2 = 'm\\'s test'} name: e::R[*];\n" +
                "}\n");
    }

    @Test
    public void testEnumerations()
    {
        test("Enum <<st.test>> {doc.doc = 'bla'} myEnum\n" +
                "{\n" +
                "  <<equality.Key, taggedValue.test>> {doc.doc = 'Tag Value for enum Value'} a,\n" +
                "  <<equality.Key, taggedValue.test>> {doc.doc = 'Tag Value for enum Value'} b,\n" +
                "  c\n" +
                "}\n" +
                "\n" +
                "Enum <<st.test>> {doc.doc = 'bla'} zz::MyOther\n" +
                "{\n" +
                "  e,\n" +
                "  g,\n" +
                "  r\n" +
                "}\n");
    }

    @Test
    public void testQuotedEnumerations()
    {
        test("Enum <<st.test>> {doc.doc = 'bla'} '@'::'my Enum'\n" +
                "{\n" +
                "  'Anything e',\n" +
                "  'A g',\n" +
                "  'Anything r'\n" +
                "}\n");
    }

    @Test
    public void testAssociations()
    {
        test("Association myAsso\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "Association {doc.doc = 'bla'} k::p::Asso\n" +
                "{\n" +
                "  a: Integer[1];\n" +
                "  b: a::c::B[1];\n" +
                "}\n");
    }

    @Test
    public void testProfile()
    {
        test("Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n");
    }

    @Test
    public void testQuotedProfile()
    {
        test("Profile meta::pure::profiles::'with quotes'\n" +
                "{\n" +
                "  stereotypes: ['two words'];\n" +
                "  tags: ['s tag', 'another tag'];\n" +
                "}\n");
    }

    @Test
    public void testMeasure()
    {
        test("Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "  UnitTwo: x -> $x * 1000;\n" +
                "  UnitThree: x -> $x * 400;\n" +
                "}\n");
    }

    @Test
    public void testNonConvertibleMeasure()
    {
        test("Measure NewMeasure\n" +
                "{\n" +
                "  UnitOne;\n" +
                "  UnitTwo;\n" +
                "  UnitThree;\n" +
                "}\n");
    }

    @Test
    public void testQuotedMeasure()
    {
        test("Measure 'some measure'\n" +
                "{\n" +
                "  *'Unit One': x -> $x;\n" +
                "  'Unit Two': x -> $x * 1000;\n" +
                "  'Unit Three': x -> $x * 400;\n" +
                "}\n");
    }

    @Test
    public void testQuotedNonConvertibleMeasure()
    {
        test("Measure 'some measure'\n" +
                "{\n" +
                "  'Unit One';\n" +
                "  'Unit Two';\n" +
                "  'Unit Three';\n" +
                "}\n");
    }

    @Test
    public void testClassWithUnitRelatedProperties()
    {
        test("Class A\n" +
                "{\n" +
                "  unitOne: NewMeasure~UnitOne[0..1];\n" +
                "  unitTwo: NewMeasure~UnitTwo[0..1];\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "  UnitTwo: x -> $x * 1000;\n" +
                "  UnitThree: x -> $x * 400;\n" +
                "}\n");
    }

    @Test
    public void testClassWithNonConvertibleUnitProperties()
    {
        test("Class A\n" +
                "{\n" +
                "  unitOne: NewMeasure~UnitOne[0..1];\n" +
                "  unitTwo: NewMeasure~UnitTwo[0..1];\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  UnitOne;\n" +
                "  UnitTwo;\n" +
                "  UnitThree;\n" +
                "}\n");
    }

    @Test
    public void testPackageWithQuotedIdentifier()
    {
        test("Class test::'p a c k a g e'::A\n" +
                "{\n" +
                "  's t r i n g': String[1];\n" +
                "}\n");
    }

    @Test
    public void testDomainMixed()
    {
        test("Class <<temporal.businesstemporal>> {doc.doc = 'bla'} A extends B\n" +
                "[\n" +
                "  constraint1: $this.ok->toOne() == 1,\n" +
                "  constraint2: $this.ok->toOne()->toString() == $this.name\n" +
                "]\n" +
                "{\n" +
                "  <<equality.Key>> {doc.doc = 'bla'} name: e::R[*];\n" +
                "  {doc.doc = 'bla'} ok: Integer[1..2];\n" +
                "  <<devStatus.inProgress>> q(s: String[1]) {$s + 'ok'}: c::d::R[1];\n" +
                "  {doc.doc = 'bla'} xza(s: z::k::B[1]) {$s + 'ok'}: String[1];\n" +
                "}\n" +
                "\n" +
                "Association myAsso\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "Enum <<st.test>> {doc.doc = 'bla'} z::k::B\n" +
                "{\n" +
                "  <<equality.Key, taggedValue.test>> {doc.doc = 'Tag Value for enum Value'} a,\n" +
                "  b,\n" +
                "  c\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::profile2\n" +
                "{\n" +
                "  tags: [doc, todo];\n" +
                "}\n");
    }

    @Test
    public void testFunction()
    {
        test("function withPath::f(s: Integer[1]): String[1]\n" +
                "{\n" +
                "   'ok'->println();\n" +
                "   'a';\n" +
                "}\n");
    }

    @Test
    public void testBooleanPrecedence1()
    {
        testFormat("function withPath::f(s: Integer[1]): String[1]\n" +
                "{\n" +
                "   false || (true && false);\n" +
                "   'a';\n" +
                "}\n", "function withPath::f(s: Integer[1]): String[1]\n" +
                "{\n" +
                "   false || true && false;\n" +
                "   'a';\n" +
                "}\n");
    }

    @Test
    public void testBooleanPrecedence2()
    {
        testFormat("function withPath::f(s: Integer[1]): String[1]\n" +
                "{\n" +
                "   (false || (true && false)) || false;\n" +
                "   'a';\n" +
                "}\n", "function withPath::f(s: Integer[1]): String[1]\n" +
                "{\n" +
                "   false || true && false || false;\n" +
                "   'a';\n" +
                "}\n");
    }


    @Test
    public void testFunction2()
    {
        test("function f(s: Integer[1], s2: Interger[2]): String[1]\n" +
                "{\n" +
                "   'ok'->println()\n" +
                "}\n");
    }

    @Test
    public void testMathParenthesis0()
    {
        test("function f(s: Integer[1], s2: Interger[2]): String[1]\n" +
                "{\n" +
                "   let a = (1 - 4 * (2 + 3)) * 4\n" +
                "}\n");
    }

    @Test
    public void testMathParenthesis1()
    {
        test("function f(s: Integer[1], s2: Interger[2]): String[1]\n" +
                "{\n" +
                "   let a = 4 + 1 * (2 + 3) * 4 + (2 + 3) * 4\n" +
                "}\n");
    }

    @Test
    public void testMathParenthesis2()
    {
        test("function f(s: Integer[1], s2: Interger[2]): String[1]\n" +
                "{\n" +
                "   let a = 4 + (1 - 2) / (2 + 3) * (1 - 4 - 5)\n" +
                "}\n");
    }

    @Test
    public void testCollectionWithFunction()
    {
        test("function package::test(value: meta::pure::metamodel::type::Any[0..1]): Boolean[1]\n" +
                "{\n" +
                "   [(true && (false && false)), false]->oneOf()\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithNew()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  v: String[1];\n" +
                "}\n\n" +
                "function f(): Any[1]\n" +
                "{\n" +
                "   let x = ^anything::goes(v='value')\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithNewAlltypes()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  v: String[1];\n" +
                "  v2: Integer[0..1];\n" +
                "  v3: Boolean[*];\n" +
                "}\n\n" +
                "function f(): Any[1]\n" +
                "{\n" +
                "   let x = ^anything::goes(v='value' , v2=17 , v3=[true, false])\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithNewAndNewValue()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  v: String[1];\n" +
                "}\n" +
                "\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "  v2: anything::goes[1];\n" +
                "}\n" +
                "\n" +
                "function f(): Any[1]\n" +
                "{\n" +
                "   let x = ^anything::goes2(v2=^anything::goes(v='value'))\n" +
                "}\n");
    }

    @Test
    public void testMultiIFExpressions()
    {
        test("function test::multiExpressions(): String[0..1]\n" +
                "{\n" +
                "   if(true, {|\n" +
                "let test = 'test';\n" +
                "$test;\n" +
                "}, |'fgh')\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithQuotedParameters()
    {
        test("function test::qoutedParams('1,2,3': Integer[3]): String[0..1]\n" +
                "{\n" +
                "   $'1,2,3'->map(n|$n->toString())->joinStrings(',')\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithQuotedVariables()
    {
        test("function test::qoutedParams(): String[0..1]\n" +
                "{\n" +
                "   let '1,2,3' = [1, 2, 3];\n" +
                "   $'1,2,3'->map(n|$n->toString())->joinStrings(',');\n" +
                "}\n");
    }

    @Test
    public void testClassWithImport()
    {
        test("import anything::*;\n" +
                "Class <<goes.businesstemporal>> {goes.doc = 'bla'} anything::A extends B, B\n" +
                "[\n" +
                "  $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false)\n" +
                "]\n" +
                "{\n" +
                "  <<goes.Key>> {goes.doc = 'bla'} name: e::R[*];\n" +
                "  {goes.doc = 'bla'} ok: A[1..2];\n" +
                "  dance: enumGoes[1..2];\n" +
                "  <<goes.inProgress>> q(s: String[1]) {$s + 'ok'}: c::d::R[1];\n" +
                "  {robot::hio.doc = 'bla'} xza(s: z::k::B[1]) {$s + 'ok'}: String[1];\n" +
                "  <<goes.inProgress>> {robot::hio.doc = 'bla'} anotherOne(s: goes[1]) {$s + 'ok'}: goes[1];\n" +
                "}\n" +
                "\n" +
                "Class anything::B\n" +
                "{\n" +
                "  z: String[1];\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Enum anything::enumGoes\n" +
                "{\n" +
                "  c\n" +
                "}\n");
    }

    @Test
    public void testEnumerationWithImport()
    {
        test("import anything::*;\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Enum <<goes.test>> {goes.doc = 'bla'} myEnum\n" +
                "{\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for enum Value'} a,\n" +
                "  <<goes.test, goes.test>> {goes.doc = 'Tag Value for enum Value'} b,\n" +
                "  c\n" +
                "}\n");
    }

    @Test
    public void testAssociationWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "Association <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} myAsso\n" +
                "{\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} a: String[1];\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} b: goes2[1];\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "function <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} f(s: goes2[1], s1: goes2[1]): goes2[1]\n" +
                "{\n" +
                "   'ok'->println()\n" +
                "}\n");
    }
}
