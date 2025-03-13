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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.meta;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestInstanceOf extends PureExpressionTest
{
    @Test
    public void testWithCompileValueSpecification()
    {
        compileTestSource("fromString.pure","Enum test::Enum1 { VALUE1, VALUE2 }\n" +
                "Enum test::Enum2 { VALUE3, VALUE4 }\n" +
                "Class test::MyClass {}\n" +
                "function test::compileAndEval(val : String[1]):Boolean[1]\n" +
                "{\n" +
                "    $val->compileValueSpecification().result->toOne()->reactivate()->cast(@Function<{->Boolean[1]}>)->toOne()->eval();\n" +
                "}\n");

        assertExpressionTrue("'{| \\'myStr\\'->instanceOf(String)}'->test::compileAndEval()");
        assertExpressionTrue("'{| instanceOf((\\'myStr\\' + \\'other\\'), String)}'->test::compileAndEval()");
        assertExpressionFalse("'{| \\'myStr\\'->instanceOf(test::Enum2)}'->test::compileAndEval()");
        assertExpressionFalse("'{| \\'myStr\\'->instanceOf(Integer)}'->test::compileAndEval()");

        assertExpressionTrue("'{| test::Enum2.VALUE3->instanceOf(test::Enum2)}'->test::compileAndEval()");
        assertExpressionFalse("'{| test::Enum2.VALUE3->instanceOf(String)}'->test::compileAndEval()");
        assertExpressionFalse("'{| test::Enum2.VALUE3->instanceOf(test::Enum1)}'->test::compileAndEval()");

        assertExpressionTrue("'{| ^test::MyClass()->instanceOf(test::MyClass)}'->test::compileAndEval()");
        assertExpressionFalse("'{| ^test::MyClass()->instanceOf(String)}'->test::compileAndEval()");
    }
}
