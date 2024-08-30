// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.runtime.java.extension.external.json.natives;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestFromJson extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void typeCheck_JsonStringToBooleanProperty()
    {
        runShouldFailTestCase(
                "StringToBoolean",
                "Boolean[1]",
                "\"foo\"",
                "Expected Boolean, found String"
        );
    }

    @Test
    public void typeCheck_JsonBooleanToStringProperty()
    {
        runShouldFailTestCase(
                "BooleanToString",
                "String[1]",
                "true",
                "Expected String, found Boolean"
        );
    }

    @Test
    public void typeCheck_JsonStringToFloatProperty()
    {
        runShouldFailTestCase(
                "StringToFloat",
                "Float[1]",
                "\"3.0\"",
                "Expected Number, found String"
        );
    }

    @Test
    public void typeCheck_JsonFloatToIntegerProperty()
    {
        runShouldFailTestCase(
                "FloatToInteger",
                "Integer[1]",
                "3.0",
                "Expected Integer, found Float");
    }

    @Test
    public void typeCheck_JsonIntegerToDateProperty()
    {
        runShouldFailTestCase(
                "IntegerToDate",
                "Date[1]",
                "3",
                "Expected Date, found Integer"
        );
    }

    @Test
    public void typeCheck_JsonStringToDateProperty()
    {
        runShouldFailTestCase(
                "StringToDate",
                "Date[1]",
                "\"foo\"",
                "Expected Date, found String"
        );
    }

    @Test
    public void typeCheck_JsonIntegerToEnumProperty()
    {
        runShouldFailTestCase(
                "IntegerToEnum",
                "meta::pure::functions::json::tests::TestingEnum[1]",
                "2",
                "Expected meta::pure::functions::json::tests::TestingEnum, found Integer",
                "Enum meta::pure::functions::json::tests::TestingEnum { Value1, Value2 } "
        );
    }

    @Test
    public void typeCheck_JsonStringToEnumProperty()
    {
        runShouldFailTestCase(
                "StringToEnum",
                "meta::pure::functions::json::tests::TestingEnum[1]",
                "\"foo\"",
                "Unknown enum: meta::pure::functions::json::tests::TestingEnum.foo",
                "Enum meta::pure::functions::json::tests::TestingEnum { Value1, Value2 } "
        );
    }

    @Test
    public void typeCheck_JsonIntegerToObjectProperty()
    {
        runShouldPassTestCase(
                "IntegerToObject",
                "meta::pure::functions::json::tests::someClass[1]",
                "3",
                "Class meta::pure::functions::json::tests::someClass {  } ",
                "[]"
        );
    }

    @Test
    public void typeCheck_JsonObjectToIntegerProperty()
    {
        runShouldFailTestCase(
                "ObjectToInteger",
                "Integer[1]",
                "{}",
                "Expected Integer, found JSON Object"
        );
    }

    @Test
    public void typeCheck_JsonIntegerToFloatProperty()
    {
        runShouldPassTestCase(
                "IntegerToFloat",
                "Float[1]",
                "1",
                "1.0"
        );
    }

    @Test
    public void typeCheck_JsonIntegerToAnyProperty()
    {
        runShouldPassTestCase(
                "IntegerToAny",
                "Any[1]",
                "1",
                "1"
        );
    }

    @Test
    public void typeCheck_JsonFloatToAnyProperty()
    {
        runShouldPassTestCase(
                "FloatToAny",
                "Any[1]",
                "2.0",
                "2.0"
        );
    }

    @Test
    public void typeCheck_JsonStringToAnyProperty()
    {
        runShouldPassTestCase(
                "StringToAny",
                "Any[1]",
                "\"Hello\"",
                "'Hello'"
        );
    }

    @Test
    public void typeCheck_JsonBooleanToAnyProperty()
    {
        runShouldPassTestCase(
                "BooleanToAny",
                "Any[1]",
                "true",
                "true"
        );
    }

    @Test
    public void typeCheck_JsonObjectToAnyProperty()
    {
        runShouldFailTestCase(
                "ObjectToAny",
                "Any[1]",
                "{}",
                "Deserialization of Any currently only supported on primitive values!"
        );
    }

    @Test
    public void typeCheck_JsonFloatToDecimalProperty()
    {
        runShouldPassTestCase(
                "FloatToDecimal",
                "Decimal[1]",
                "3.14",
                "3.14D"
        );
    }

    @Test
    public void typeCheck_JsonIntegerToDecimalProperty()
    {
        runShouldPassTestCase(
                "IntegerToDecimal",
                "Decimal[1]",
                "3",
                "3D"
        );
    }

    @Test
    public void typeCheck_JsonStringToDecimalProperty()
    {
        runShouldFailTestCase(
                "StringToDecimal",
                "Float[1]",
                "\"3.0\"",
                "Expected Number, found String"
        );
    }

    @Test
    public void multiplicityIsInRange_TwoWhenExpectingThree()
    {
        runShouldFailTestCase(
                "TwoToThree",
                "Integer[3]",
                "[1,2]",
                "Expected value(s) of multiplicity [3], found 2 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_FourWhenExpectingThree()
    {
        runShouldFailTestCase(
                "FourToThree",
                "Integer[3]",
                "[1,2,3,4]",
                "Expected value(s) of multiplicity [3], found 4 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_TwoWhenExpectingOne()
    {
        runShouldFailTestCase(
                "TwoToOne",
                "Integer[1]",
                "[1,2]",
                "Expected value(s) of multiplicity [1], found 2 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_OneWhenExpectingTwo()
    {
        runShouldFailTestCase(
                "OneToTwo",
                "Integer[2]",
                "1",
                "Expected value(s) of multiplicity [2], found 1 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_NullToSingletonProperty()
    {
        runShouldFailTestCase(
                "NullToSingleton",
                "Float[1]",
                "null",
                "Expected value(s) of multiplicity [1], found 0 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToSingletonProperty()
    {
        runShouldFailTestCase(
                "EmptyToSingleton",
                "Float[1]",
                "[]",
                "Expected value(s) of multiplicity [1], found 0 value(s)."
        );
    }

    @Test
    public void multiplicityIsInRange_SingleValueToSingletonProperty()
    {
        runShouldPassTestCase(
                "SingleToSingleton",
                "Float[1]",
                "3.0",
                "3.0"
        );
    }

    @Test
    public void multiplicityIsInRange_SingleArrayToSingletonProperty()
    {
        runShouldPassTestCase(
                "SingleArrayToSingleton",
                "Float[1]",
                "[3.0]",
                "3.0"
        );
    }

    @Test
    public void multiplicityIsInRange_NullToOptionalProperty()
    {
        runShouldPassTestCase(
                "NullToOptional",
                "Float[0..1]",
                "null",
                "[]"
        );
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToOptionalProperty()
    {
        runShouldPassTestCase(
                "EmptyToOptional",
                "Float[0..1]",
                "[]",
                "[]"
        );
    }

    @Test
    public void multiplicityIsInRange_NullToManyProperty()
    {
        runShouldPassTestCase(
                "NullToMany",
                "Float[*]",
                "null",
                "[]"
        );
    }

    @Test
    public void multiplicityIsInRange_EmptyArrayToManyProperty()
    {
        runShouldPassTestCase(
                "EmptyToMany",
                "Float[*]",
                "[]",
                "[]"
        );
    }

    @Test
    public void multiplicityIsInRange_SingleValueToManyProperty()
    {
        runShouldPassTestCase(
                "SingleToMany",
                "Float[*]",
                "3.0",
                "[3.0]"
        );
    }

    @Test
    public void multiplicityIsInRange_SingleArrayToManyProperty()
    {
        runShouldPassTestCase(
                "SingleArrayToMany",
                "Float[*]",
                "[3.0]",
                "[3.0]"
        );
    }

    @Test
    public void multiplicityIsInRange_Association()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::Employment\n" +
                "{\n" +
                "    employer : Firm[1];\n" +
                "    employees : Person[7];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Person\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Firm\n{\n" +
                "}\n" +
                "function Association():Any[*]\n" +
                "{\n" +
                // this json describes a Firm with one employee, but the association states all Firms must have exactly 7 employees.
                "    let json = '{\"employees\":[{\"employer\":{\"employees\":[{}]}}]}';\n" +
                "    $json -> fromJson(Firm, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";
        String testFunction = "Association():Any[*]";
        String exceptionMessage = "Error populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \n" +
                "Expected value(s) of multiplicity [7], found 1 value(s).";
        assertFailsExecution(sourceCode, testFunction, exceptionMessage);
    }

    @Test
    public void multiplicityIsInRange_Association_nestedClasses()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::Employment\n" +
                "{\n" +
                "    employer : Firm[1];\n" +
                "    employees : Person[7];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Person\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Firm\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::OfficeBuilding\n" +
                "{\n" +
                "    firms: Firm[1];\n" +
                "}\n" +
                "function Association():Any[*]\n" +
                "{\n" +
                "    let json = '{\"firms\": {\"employees\":[{\"employer\":{\"employees\":{}}}]}}';\n" +
                "    $json -> fromJson(OfficeBuilding, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";
        String testFunction = "Association():Any[*]";
        String exceptionMessage = "Error populating property 'firms' on class 'meta::pure::functions::json::tests::OfficeBuilding': \n" +
                "Error populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \n" +
                "Expected value(s) of multiplicity [7], found 1 value(s).";
        assertFailsExecution(sourceCode, testFunction, exceptionMessage);
    }

    @Test
    public void multiplicityIsInRange_Association_multipleAssociations()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::Employment\n" +
                "{\n" +
                "    employer : Firm[1];\n" +
                "    employees : Person[7];\n" +
                "}\n" +
                "Association meta::pure::functions::json::tests::Occupancy\n" +
                "{\n" +
                "    building : OfficeBuilding[1];\n" +
                "    occupant : Firm[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Person\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Firm\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::OfficeBuilding\n" +
                "{\n" +
                "}\n" +
                "function Association():Any[*]\n" +
                "{\n" +
                "    let json = '{\"occupant\": {\"building\": {\"occupant\": {}}, \"employees\":[{\"employer\":{\"employees\":{}}}]}}';\n" +
                "    $json -> fromJson(OfficeBuilding, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";
        String testFunction = "Association():Any[*]";
        String exceptionMessage = "Error populating property 'occupant' on class 'meta::pure::functions::json::tests::OfficeBuilding': \n" +
                "Error populating property 'employees' on class 'meta::pure::functions::json::tests::Firm': \n" +
                "Expected value(s) of multiplicity [7], found 1 value(s).";
        assertFailsExecution(sourceCode, testFunction, exceptionMessage);
    }

    @Test
    public void multiplicityIsInRange_Association_jsonLooksLikeAssociation()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "    bar : Bar[1];\n" +
                "    check : String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Bar\n" +
                "{\n" +
                "    foo : Foo[*];\n" +
                "}\n" +
                "function mimic():Any[*]\n" +
                "{\n" +
                "    let json = '{\"check\": \"passes here\", \"bar\": {\"foo\": {\"check\": \"and here\", \"bar\": {}}}}';\n" +
                "    $json -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunction = "mimic():Any[*]";
        assertPassesExecution(sourceCode, testFunction);
    }

    @Test
    public void multiplicityIsInRange_Association_inferredCycleFromModel()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::Occupancy\n" +
                "{\n" +
                "    building : OfficeBuilding[1];\n" +
                "    occupant : Firm[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Firm\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::OfficeBuilding\n" +
                "{\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Campus\n" +
                "{\n" +
                "    buildings : OfficeBuilding[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "    let json = '{\"buildings\": {\"occupant\": {\"building\": {}}}}';\n" +
                "    $json -> fromJson(Campus, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunction = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunction);
    }

    @Test
    public void propertiesFromAssociationOnSuperclass()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::SuperclassAssoc\n" +
                "{\n" +
                "    a : A[1];\n" +
                "    b : B[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "{\n" +
                "   str : String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::B {}\n" +
                "Class meta::pure::functions::json::tests::C extends A {}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "    let json = '{\"str\": \"bar\", \"b\": {\"a\": {\"str\": \"foo\"}}}';\n" +
                "    let o = $json -> fromJson(C, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "    assert($o.str == 'bar', |'');\n" +
                "}";
        String testFunction = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunction);
    }

    @Test
    public void propertiesFromAssociationOnSuperclass_NestedObjectHasSuperfluous()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::SuperclassAssoc\n" +
                "{\n" +
                "    a : A[1];\n" +
                "    b : B[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A {}\n" +
                "Class meta::pure::functions::json::tests::B {}\n" +
                "Class meta::pure::functions::json::tests::C extends A\n" +
                "{\n" +
                "   str : String[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "    let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n\n" +
                "    let json = '{\"str\": \"foo\", \"b\": {\"a\": {\"str\": \"foo\"}}}';\n" +
                "    let o = $json -> fromJson(C, $config);\n" +
                "}";
        String testFunction = "foo():Any[*]";
        String expectedException = "Error populating property 'b' on class 'meta::pure::functions::json::tests::C': \n" +
                "Error populating property 'a' on class 'meta::pure::functions::json::tests::B': \n" +
                "Property 'str' can't be found in class meta::pure::functions::json::tests::A. ";
        assertFailsExecution(sourceCode, testFunction, expectedException);
    }

    @Test
    public void multiplicityIsInRange_toOne_missing()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::must\n" +
                "{\n" +
                "    testField : String[1];\n" +
                "}\n" +
                "function InRangeToOne():Any[*]\n" +
                "{\n" +
                "    let json = '{}';\n" +
                "    $json -> fromJson(must, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunction = "InRangeToOne():Any[*]";
        String expectedException = "Error populating property 'testField' on class 'meta::pure::functions::json::tests::must': \n" +
                "Expected value(s) of multiplicity [1], found 0 value(s).";
        assertFailsExecution(sourceCode, testFunction, expectedException);
    }

    @Test
    public void multiplicityIsInRange_givenValueExpectingArray()
    {
        runShouldFailTestCase(
                "givenValueExpectingArray",
                "String[2]",
                "\"foo\"",
                "Expected value(s) of multiplicity [2], found 1 value(s)."
        );
    }

    @Test
    public void failOnMissingField()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::MissingData\n" +
                "{\n" +
                "    missing : Integer[1];\n" +
                "}\n" +
                "function missing():Any[*]\n" +
                "{\n" +
                "    let json = '{}';\n" +
                "    $json -> fromJson(MissingData, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "missing():Any[*]";
        String expectedException = "Error populating property 'missing' on class 'meta::pure::functions::json::tests::MissingData': \n" +
                "Expected value(s) of multiplicity [1], found 0 value(s).";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void deserializationConfig_failOnUnknownProperties()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::failUnknown\n" +
                "{\n" +
                "    testField : Integer[1];\n" +
                "}\n" +
                "function failUnknown():Any[*]\n" +
                "{\n" +
                "    let json = '{\"testField\": 1,\"secondProperty\":2}';\n" +
                "    let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n" +
                "    $json -> fromJson(failUnknown, $config);\n" +
                "}";
        String testFunc = "failUnknown():Any[*]";
        String expectedException = "Property 'secondProperty' can't be found in class meta::pure::functions::json::tests::failUnknown. ";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void deserializationConfig_wrongTypeKeyName()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::failUnknown\n" +
                "{\n" +
                "  testField : Integer[1];\n" +
                "}\n" +
                "function TypeKey():Any[*]\n" +
                "{\n" +
                "  let json = '{\"testField\": 1,\"__TYPE\":\"failUnknown\"}';\n" +
                "  let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true);\n" +
                "  $json -> fromJson(failUnknown, $config);\n" +
                "}";
        String testFunc = "TypeKey():Any[*]";
        String expectedException = "Property '__TYPE' can't be found in class meta::pure::functions::json::tests::failUnknown. ";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testFromJsonThrowsValidationErrors()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "[ TEST_CONTROL: $this.a == 'dave' ]\n" +
                "{\n" +
                "  a:String[1];\n" +
                "}\n" +
                "\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{ \"a\": \"fred\" }'->meta::json::fromJson(meta::pure::functions::json::tests::A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  assert(!$json->isEmpty(), |'');\n" +
                "  assert('fred' == $json.a, |'');\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Could not create new instance of meta::pure::functions::json::tests::A: \nConstraint :[TEST_CONTROL] violated in the Class A";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testFromJsonDoesNotThrowValidationErrors()
    {
        String sourceCode = "import meta::json::*;\n" +
                "function myFunc(o:Any[1]):Any[1]\n" +
                "{\n" +
                "   $o;\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "[ TEST_CONTROL: $this.a == 'dave' ]\n" +
                "{\n" +
                "  a:String[1];\n" +
                "}\n" +
                "\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let config = ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=true, constraintsHandler=^ConstraintsOverride(constraintsManager=myFunc_Any_1__Any_1_));\n" +
                "  let json='{ \"a\": \"fred\" }'->meta::json::fromJson(meta::pure::functions::json::tests::A, $config);\n" +
                "  assert(!$json->isEmpty(), |''); \n" +
                "  assert('fred' == $json.a, |''); \n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void testFromJsonThrowsValidationErrors_ConstraintOnRoot()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::A {}\n" +
                "Class meta::pure::functions::json::tests::B\n" +
                "[ TEST_CONTROL: $this.str == 'dave' ]\n" +
                "{\n" +
                "   a:meta::pure::functions::json::tests::A[1];\n" +
                "   str:String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{ \"str\": \"fred\", \"a\": {} }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  assert(!$json->isEmpty(), |'');\n" +
                "  assert('fred' == $json.str, |'');\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Could not create new instance of meta::pure::functions::json::tests::B: \n" +
                "Constraint :[TEST_CONTROL] violated in the Class B";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testFromJsonThrowsValidationErrors_ConstraintOnProperty()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "[ TEST_CONTROL: $this.a == 'dave' ]\n" +
                "{\n" +
                "  a:String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::B\n" +
                "{\n" +
                "  b:meta::pure::functions::json::tests::A[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{ \"b\": {\"a\": \"fred\" } }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  assert(!$json->isEmpty(), |'');\n" +
                "  assert('fred' == $json.b.a, |'');\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Error populating property 'b' on class 'meta::pure::functions::json::tests::B': \n" +
                "Could not create new instance of meta::pure::functions::json::tests::A: \nConstraint :[TEST_CONTROL] violated in the Class A";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testFromJsonThrowsValidationErrors_RootConstraintOnPropertyClass()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "{\n" +
                "  a:String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::B\n" +
                "[ TEST_CONTROL: $this.b.a == 'dave' ]\n" +
                "{\n" +
                "  b:meta::pure::functions::json::tests::A[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{ \"b\": {\"a\": \"fred\" } }'->meta::json::fromJson(meta::pure::functions::json::tests::B, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  assert(!$json->isEmpty(), |'');\n" +
                "  assert('fred' == $json.b.a, |'');\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Could not create new instance of meta::pure::functions::json::tests::B: \n" +
                "Constraint :[TEST_CONTROL] violated in the Class B";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testFromJsonThrowsValidationErrors_Association()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::a\n" +
                "{ string : String[1]; }\n" +
                "Class meta::pure::functions::json::tests::b\n" +
                "[ TEST_CONTROL: $this.A.string == 'fred' ]\n" +
                "{\n" +
                "  string : String[1];\n" +
                "}\n" +
                "Association meta::pure::functions::json::tests::Assoc\n" +
                "{\n" +
                "   A : meta::pure::functions::json::tests::a[1];\n" +
                "   B : meta::pure::functions::json::tests::b[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{ \"string\": \"dave\", \"B\": {\"string\": \"fred\", \"A\": { \"string\": \"dave\" } } }'->meta::json::fromJson(meta::pure::functions::json::tests::a, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Error populating property 'B' on class 'meta::pure::functions::json::tests::a': \n" +
                "Could not create new instance of meta::pure::functions::json::tests::b: \nConstraint :[TEST_CONTROL] violated in the Class b";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void recursiveStructures()
    {
        String basePattern = "{\"INT\": 1}";
        String recursivePattern = "{\"INT\": 0, \"FOO\": %s}";
        String json = recursivePattern;
        int recursiveDepth = 2;
        for (int i = 0; i < recursiveDepth - 1; i++)
        {
            json = String.format(json, recursivePattern);
        }
        json = String.format(json, basePattern);


        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "    FOO : Foo[0..1];\n" +
                "    INT : Integer[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "    '" + json + "' -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void idConflicts()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "   FOO : Foo[1];\n" +
                "   str : String[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "   let fromJson = '{\"@id\": 1, \"str\": \"one\", \"FOO\": {\"@id\": 1, \"str\": \"two\", \"FOO\": 1}}' -> fromJson(Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   assert($fromJson.FOO.FOO == [], |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void idKeysWithAssociation()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::_assoc\n" +
                "{\n" +
                "  a : _A[1];\n" +
                "  b : _B[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::_A\n" +
                "{\n" +
                "  str : String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::_B {}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "  let json = '{\"@id\": 1, \"str\": \"foo\", \"b\": {\"a\": 1}}';\n" +
                "  let o = $json -> fromJson(_A, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  let _b = $o.b;\n" +
                "  assert($_b == $_b.a.b, |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void idKeysWithExplicitReferences()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::ClassOne\n" +
                "{\n" +
                "  two : ClassTwo[1];\n" +
                "  str : String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::ClassTwo\n" +
                "{\n" +
                "   one : ClassOne[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "  let json = '{\"@id\": 1, \"str\": \"foo\", \"two\": {\"one\": 1}}';\n" +
                "  let one = $json -> fromJson(ClassOne, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  let two = $one.two;\n" +
                "  assert($two.one == [], |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void regularAssociation()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Association meta::pure::functions::json::tests::assoc\n" +
                "{\n" +
                "  a : A[1];\n" +
                "  b : B[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A\n" +
                "{\n" +
                "  str : String[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::B {}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "  let json = '{\"str\": \"foo\", \"b\": {}}';\n" +
                "  let o = $json -> fromJson(A, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "  let _b = $o.b -> toOne();\n" +
                "  assert($_b == $_b.a.b, |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void typeResolving_jsonArrayOfDifferentTypes()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Parent\n" +
                "{\n" +
                "   a : A[2];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A {}\n" +
                "Class meta::pure::functions::json::tests::B extends A\n" +
                "{\n" +
                "   float : Float[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::C extends A\n" +
                "{\n" +
                "   string : String[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "   let json = '{\"a\": [{\"@type\": \"B\", \"float\": 4167}, {\"@type\": \"C\", \"string\": \"foo\"}]}';\n" +
                "   let o = $json -> fromJson(Parent, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   assert(4167.0 == $o.a -> first() -> cast(@B).float, |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void typeResolving_customTypeKeyName()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::B\n" +
                "{\n" +
                "   float : Float[1];\n" +
                "}\n" +
                "function foo():Any[*]\n" +
                "{\n" +
                "   let config = ^JSONDeserializationConfig(typeKeyName='__TYPE', failOnUnknownProperties=true);\n" +
                "   let json = '{\"__TYPE\": \"B\", \"float\": 4167}';\n" +
                "   let o = $json -> fromJson(B, $config);\n" +
                "   assert(4167.0 == $o.float, |'');\n" +
                "}";
        String testFunc = "foo():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void typeResolving_noValidType()
    {
        String sourceCode = "import meta::json::*;\n" +
                "import meta::pure::functions::json::tests::*;\n" +
                "Class meta::pure::functions::json::tests::Parent\n" +
                "{\n" +
                "   a : A[1];\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::A {}\n" +
                "Class meta::pure::functions::json::tests::B extends A {}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   let json = '{\"a\": {\"float\": 4167}}';\n" +
                "   let o = $json -> fromJson(Parent, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void missingData()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::a\n" +
                "{\n" +
                "  string : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  let json='{}'->fromJson(meta::pure::functions::json::tests::a, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Error populating property 'string' on class 'meta::pure::functions::json::tests::a': \n" +
                "Expected value(s) of multiplicity [1], found 0 value(s).";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void oneAndMany()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::a\n" +
                "{\n" +
                "  string : String[1..*];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n\n" +
                "  let json='{\"string\": [\"foo\", \"bar\"]}'->fromJson(meta::pure::functions::json::tests::a, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void specifyingTheExactTypeGivenInTheModel()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  '{\"@type\": \"Foo\"}'->fromJson(meta::pure::functions::json::tests::Foo, ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void passingEnum()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Enum MyEnum\n" +
                "{\n" +
                "  Foo, Bar\n" +
                "}\n" +
                "Class meta::pure::functions::json::tests::Foo\n" +
                "{\n" +
                "  e : MyEnum[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  '{\"e\": \"Foo\"}'->fromJson(meta::pure::functions::json::tests::Foo, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void invalidTypeSpecified()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::json::test::Foo {}\n" +
                "Class meta::json::test::Bar {}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  '{\"@type\": \"Foo\"}'->fromJson(meta::json::test::Bar, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Could not find a sub-type of \"meta::json::test::Bar\" with name \"Foo\".";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void multipleObjectsString()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::json::test::Foo {}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  '[]'->fromJson(meta::json::test::Foo, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunc = "go():Any[*]";
        String expectedException = "Can only deserialize root-level JSONObjects i.e. serialized single instances of PURE classes. Cannot deserialize collections of multiple PURE objects.";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testLookup()
    {
        String sourceCode = "import meta::json::*;\n" +
                "Class meta::json::test::Foo\n" +
                "{\n" +
                "  name : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "  '{\"_type\":\"z\",\"name\":\"bla\"}'->fromJson(meta::json::test::Foo, ^meta::json::JSONDeserializationConfig(failOnUnknownProperties=true, typeKeyName='_type', typeLookup = [pair('z','meta::json::test::Foo')]));\n" +
                "}";
        String testFunc = "go():Any[*]";
        assertPassesExecution(sourceCode, testFunc);
    }

    @Test
    public void testDeserializeUnitInstanceAsClassProperty()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJson():Any[*]\n" +
                "{\n" +
                "   let res ='{\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";
        String testFunc = "testUnitToJson():Any[*]";
        CoreInstance result = assertPassesExecution(sourceCode, testFunc);
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne("genericType"), processorSupport));
        Assert.assertEquals("5.5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeUnitInstanceAsClassPropertyMany()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[*];\n" +
                "}\n" +
                "function testUnitToJson():Any[*]\n" +
                "{\n" +
                "   let res ='{\"myWeight\":[{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5},{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":1}]}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight->at(0);\n" +
                "}\n";
        String testFunc = "testUnitToJson():Any[*]";
        CoreInstance result = assertPassesExecution(sourceCode, testFunc);
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne("genericType"), processorSupport));
        Assert.assertEquals("5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeUnitInstanceAsSuperTypeProperty()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass[1];\n" +
                "}\n" +
                "function testUnitToJson():Any[*]\n" +
                "{\n" +
                "   let res ='{\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}}'\n->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";
        String testFunc = "testUnitToJson():Any[*]";
        CoreInstance result = assertPassesExecution(sourceCode, testFunc);
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne("genericType"), processorSupport));
        Assert.assertEquals("5.5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeClassWithOptionalUnitPropertyNotRemoved()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "   myOptionalWeight : Mass~Kilogram[0..1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5.5}, \"myOptionalWeight\":[]}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";
        String testFunc = "testUnitToJsonWithType():Any[*]";
        CoreInstance result = assertPassesExecution(sourceCode, testFunc);
        Assert.assertEquals("Mass~Kilogram", GenericType.print(result.getValueForMetaPropertyToOne("genericType"), processorSupport));
        Assert.assertEquals("5.5", result.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
    }

    @Test
    public void testDeserializeWrongUnitTypeInJsonThrowsError()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"badtype\",\"exponentValue\":1}],\"value\":5.5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";
        String testFunc = "testUnitToJsonWithType():Any[*]";
        String expectedException = "Error populating property 'myWeight' on class 'A': \n" +
                "Could not create new instance of Unit";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testDeserializeWrongUnitValueTypeInJsonThrowsError()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":\"5.5\"}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "   $res.myWeight;\n" +
                "}\n";
        String testFunc = "testUnitToJsonWithType():Any[*]";
        String expectedException = "Error populating property 'myWeight' on class 'A': \n" +
                "Value from unitValue field must be of Number type, getting java.lang.String type instead.";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testDeserializeNonOneExponentInJsonThrowsError()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":3}],\"value\":5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";
        String testFunc = "testUnitToJsonWithType():Any[*]";
        String expectedException = "Error populating property 'myWeight' on class 'A': \n" +
                "Currently non-one exponent for unit is not supported. Got: 3.";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    @Test
    public void testDeserializeCompositeJsonResultThrowsError()
    {
        String sourceCode = "import pkg::*;\n" +
                "Measure pkg::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x*1000;\n" +
                "   Pound: x -> $x*453.59;\n" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "   myWeight : Mass~Kilogram[1];\n" +
                "}\n" +
                "function testUnitToJsonWithType():Any[*]\n" +
                "{\n" +
                "   let res ='{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1},{\"unitId\":\"pkg::Mass~Gram\",\"exponentValue\":1}],\"value\":5}}'->meta::json::fromJson(A, ^meta::json::JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}\n";
        String testFunc = "testUnitToJsonWithType():Any[*]";
        String expectedException = "Error populating property 'myWeight' on class 'A': \n" +
                "Currently composite units are not supported.";
        assertFailsExecution(sourceCode, testFunc, expectedException);
    }

    private void runShouldFailTestCase(String testName, String expectedType, String actualJson, String expectedExceptionSnippet)
    {
        runShouldFailTestCase(testName, expectedType, actualJson, expectedExceptionSnippet, "");
    }

    /**
     * Ensure that assigning a json value of one type to a pure property of another, incompatible, type throws an exception.
     *
     * @param testName                 Unique name for the test case
     * @param expectedType             Expected PURE type (with multiplicity)
     * @param actualJson               Example of JSON type
     * @param expectedExceptionSnippet Expected-found snippet of exception e.g. "Expected Boolean, found String"
     */
    private void runShouldFailTestCase(String testName, String expectedType, String actualJson, String expectedExceptionSnippet, String additionalPureCode)
    {
        String sourceId = testName + RepositoryCodeStorage.PURE_FILE_EXTENSION;
        String sourceCode = "import meta::json::*;\nimport meta::pure::functions::json::tests::*;\n" +
                "\n" +
                additionalPureCode +
                "\n" +
                "Class meta::pure::functions::json::tests::" + testName + "\n" +
                "{\n" +
                "    testField : " + expectedType + ";\n" +
                "}\n" +
                "function " + testName + "():Any[*]\n" +
                "{\n" +
                "    let json = '{\"testField\": " + actualJson + "}';\n" +
                "    $json -> fromJson(" + testName + ", ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "}";
        String testFunction = testName + "():Any[*]";
        String exceptionDetails = "".equals(expectedExceptionSnippet) ? "" : ": \n" + expectedExceptionSnippet;
        String expectedExceptionMessage = "Error populating property 'testField' on class 'meta::pure::functions::json::tests::" + testName + "'" + exceptionDetails;
        assertFailsExecution(sourceId, sourceCode, testFunction, expectedExceptionMessage);
    }

    private void runShouldPassTestCase(String testName, String expectedType, String actualJson, String result)
    {
        runShouldPassTestCase(testName, expectedType, actualJson, "", result);
    }

    private void runShouldPassTestCase(String testName, String expectedType, String actualJson, String additionalPureCode, String result)
    {
        String sourceId = testName + RepositoryCodeStorage.PURE_FILE_EXTENSION;
        String sourceCode = "import meta::json::*;\nimport meta::pure::functions::json::tests::*;\n" +
                additionalPureCode +
                "Class meta::pure::functions::json::tests::" + testName + "\n" +
                "{\n" +
                "    testField : " + expectedType + ";\n" +
                "}\n" +
                "function " + testName + "():Any[*]\n" +
                "{\n" +
                "    let json = '{\"testField\": " + actualJson + "}';\n" +
                "    let jsonAsPure = $json -> fromJson(" + testName + ", ^JSONDeserializationConfig(typeKeyName='@type', failOnUnknownProperties=false));\n" +
                "    assertEquals(" + result + ", $jsonAsPure.testField, 'Output does match expected');\n" +
                "}";
        String testFunction = testName + "():Any[*]";
        assertPassesExecution(sourceId, sourceCode, testFunction);
    }

    private void assertException(PureException e, String expectedInfo)
    {
        // The cause of the exception should be considered the root level fromJson call so that it contains all of the information about the nested JSON structure.
        Assert.assertEquals(e.getInfo(), e.getOriginatingPureException().getInfo());
        Assert.assertEquals(expectedInfo, e.getInfo());
    }

    private void assertFailsExecution(String testSource, String testFunction, String expectedExceptionMessage)
    {
        assertFailsExecution("fromString.pure", testSource, testFunction, expectedExceptionMessage);
    }

    private void assertFailsExecution(String sourceId, String sourceCode, String testFunction, String expectedExceptionMessage)
    {
        compileTestSource(sourceId, sourceCode);
        try
        {
            PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute(testFunction));
            assertException(e, expectedExceptionMessage);
        }
        finally
        {
            runtime.delete(sourceId);
            runtime.compile();
        }
    }

    private CoreInstance assertPassesExecution(String sourceCode, String testFunction)
    {
        return assertPassesExecution("fromString.pure", sourceCode, testFunction);
    }

    private CoreInstance assertPassesExecution(String sourceId, String sourceCode, String testFunction)
    {
        compileTestSource(sourceId, sourceCode);
        try
        {
            return execute(testFunction);
        }
        finally
        {
            runtime.delete(sourceId);
            runtime.compile();
        }
    }
}
