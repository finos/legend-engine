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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.junit.Assert;
import org.junit.Test;

public class TestFunctionGrammarRoundTrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFunctionTestSimplePrimitiveValueParameterWithParamName()
    {
        test(
                  "###Pure\n" +
                        "function trial2::hello(name: String[1]): String[1]\n" +
                        "<\n" +
                        "   test1: {[name = 'Sharvani'], 'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "   let result = 'Hello, ' + $name;\n" +
                        "   $result;\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestSimplePrimitiveValueParameterWithoutParamName()
    {
        test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n\n" +
                        "###Pure\n" +
                        "function trial2::hello(name: String[1]): String[1]\n" +
                        "<\n" +
                        "   test1: {['Sharvani'], 'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "   let result = 'Hello, ' + $name;\n" +
                        "   $result;\n" +
                        "}\n");
    }

    @Test
    public void testFunctionTestBasicComplexValueParameter()
    {
        test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(name: String[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[name = 'GS'], #{{\"firmName\": \"GS\"}}#}\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$name)\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestZeroParameter()
    {
        test(
                "###Pure\n" +
                        "function trial2::hello(): String[1]\n" +
                        "<\n" +
                        "   test1: {[], 'word'}\n" +
                        ">\n" +
                        "{\n" +
                        "   let result = 'work';\n" +
                        "   $result->replace('k', 'd');\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestIntegerParameterIntegerAssertWithSign()
    {
        test(
                "###Pure\n" +
                        "function trial2::add(var1: Integer[1], var2: Integer[1]): Integer[1]\n" +
                        "<\n" +
                        "   test1: {[-2, 5], 3}\n" +
                        ">\n" +
                        "{\n" +
                        "   $var1 + $var2\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestEmptyOptionalParameterEmptyOptionalAssert()
    {
        test(
                "function trial3::ListParameter(var1: Integer[0..1]): Integer[0..1]\n" +
                        "<\n" +
                        "   test1: {[[]], []}\n" +
                        ">\n" +
                        "{\n" +
                        "   if($var1->isEmpty(), |[], |let result = $var1)\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestDateParameterBooleanAssert()
    {
        test(
                "function trial::DateParameter(date: StrictDate[1]): Boolean[1]\n" +
                        "<\n" +
                        "   test1: {[%2020-1-1], true}\n" +
                        ">\n" +
                        "{\n" +
                        "   if($date->meta::pure::functions::date::hasDay(), |let result = true, |let result = false)\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestDecimalParameterDecimalAssert()
    {
        test(
                "function trial2::DecimalToDecimal(var1: Decimal[1]): Decimal[1]\n" +
                        "<\n" +
                        "   test1: {[99911111111111111199.322222222222222222222d], 99911111111111111199.322222222222222222222d}\n" +
                        ">\n" +
                        "{\n" +
                        "   $var1->toDecimal()\n" +
                        "}\n");

    }

    @Test
    public void testFunctionTestDateTimeParameterAndAssert()
    {
        test(
                "function trial3::DateTimeParameterMinimum(var1: DateTime[1], var2: DateTime[1]): DateTime[1]\n" +
                        "<\n" +
                        "   test1: {[%2015-09-10T20:10:20, %2015-09-10T20:10:20], %2015-09-10T20:10:20}\n" +
                        ">\n" +
                        "{\n" +
                        "   if($var1 < $var2, |$var2, |$var1)\n" +
                        "}\n");

    }

    @Test
    public void testFunctionComplexValueAssertComplexValueParameterSimple()
    {
        test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  version: Integer[1];\n" +
                        "}\n\n" +
                        "function trial4::createFirm(data: my::Firm[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[#{ { \"version\": 2 } }#], #{ { \"version\": 2 } }#}\n" +
                        ">\n" +
                        "{\n" +
                        "   $data\n" +
                        "}\n");

    }

}
