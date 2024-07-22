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

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAssertEquals extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("\nexpected: 1\nactual:   2", 3, 9, "assertEquals(1, 2)");
    }

    @Test
    public void testFailureWithCollections()
    {
        assertExpressionRaisesPureException("\nexpected: [1, 3, 2]\nactual:   [2, 4, 1, 5]", 3, 9, "assertEquals([1, 3, 2], [2, 4, 1, 5])");
        assertExpressionRaisesPureException("\nexpected: [1, 2]\nactual:   [2, 1]", 3, 9, "assertEquals([1, 2], [2, 1])");
        assertExpressionRaisesPureException("\nexpected: ['aaa', 2]\nactual:   [2, 'aaa']", 3, 9, "assertEquals(['aaa', 2], [2, 'aaa'])");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
