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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEvaluateFunctions extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        AbstractPureTestWithCoreCompiled.setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        AbstractPureTestWithCoreCompiled.runtime.delete("fromString.pure");
        AbstractPureTestWithCoreCompiled.runtime.compile();
    }

    @Test
    public void testFilterSimple()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert('test' == ['a','b','test']->filter(x|$x == 'test'), |'')\n" +
                        "}");
        this.compileAndExecute("test():Boolean[1]");
    }

    @Test
    public void testFilterReflectiveEval()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert('test' == filter_T_MANY__Function_1__T_MANY_->eval(['a','b','test'], x:String[1]|$x == 'test'), |'')\n" +
                        "}");
        this.compileAndExecute("test():Boolean[1]");
    }

    @Test
    public void testFilterReflectiveEvaluate()
    {
        AbstractPureTestWithCoreCompiled.compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert('test' == filter_T_MANY__Function_1__T_MANY_->evaluate([list(['a','b','test']), list(x:String[1]|$x == 'test')]), |'')\n" +
                        "}");
        this.compileAndExecute("test():Boolean[1]");

    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}