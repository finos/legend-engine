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
    // TO FIX ------
    // We don't want to authorize typeParameters but typeArguments for system's parameterized types should be allowed
    // Unfortunately the parser is considering Result<String> as a type and not a GenericType with rawType=Result and typeArguments = GenericType(rawType=String)
    // Which means the compiler can't work... We need to parse the GenericType and 'slot' it in the Protocol.
    // -------------

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
}
