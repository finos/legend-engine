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

public class TestDomainTypeVariablesRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFunctionWithTypeHavingTypeVariableValues()
    {
        test("function withPath::f(s: Res(1)[1]): Res(1,'a')[0..1]\n" +
                "{\n" +
                "  []\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithTypeHavingTypeVariableValuesAndGenerics()
    {
        test("function withPath::f(s: Res<String>(1)[1]): Res<Integer>(1,'a')[0..1]\n" +
                "{\n" +
                "  []\n" +
                "}\n");
    }

    @Test
    public void testClassWithPropertyHavingTypeVariableValues()
    {
        test("Class A\n" +
                "{\n" +
                "  name: VARCHAR(200)[1];\n" +
                "}\n");
    }

    @Test
    public void testRelationWithPropertyHavingTypeVariableValues()
    {
        test("function t::f(): X<(a:Integer(200), z:V('ok'))>[0..1]\n" +
                "{\n" +
                "  []\n" +
                "}\n");
    }
}
