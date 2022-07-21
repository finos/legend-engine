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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.antlr.v4.runtime.Vocabulary;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestFunctionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DomainParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return null;
    }

    @Test
    public void testFunctionTestSimplePrimitiveValueParameterWithParamName()
    {
        test(
                    "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n" +
                        "###Pure\n" +
                        "function trial2::hello(name: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[name = 'Sharvani'],'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = 'Hello, ' + $name;\n" +
                        "    $result;\n" +
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
                        "}\n" +
                        "###Pure\n" +
                        "function trial2::hello(name: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {['Sharvani'],'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = 'Hello, ' + $name;\n" +
                        "    $result;\n" +
                        "}\n");
    }

    @Test
    public void testFunctionTestBasicComplexValueAssert()
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
    public void testFunctionTestBasicComplexValueParameterAndAssert()
    {
        test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(firm: my::Firm[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[#{{\"firmName\": \"\"}}#], #{{\"firmName\": \"GS\"}}#}\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$name)\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestParameterInvalidFormatMissingIdentifier()
    {
        test("###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {['Alice', place = 'Wonderland'],'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "PARSER error at [4:5-66]: Expected Identifier");
    }

    @Test
    public void testFunctionTestParameterInvalidFormat()
    {
        test("###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {'Alice', 'Wonderland','Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "PARSER error at [4:13-19]: Unexpected token");
    }

    @Test
    public void testFunctionTestMissingAssert()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    test1: {['Alice', 'Wonderland']}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:36]: Unexpected token '}'");
    }

    @Test
    public void testFunctionTestMultipleAsserts()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    test1: {['Alice', 'Wonderland'], 'Alice in Wonderland', 'Alice in Wonderland'}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:59]: Unexpected token");
    }

    @Test
    public void testFunctionTestMissingTestId()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "{['Alice', 'Wonderland'], 'Alice in Wonderland'}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:1]: Unexpected token '{'");
    }

    @Test
    public void testFunctionTestMultipleTests()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "test1 : {['Alice', 'Wonderland'], 'Alice in Wonderland'},\n" +
                "test2 : {['Oxygen', 'Space'], 'Oxygen in Space'}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n");
    }

    @Test
    public void testFunctionTestMultipleTestsWithSeparateFunctionTestBracket()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "test1 : {['Alice', 'Wonderland'], 'Alice in Wonderland'}\n" +
                ">\n" +
                "<\n" +
                "test2 : {['Oxygen', 'Space'], 'Oxygen in Space'}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [6:1]: Unexpected token '<'");
    }

    @Test
    public void testFunctionTestMissingFunctionBody()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "test1 : {['Alice', 'Wonderland'], 'Alice in Wonderland'}\n" +
                ">\n", "PARSER error at [6:1-5]: Unexpected token '<EOF>'");
    }

    @Test
    public void testFunctionTestMissingTestInsideTestsBody()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [3:1-4:1]: Atleast 1 function test needs to be present.");
    }

    @Test
    public void testFunctionTestNoTest()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n");
    }

    @Test
    public void testFunctionTestUnsupportedAssert()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    test1: {['Alice', 'Wonderland'],('Alice in Wonderland')}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:37]: Unexpected token");
    }

    @Test
    public void testFunctionTestParameterUnsupportedParameter()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    test1: {['Alice', { Wonderland }],'Alice in Wonderland'}\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:23]: Unexpected token");
    }

    @Test
    public void testFunctionTestMissingBraces()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    test1: ['Alice', 'Wonderland'],'Alice in Wonderland'\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:12]: Unexpected token");
    }

    @Test
    public void testFunctionTestInvalidBracket()
    {
        test("###Pure\n" +
                "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                "<\n" +
                "    [ test1: ['Alice', 'Wonderland'],'Alice in Wonderland' ]\n" +
                ">\n" +
                "{\n" +
                "    let result = $name + ' in ' + $place;\n" +
                "    $result;\n" +
                "}\n", "PARSER error at [4:5]: Unexpected token");
    }

    @Test
    public void testFunctionTestWithAllFunctionSubSections()
    {
        test("###Pure\n" +
                "import meta::pure::executionPlan::*;\n" +
                "Class my::Firm\n" +
                "{\n" +
                "  firmName: String[1];\n" +
                "}\n" +
                "function <<trail2.test>> {trial2.doc = 'Helper for creating Firm'} trial2::createFirm(firm: my::Firm[1]): my::Firm[1]\n" +
                "[\n" +
                "   containsConstraint: $myFirm.firmName->contains('GS')\n" +
                "]\n" +
                "<\n" +
                "   test1: {[#{{ \"firmName\": \"\"}}#], #{{\"firmName\": \"GS\"}}#}\n" +
                ">\n" +
                "{\n" +
                "   ^my::Firm(firmName=$name)\n" +
                "}\n");
    }

}