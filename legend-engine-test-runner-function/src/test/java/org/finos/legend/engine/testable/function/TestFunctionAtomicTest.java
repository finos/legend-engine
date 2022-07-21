// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.function;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.testable.function.extension.FunctionTestRunner;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTest_Impl;
import org.junit.Assert;
import org.junit.Test;

public class TestFunctionAtomicTest
{
    @Test
    public void testFunctionStringPrimitiveValueSimpleExpression()
    {
        String grammar = "###Pure\n" +
                "function trial2::hello(name: String[1]): String[1]\n" +
                "<\n" +
                "test1: {[name = 'Sharvani'], 'Hello, Sharvani'}\n" +
                ">\n" +
                "{\n" +
                "   'Hello, ' + $name;\n" +
                "}\n";

        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionStringPrimitiveValueSimpleExpressionWithoutParameterName()
    {
        String grammar = "###Pure\n" +
                "function trial2::hello(name: String[1]): String[1]\n" +
                "<\n" +
                "test1: {['Sharvani'], 'Hello, Sharvani'}\n" +
                ">\n" +
                "{\n" +
                "   'Hello, ' + $name;\n" +
                "}\n";

        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    //TODO: Fix the issue

//    @Test
//    public void testFunctionStringPrimitiveValueLetFunction()
//    {
//        String grammar = "###Pure\n" +
//                "function trial2::hello(name: String[1]): String[1]\n" +
//                "<\n" +
//                "test1: {[name = 'Sharvani'], 'Hello, Sharvani'}\n" +
//                ">\n" +
//                "{\n" +
//                "   let result = 'Hello, ' + $name;\n" +
//                "   $result;\n" +
//                "}\n";
//
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    @Test
    public void testFunctionTestZeroParameter()
    {
        String grammar = "###Pure\n" +
                "function trial2::hello(): String[1]\n" +
                "<\n" +
                "test1: {[], 'word'}\n" +
                ">\n" +
                "{\n" +
                "   let result = 'work';\n" +
                "   $result->replace('k', 'd');\n" +
                "}\n";

        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestIntegerParameterIntegerAssert()
    {
        String grammar = "###Pure\n" +
                "function trial2::add(var1: Integer[1], var2: Integer[1]): Integer[1]\n" +
                "<\n" +
                "test1: {[2, 5], 7}\n" +
                ">\n" +
                "{\n" +
                "   $var1 + $var2;\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestIntegerParameterIntegerAssertWithSign()
    {
        String grammar = "###Pure\n" +
                "function trial2::add(var1: Integer[1], var2: Integer[1]): Integer[1]\n" +
                "<\n" +
                "test1: {[-2, +5], +3}\n" +
                ">\n" +
                "{\n" +
                "   $var1 + $var2;\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    //TODO: Fix the issue

//    @Test
//    public void testFunctionTestIntegerParameterVariableExpression()
//    {
//        String grammar = "###Pure\n" +
//                "function trial2::passNumber(var1: Integer[1]): Integer[1]\n" +
//                "<\n" +
//                "test1: {[7], 7}\n" +
//                ">\n" +
//                "{\n" +
//                "   $var1;\n" +
//                "}\n";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    @Test
    public void testFunctionStringParameterIntegerAssert()
    {
        String grammar = "###Pure\n" +
                "function trial2::parseInt(var1: String[1]): Integer[1]\n" +
                "<\n" +
                "test1: {['7'], 7}\n" +
                ">\n" +
                "{\n" +
                "   $var1->parseInteger();\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionFloatParameterIntegerAssert()
    {
        String grammar = "###Pure\n" +
                "function trial2::roundFloat(var1: Float[1]): Integer[1]\n" +
                "<\n" +
                "test1: {[17.6], 18}\n" +
                ">\n" +
                "{\n" +
                "   round($var1);\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionMultipleTests()
    {
        String grammar = "###Pure\n" +
                "function trial2::roundFloat(var1: Float[1]): Integer[1]\n" +
                "<\n" +
                "test1: {[17.6], 18},\n" +
                "test2: {[17.1], 17}\n" +
                ">\n" +
                "{\n" +
                "   round($var1);\n" +
                "}\n";
        TestResult functionTestTestResult1 = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult1 instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult1).atomicTestId.atomicTestId);

        TestResult functionTestTestResult2 = TestFunctionAtomicTestHelper.runTest(grammar, "test2");
        Assert.assertTrue(functionTestTestResult2 instanceof TestPassed);
        Assert.assertEquals("test2", ((TestPassed) functionTestTestResult2).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestFailed()
    {
        String grammar = "###Pure\n" +
                "function trial2::roundFloat(var1: Float[1]): Integer[1]\n" +
                "<\n" +
                "test1: {[17.6], 10}\n" +
                ">\n" +
                "{\n" +
                "   round($var1);\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestFailed);
        Assert.assertEquals("test1", ((TestFailed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestListParameterOptionalAssert()
    {
        String grammar = "###Pure\n" +
                "function trial2::ListParameter(var1: Integer[*]): Integer[0..1]\n" +
                "<\n" +
                "test1: {[[17,20,7]], 20}\n" +
                ">\n" +
                "{\n" +
                "   $var1->max();\n" +
                "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    // TODO: fix the issue

//    @Test
//    public void testFunctionTestEmptyListParameterEmptyOptionalAssert()
//    {
//        String grammar =
//        "function trial3::ListParameter(var1: Integer[*]): Integer[0..1]\n" +
//                "<\n" +
//                "test1: {[ [] ], []}\n" +
//                ">\n" +
//                "{\n" +
//                "  if ($var1->isEmpty(),\n" +
//                "  | [],\n" +
//                "  | let result = $var1->max()\n" +
//                "  )\n" +
//                "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    // TODO: fix the issue

//    @Test
//    public void testFunctionTestEmptyOptionalParameterEmptyOptionalAssert()
//    {
//        String grammar =
//                "function trial3::ListParameter(var1: Integer[0..1]): Integer[0..1]\n" +
//                        "<\n" +
//                        "test1: {[ [] ], []}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "  if ($var1->isEmpty(),\n" +
//                        "  | [],\n" +
//                        "  | let result = $var1\n" +
//                        "  )\n" +
//                        "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    //TODO: Fix the issue

//    @Test
//    public void testFunctionTestMismatchingNumberOfElementsDefinitionAssert()
//    {
//        String grammar =
//                "function trial3::ListParameter(var1: Integer[*]): Integer[2]\n" +
//                        "<\n" +
//                        "test1: {[ [1,2] ], [1,2,3]}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "  | let result = $var1\n" +
//                        "  )\n" +
//                        "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestError);
//        Assert.assertEquals("test1", ((TestError) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    //TODO: Fix the issue

//    @Test
//    public void testFunctionTestMismatchingNumberOfElementsListAssert()
//    {
//        String grammar =
//                "function trial3::ListParameter(var1: Integer[*]): Integer[*]\n" +
//                        "<\n" +
//                        "test1: {[ [1,2] ], [1]}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "  if ($var1->isEmpty(),\n" +
//                        "  | [],\n" +
//                        "  | let result = $var1\n" +
//                        "  )\n" +
//                        "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestFailed);
//        Assert.assertEquals("test1", ((TestFailed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    @Test
    public void testFunctionTestDateParameterBooleanAssert()
    {
        String grammar =
                "function trial::DateParameter(date: StrictDate[1]):Boolean[1]\n" +
                        "<\n" +
                        "    test1: {[%2020-1-1], true}\n" +
                        ">\n" +
                        "{\n" +
                        "    if (meta::pure::functions::date::hasDay($date), \n" +
                        "          | let result = true,\n" +
                        "          | let result = false\n" +
                        "         )\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    // TODO: Fix the issue (duplicate var in java generation)

//    @Test
//    public void testFunctionTestDateParameterStrictDateAssert()
//    {
//        String grammar =
//                "function trial::DateParameter(date: StrictDate[1]):StrictDate[1]\n" +
//                        "<\n" +
//                        "    test1: {[%2020-01-01], %2020-01-01}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "    if (meta::pure::functions::date::hasDay($date), \n" +
//                        "          | $date,\n" +
//                        "          | $date\n" +
//                        "         )\n" +
//                        "}\n";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    // TODO: issue with casting: 2020-01 would be of type yearmonth than date time in java generation

//    @Test
//    public void testFunctionTestDateParameterDateAssert()
//    {
//        String grammar =
//                "function trial::DateParameter(date: Date[1]):Date[1]\n" +
//                        "<\n" +
//                        "    test1: {[%2020-01], %2020-01}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "    if (meta::pure::functions::date::hasDay($date), \n" +
//                        "          | $date,\n" +
//                        "          | $date\n" +
//                        "         )\n" +
//                        "}\n";
//
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    //TODO: fix the issue
    //org.finos.legend.engine.shared.core.operational.errorManagement.EngineException: Error in 'trial3::getDate': class org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth cannot be cast to class org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime (org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth and org.finos.legend.pure.m4

//    @Test
//    public void testFunctionTestIntegerParameterDateAssert()
//    {
//        String grammar =
//                "function trial3::getDate(var1: Integer[1], var2: Integer[1]):Date[1]\n" +
//                        "<\n" +
//                        "  test1: {[2001,2], %2001-02}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "  let result =  meta::pure::functions::date::date($var1, $var2);\n" +
//                        "  if (meta::pure::functions::date::hasDay($result), \n" +
//                        "  | $result,\n" +
//                        "  | $result\n" +
//                        "  );\n" +
//                        "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    //TODO: Fix the issue

//    @Test
//    public void testFunctionTestEmptyListParameterEmptyListAssert()
//    {
//        String grammar = "function trial::ListParameter(var1: Integer[*]): Integer[*]\n" +
//                " <\n" +
//                " test1: {[ [] ], []}\n" +
//                ">\n" +
//                "{\n" +
//                "  if ($var1->isEmpty(), \n" +
//                "  | [],\n" +
//                "  | $var1\n" +
//                "  )\n" +
//                "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    // TODO: Fix the issue

//    @Test
//    public void testFunctionTestOptionalParameterOptionalAssert()
//    {
//        String grammar =
//                "function trial3::OptionalParameter(var1: Integer[0..1]): Integer[0..1]\n" +
//                        "<\n" +
//                        "test1: {[ 1 ], 2}\n" +
//                        ">\n" +
//                        "{\n" +
//                        "  if ($var1->isEmpty(),\n" +
//                        "  | [],\n" +
//                        "  | $var1->toOne() + 1\n" +
//                        "  )\n" +
//                        "}\n";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    // TODO: fix "Cast exception: VariableExpression cannot be cast to FunctionExpression"

//    @Test
//    public void testFunctionTestVariableExpressionEmptyList()
//    {
//
//        String grammar = "function trial::ListParameter(var1: Integer[*]): Integer[*]\n" +
//                " <\n" +
//                " test1: {[ [] ], []}\n" +
//                " >\n" +
//                "{\n" +
//                "  $var1\n" +
//                "}";
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

    @Test
    public void testFunctionTestFloatParameterFloatAssertWithSign()
    {
        String grammar =
                "function trial2::FloatToFloat(var1: Float[1]): Float[1]\n" +
                        "<\n" +
                        "   test1: {[+2.0], -3.0}\n" +
                        ">\n" +
                        "{\n" +
                        "   $var1 - 5.0\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestFloatParameterFloatAssert()
    {
        String grammar =
                "function trial2::FloatToFloat(var1: Float[1]): Float[1]\n" +
                        "<\n" +
                        "   test1: {[2.0], 3.0}\n" +
                        ">\n" +
                        "{\n" +
                        "   $var1 + 1.0\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestDecimalParameterDecimalAssert()
    {
        String grammar =
                "function trial2::DecimalToDecimal(var1: Decimal[1]): Decimal[1]\n" +
                        "<\n" +
                        "   test1: {[99911111111111111199.322222222222222222222d], 99911111111111111199.322222222222222222222d}\n" +
                        ">\n" +
                        "{\n" +
                        " $var1->toDecimal();\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestDecimalParameterDecimalAssertWithSign()
    {
        String grammar =
                "function trial2::DecimalToDecimal(var1: Decimal[1]): Decimal[1]\n" +
                        "<\n" +
                        "   test1: {[-2.2d], +1.1d}\n" +
                        ">\n" +
                        "{\n" +
                        " $var1 + 3.3d;\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    @Test
    public void testFunctionTestDecimalParameterDecimalAssertDecimalInFunctionBody()
    {
        String grammar =
                "function trial2::DecimalToDecimal(var1: Decimal[1]): Decimal[1]\n" +
                        "<\n" +
                        "   test1: {[1.1d], 1.2d}\n" +
                        ">\n" +
                        "{\n" +
                        "   $var1 + 0.1d\n" +
                        "}\n";
        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
    }

    // TODO: Fix the issue (duplicate var in java generation)

//    @Test
//    public void testFunctionTestDateTimeParameterAndAssert()
//    {
//        String grammar =
//        "function trial3::DateTimeParameterMinimum(var1: DateTime[1], var2: DateTime[1]): DateTime[1]\n" +
//                "<\n" +
//                "test1: {[ %2015-09-10T20:10:20, %2015-09-10T20:10:20 ], %2015-09-10T20:10:20}\n" +
//                ">\n" +
//                "{\n" +
//                "  if($var1 < $var2, | $var2, | $var1)\n" +
//                "}";
//
//        TestResult functionTestTestResult = TestFunctionAtomicTestHelper.runTest(grammar, "test1");
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionComplexValueAssertPrimitiveValueParameterSimple()
//    {
//        String grammar1 = "Class my::Firm\n" +
//                "{\n" +
//                "firmName: String[1];\n" +
//                "}\n" +
//                "function trial::createFirm(name: String[1]): my::Firm[1]\n" +
//                "<\n" +
//                "   test1: { [name = 'GS'], #{ { \"firmName\": \"GS\" } }# }\n" +
//                ">\n" +
//                "{\n" +
//                "\n" +
//                "^my::Firm(firmName=$name)\n" +
//                "\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar1);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionComplexValueAssertPrimitiveValueParameterIfElse()
//    {
//        String grammar1 =
//                "Class my::Firm\n" +
//                "{\n" +
//                "firmName: String[1];\n" +
//                "}\n" +
//                "function trial4::createFirm(name: String[1]): my::Firm[1]\n" +
//                " <\n" +
//                "    test1: { [name = 'GS'], #{ { \"firmName\": \"GS\" } }# }\n" +
//                " >\n" +
//                "{\n" +
//                "  if($name == 'GS',\n" +
//                "  | ^my::Firm(firmName=$name),\n" +
//                "  | ^my::Firm(firmName='')\n" +
//                "  );\n" +
//                "\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar1);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionComplexValueAssertPrimitiveValueParameterMultipleFunctionElements()
//    {
//        String grammar1 =
//                "Class my::Firm\n" +
//                "{\n" +
//                "firmName: String[1];\n" +
//                "}\n" +
//                "function trial::createFirm(name: String[1]): my::Firm[1]\n" +
//                "<\n" +
//                "   test1: { [name = 'GS'], #{ { \"firmName\": \"GS\" } }# }\n" +
//                ">\n" +
//                "{\n" +
//                "\n" +
//                "^my::Firm(firmName=$name)\n" +
//                "\n" +
//                "}\n" +
//                "function trial4::createFirmCaseBasis(name: String[1]): my::Firm[1]\n" +
//                " <\n" +
//                "    test1: { [name = 'GS'], #{ { \"firmName\": \"GS\"} }# }\n" +
//                " >\n" +
//                "{\n" +
//                "  if($name == 'GS',\n" +
//                "  | let result = ^my::Firm(firmName=$name),\n" +
//                "  | let result = ^my::Firm(firmName='')\n" +
//                "  );\n" +
//                "\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar1);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionPrimitiveValueAssertComplexValueParameter()
//    {
//        String grammar = "###Pure\n" +
//                "Class my::Firm\n" +
//                "{\n" +
//                "version: Integer[1];\n" +
//                "}\n" +
//                "function trial4::createFirm(data: my::Firm[1]): Integer[1]\n" +
//                " <\n" +
//                "    test1: { [ #{ { \"version\": 2 } }# ], 3}\n" +
//                " >\n" +
//                "{\n" +
//                "\n" +
//                "$data.version->toOne() + 1;\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionComplexValueAssertComplexValueParameterSimple()
//    {
//        String grammar = "###Pure\n" +
//                "Class my::Firm\n" +
//                "{\n" +
//                "version: Integer[1];\n" +
//                "}\n" +
//                "function trial4::createFirm(data: my::Firm[1]): my::Firm[1]\n" +
//                " <\n" +
//                "    test1: { [ #{ { \"version\": 2 } }# ], #{ { \"version\": 2 } }# }\n" +
//                " >\n" +
//                "{\n" +
//                "$data\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//    }

// TODO: uncomment once Complex Value tests are supported

//    @Test
//    public void testFunctionComplexValueAssertComplexValueParameter()
//    {
//        String grammar = "###Pure\n" +
//                "Class my::Firm2\n" +
//                "{\n" +
//                "version: Integer[1];\n" +
//                "}\n" +
//                "function trial4::createFirm2(data: my::Firm2[1]):my::Firm2[1]\n" +
//                "<\n" +
//                "    test1: { [ #{ { \"version\": 2 } }# ],  #{ { \"version\": 2 } }#}\n" +
//                ">\n" +
//                "{\n" +
//                "if (1 == 2,\n" +
//                "| ^my::Firm2(version = 1),\n" +
//                "| ^my::Firm2(version = 2))\n" +
//                "}";
//
//        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);
//        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
//
//        Root_meta_legend_function_metamodel_FunctionTest atomicTest = new Root_meta_legend_function_metamodel_FunctionTest_Impl("")._id("test1");
//
//        Function function = ListIterate.detect(modelData.getElementsOfType(Function.class), ele -> ele.tests != null);
//        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> pureFunction = FunctionTestRunner.findPureFunction(function, pureModel);
//        org.finos.legend.engine.testable.function.extension.FunctionTestRunner functionTestRunner = new org.finos.legend.engine.testable.function.extension.FunctionTestRunner(pureFunction, "vX_X_X");
//        TestResult functionTestTestResult = functionTestRunner.executeAtomicTest(atomicTest, pureModel, modelData);
//
//        Assert.assertTrue(functionTestTestResult instanceof TestPassed);
//        Assert.assertEquals("test1", ((TestPassed) functionTestTestResult).atomicTestId.atomicTestId);
//
//    }

}
