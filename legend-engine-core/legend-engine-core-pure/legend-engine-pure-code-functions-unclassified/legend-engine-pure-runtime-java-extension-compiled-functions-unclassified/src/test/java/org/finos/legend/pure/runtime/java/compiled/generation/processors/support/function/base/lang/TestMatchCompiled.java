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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.lang;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.function.base.lang.AbstractTestMatch;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMatchCompiled extends AbstractTestMatch
{
    @BeforeClass
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Test
    public void testUnAssignedMatchInFuncExpression()
    {
        try
        {
            String func = "function testMatch(v:Any[1]):String[1]\n" +
                    "{\n" +
                    "   $v->match(  [\n" +
                    "                    a:AA[1] | 'AA',\n" +
                    "                    b:BB[1] | 'BB'\n" +
                    "                ]\n" +
                    "             );\n" +
                    "\n" +
                    "   $v->match(  [\n" +
                    "                    a:AA[1] | 'AA',\n" +
                    "                    b:BB[1] | 'BB'\n" +
                    "                ]\n" +
                    "             );\n" +
                    "}\n" +
                    "\n" +
                    "Class AA\n" +
                    "{\n" +
                    "   name:String[*];\n" +
                    "}\n" +
                    "\n" +
                    "Class BB\n" +
                    "{\n" +
                    "   name:String[*];\n" +
                    "}\n";
            this.compileTestSource("fromString.pure", func);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
    }
}
