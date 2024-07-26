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

package org.finos.legend.pure.runtime.java.interpreted.function;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEqualityFunctionIntegrity extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
    }

    @Test
    public void testCannotOverrideIs()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "function is(left:String[1], right:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "    true\n" +
                            "}\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "It is forbidden to override the function 'is'", "testSource.pure", 1, 10, 4, 1, e);
        }
    }

    @Test
    public void testCannotOverrideEq()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "function eq(left:String[1], right:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "    true\n" +
                            "}\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "It is forbidden to override the function 'eq'", "testSource.pure", 1, 10, 4, 1, e);
        }
    }

    @Test
    public void testCannotOverrideEqual()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "function equal(left:String[1], right:String[1]):Boolean[1]\n" +
                            "{\n" +
                            "    true\n" +
                            "}\n");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "It is forbidden to override the function 'equal'", "testSource.pure", 1, 10, 4, 1, e);
        }
    }
}
