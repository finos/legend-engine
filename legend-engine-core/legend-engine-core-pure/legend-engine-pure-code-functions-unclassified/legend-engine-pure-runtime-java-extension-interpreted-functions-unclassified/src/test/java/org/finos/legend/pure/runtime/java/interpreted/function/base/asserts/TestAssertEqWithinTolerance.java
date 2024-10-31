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

public class TestAssertEqWithinTolerance extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    public void testFailure()
    {
        assertExpressionRaisesPureException("\nexpected: 1\nactual:   0", 21, 5, "assertEqWithinTolerance(1, 0, 0)");
        assertExpressionRaisesPureException("\nexpected: 2.718271828459045\nactual:   2.718281828459045", 21, 5, "assertEqWithinTolerance(2.718271828459045, 2.718281828459045, 0.000000001)");
        assertExpressionRaisesPureException("\nexpected: 2.718281828459045\nactual:   2.7182818284590455", 21, 5, "assertEqWithinTolerance(2.718281828459045, 2.7182818284590455, 0.0000000000000001)");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
