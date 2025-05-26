//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.memSqlFunction.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MemSqlFunctionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestMemSqlFunctionParsing extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MemSqlFunctionParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###MemSql\n" +
                "MemSqlFunction " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "   functionName : 'demoFunction';\n" +
                "   function : a::f():String[1];" +
                "}\n";
    }

    @Test
    public void testGetParserErrorWrongProperty()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   funcName : 'demoFunction';\n" +
                "}\n", "PARSER error at [4:4-11]: Unexpected token 'funcName'. Valid alternatives: ['functionName', 'description', 'function', 'owner', 'activationConfiguration']");
    }

    @Test
    public void testGetParserErrorMissingFunctionName()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   function : a::f():String[1];" +
                "}\n", "PARSER error at [2:1-4:32]: Field 'functionName' is required");
    }

    @Test
    public void testGetParserErrorMissingFunction()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   functionName : 'demoFunction';\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'function' is required");
    }
}
