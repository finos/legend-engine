// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.memsqlFunction.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MemSqlFunctionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestMemSqlParsing extends TestGrammarParser.TestGrammarParserTestSuite
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
                "   function : a::f():String[1];" +
                "   functionName : 'sass';\n" +
                "}\n";
    }

    @Test
    public void testGetParserErrorWrongProperty()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   functioName : 'sass';\n" +
                "}\n", "PARSER error at [4:4-14]: Unexpected token 'functioName'. Valid alternatives: ['functionName', 'description', 'function', 'owner', 'activationConfiguration']");
    }

    @Test
    public void testGetParserErrorMissingApplicationName()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   owner : 'pierre';\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'functionName' is required");
    }

    @Test
    public void testGetParserErrorMissingFunction()
    {
        test("###MemSql\n" +
                "MemSqlFunction x::A\n" +
                "{\n" +
                "   functionName : 'MyApp';\n" +
                "   owner : 'pierre';\n" +
                "}\n", "PARSER error at [2:1-6:1]: Field 'function' is required");
    }
}
