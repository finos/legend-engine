// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.sql.expression.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestSQLExpression extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSQLExpression()
    {
        test("function pack::f(): String[1]\n" +
                "{\n" +
                "  #SQL{select a,b from csv('a,b\\n1,2')}#;\n" +
                "  '';\n" +
                "}\n");
    }
}
