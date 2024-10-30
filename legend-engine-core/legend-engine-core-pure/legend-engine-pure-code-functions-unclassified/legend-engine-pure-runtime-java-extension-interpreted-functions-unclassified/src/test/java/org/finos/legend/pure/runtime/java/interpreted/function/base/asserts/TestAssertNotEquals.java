// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.function.base.asserts;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAssertNotEquals extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("1 should not equal 1", 21, 5, "assertNotEquals(1, 1)");
    }

    @Test
    public void testFailureWithCollections()
    {
        assertExpressionRaisesPureException("[1, 2] should not equal [1, 2]", 34, 5, "assertNotEquals([1, 2], [1, 2])");
        assertExpressionRaisesPureException("['aaa', 'bb'] should not equal ['aaa', 'bb']", 34, 5, "assertNotEquals(['aaa', 'bb'], ['aaa', 'bb'])");
        assertExpressionRaisesPureException("['aaa', 2] should not equal ['aaa', 2]", 34, 5, "assertNotEquals(['aaa', 2], ['aaa', 2])");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair("testAssertNotEquals.pure",
                "function meta::pure::functions::asserts::assertNotEquals(notExpected:Any[*], actual:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    if(eq($notExpected->size(), 1) && eq($actual->size(), 1),\n" +
                        "       | assertNotEquals($notExpected, $actual, '%r should not equal %r', [$notExpected->toOne(), $actual->toOne()]),\n" +
                        "       | assertNotEquals($notExpected, $actual, | $notExpected->map(e | $e->toRepresentation())->joinStrings('[', ', ', ']') + ' should not equal ' + $actual->map(a | $a->toRepresentation())->joinStrings('[', ', ', ']')))\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEquals(notExpected:Any[*], actual:Any[*], message:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!equal($notExpected, $actual), $message);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEquals(notExpected:Any[*], actual:Any[*], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!equal($notExpected, $actual), $formatString, $formatArgs);\n" +
                        "}\n" +
                        "\n" +
                        "function meta::pure::functions::asserts::assertNotEquals(notExpected:Any[*], actual:Any[*], message:Function<{->String[1]}>[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(!equal($notExpected, $actual), $message);\n" +
                        "}\n" +
                        "function meta::pure::functions::asserts::assert(condition:Boolean[1], formatString:String[1], formatArgs:Any[*]):Boolean[1]\n" +
                        "{\n" +
                        "    assert($condition, | format($formatString, $formatArgs));\n" +
                        "}"
        );
    }
}
