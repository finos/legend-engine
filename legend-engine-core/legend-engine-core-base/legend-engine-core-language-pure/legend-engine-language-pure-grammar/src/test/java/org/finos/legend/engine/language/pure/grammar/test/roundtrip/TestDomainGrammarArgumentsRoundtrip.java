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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDomainGrammarArgumentsRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFunctionWithTypeArguments()
    {
        test("function withPath::f(s: Result<String>[1]): Result<String>[0..1]\n" +
                "{\n" +
                "  []\n" +
                "}\n");
    }

    @Test
    public void testClassPropertiesWithTypeArguments()
    {
        test("Class my::Class\n" +
                "{\n" +
                "  prop1: Result<String>[1];\n" +
                "}\n");
    }

    @Test
    public void testCast()
    {
        test("function withPath::f(s: a::Result<x::String>[1]): Relation<(a:Integer)>[0..1]\n" +
                "{\n" +
                "  []->cast(@Relation<(a:Integer)>)\n" +
                "}\n");
    }

    @Test
    public void testCastWithQuotedIdentifiers()
    {
        test("function withPath::f(s: a::Result<x::String>[1]): Relation<('2000__a':'2000 Integer')>[0..1]\n" +
                "{\n" +
                "  []->cast(@Relation<('2000__a':'2000 Integer')>)\n" +
                "}\n");
    }

    @Test
    public void testFunc()
    {
        test("function withPath::f(s: a::Type[1]): String[0..1]\n" +
                "{\n" +
                "  []\n" +
                "}\n");
    }
}
