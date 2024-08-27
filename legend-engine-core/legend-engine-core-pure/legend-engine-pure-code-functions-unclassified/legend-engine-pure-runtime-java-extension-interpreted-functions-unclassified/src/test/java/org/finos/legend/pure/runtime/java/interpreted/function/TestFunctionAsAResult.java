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

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestFunctionAsAResult extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @Test
    @Ignore
    @ToFix
    public void testLambda()
    {
        compileTestSource("Class Person\n" +
                "{\n" +
                "    name:String[1];\n" +
                "}\n" +
                "\n" +
                "function ascend<T,V>(p:Property<T,V|1>[1]):Function<{T[1],T[1]->Integer[1]}>[1]\n" +
                "{\n" +
                "   {a,b|$p->eval($a)->compare($p->eval($b))}\n" +
                "}\n" +
                "\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "    print(ascend(Person.property('name')->toOne()->cast(@Property<Person,String|1>)));\n" +
                "}\n");
        this.execute("test():Nil[0]");
        Assert.assertEquals("okeee", functionExecution.getConsole().getLine(0));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
