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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip.valueSpecification;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPrimitives extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testStringOne()
    {
        test("function a::a(): String[1]\n" +
                "{\n" +
                "   'ok'\n" +
                "}\n"
        );
    }

    @Test
    public void testStringMany()
    {
        test("function a::a(): String[*]\n" +
                "{\n" +
                "   ['ok', 'bla']\n" +
                "}\n"
        );
    }

    @Test
    public void testIntegerOne()
    {
        test("function a::a(): String[1]\n" +
                "{\n" +
                "   1\n" +
                "}\n"
        );
    }

    @Test
    public void testIntegerMany()
    {
        test("function a::a(): String[*]\n" +
                "{\n" +
                "   [1, 2]\n" +
                "}\n"
        );
    }

    @Test
    public void testBooleanOne()
    {
        test("function a::a(): String[*]\n" +
                "{\n" +
                "   true\n" +
                "}\n"
        );
    }

    @Test
    public void testBooleanMany()
    {
        test("function a::a(): String[*]\n" +
                "{\n" +
                "   [true, false, true]\n" +
                "}\n"
        );
    }

    @Test
    public void testMixedMany()
    {
        test("function a::a(): String[*]\n" +
                "{\n" +
                "   [1, 'a', true]\n" +
                "}\n"
        );
    }

}
