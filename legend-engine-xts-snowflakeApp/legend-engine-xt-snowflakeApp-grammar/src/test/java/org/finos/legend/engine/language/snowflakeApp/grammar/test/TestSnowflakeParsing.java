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

package org.finos.legend.engine.language.snowflakeApp.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.SnowflakeAppParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestSnowflakeParsing extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return SnowflakeAppParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Snowflake\n" +
                "SnowflakeApp " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "   ownership : Deployment{identifier:'testDeployment'};" +
                "   applicationName : 'sass';\n" +
                "}\n";
    }

    @Test
    public void testGetParserErrorWrongProperty()
    {
        test("###Snowflake\n" +
                "SnowflakeApp x::A\n" +
                "{\n" +
                "   applicatioName : 'sass';\n" +
                "}\n", "PARSER error at [4:4-17]: Unexpected token 'applicatioName'. Valid alternatives: ['applicationName', 'description', 'function', 'ownership', 'activationConfiguration']");
    }

    @Test
    public void testGetParserErrorMissingApplicationName()
    {
        test("###Snowflake\n" +
                "SnowflakeApp x::A\n" +
                "{\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'applicationName' is required");
    }

    @Test
    public void testGetParserErrorMissingFunction()
    {
        test("###Snowflake\n" +
                "SnowflakeApp x::A\n" +
                "{\n" +
                "   applicationName : 'MyApp';\n" +
                "   ownership : Deployment { identifier: 'pierre'};\n" +
                "}\n", "PARSER error at [2:1-6:1]: Field 'function' is required");
    }

}