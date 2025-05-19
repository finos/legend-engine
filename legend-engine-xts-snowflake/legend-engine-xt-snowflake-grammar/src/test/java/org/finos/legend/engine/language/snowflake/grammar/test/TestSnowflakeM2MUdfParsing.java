//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.snowflake.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.SnowflakeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestSnowflakeM2MUdfParsing extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return SnowflakeParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Snowflake\n" +
                "SnowflakeM2MUdf " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   ownership : Deployment{identifier:'testDeployment'};" +
                "   udfName : 'sass';\n" +
                "   deploymentSchema : 'legend_native_apps_1';\n" +
                "   deploymentStage : 'snowflakeStage';\n" +
                "}\n";
    }

    @Test
    public void testGetParserErrorWrongProperty()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   applicatioName : 'sass';\n" +
                "}\n", "PARSER error at [4:4-17]: Unexpected token 'applicatioName'. Valid alternatives: ['udfName', 'description', 'function', 'ownership', 'activationConfiguration', 'permissionScheme', 'usageRole', 'deploymentSchema', 'deploymentStage']");
    }

    @Test
    public void testGetParserErrorMissingApplicationName()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'udfName' is required");
    }

    @Test
    public void testGetParserErrorMissingOwnership()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   udfName : 'sass';\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'ownership' is required");
    }

    @Test
    public void testGetParserErrorMissingFunction()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   udfName : 'MyApp';\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "}\n", "PARSER error at [2:1-6:1]: Field 'function' is required");
    }

    @Test
    public void testGetParserErrorWrongScheme()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   udfName : 'MyApp';\n" +
                "   permissionScheme : WHATSCHEME;\n" +
                "   deploymentSchema : 'legend_native_schema_1';\n" +
                "   deploymentStage : 'snowflakeStage';\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "}\n", "PARSER error at [5:4-33]: Unknown permission scheme 'WHATSCHEME'");
    }

    @Test
    public void testGetParserErrorMissingDeploymentSchema()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   ownership : Deployment{identifier:'testDeployment'};" +
                "   udfName : 'sass';\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'deploymentSchema' is required");
    }

    @Test
    public void testGetParserErrorMissingDeploymentStage()
    {
        test("###Snowflake\n" +
                "SnowflakeM2MUdf x::A\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   ownership : Deployment{identifier:'testDeployment'};" +
                "   udfName : 'sass';\n" +
                "   deploymentSchema : 'legend_native_schema_1';\n" +
                "}\n", "PARSER error at [2:1-6:1]: Field 'deploymentStage' is required");
    }


}
