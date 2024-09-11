// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.relation.compiled.pure;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.BeforeClass;

public class TestFunctionTester extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
//        return new FunctionExecutionInterpreted();
        return new FunctionExecutionCompiledBuilder().build();
    }

    @org.junit.Test
    public void testFunction()
    {
//        //--------------------------------------------------------------------
//
        compileTestSource("fromString.pure",
                "function test():Any[*]\n" +
                        "{ " +
                        "    let tds = #TDS\n" +
                        "              id, name\n" +
                        "              1, George\n" +
                        "              2, Pierre\n" +
                        "              3, Sachin\n" +
                        "              4, David\n" +
                        "            #;\n" +
                        "\n" +
                        "  print(" +
                        "       $tds->limit(2)->size()" +
                        "   ,2);" +
                        "}\n");
        this.execute("test():Any[*]");
        runtime.delete("fromString.pure");
//
//        //--------------------------------------------------------------------
//
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{ " +
//                        "    let tds = #TDS\n" +
//                        "              id, name\n" +
//                        "              1, George\n" +
//                        "              2, Pierre\n" +
//                        "              3, Sachin\n" +
//                        "              4, David\n" +
//                        "            #;\n" +
//                        "\n" +
//                        "  print(" +
//                        "       $tds->drop(3)->size()" +
//                        "   ,2);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
//
//        //--------------------------------------------------------------------
//
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{ " +
//                        "    let tds = #TDS\n" +
//                        "              id, name\n" +
//                        "              1, George\n" +
//                        "              2, Pierre\n" +
//                        "              3, Sachin\n" +
//                        "              4, David\n" +
//                        "            #;\n" +
//                        "\n" +
//                        "  print(" +
//                        "       $tds->rename(~id,~newId)" +
//                        "   ,2);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
//
//        //--------------------------------------------------------------------
//
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{ " +
//                        "    let tds = #TDS\n" +
//                        "              id, name\n" +
//                        "              1, George\n" +
//                        "              2, Pierre\n" +
//                        "              3, Sachin\n" +
//                        "              4, David\n" +
//                        "            #;\n" +
//                        "\n" +
//                        "  print(" +
//                        "       $tds->concatenate($tds)->size()" +
//                        "   ,2);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
//
//
//        //--------------------------------------------------------------------
//
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{ " +
//                        "    let tds = #TDS\n" +
//                        "              id, name\n" +
//                        "              1, George\n" +
//                        "              2, Pierre\n" +
//                        "              3, Sachin\n" +
//                        "              4, David\n" +
//                        "            #;\n" +
//                        "\n" +
//                        "  print(" +
//                        "       $tds->filter(x | $x.id > 2)->size()" +
//                        "   ,2);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
//
//        //--------------------------------------------------------------------
    }
}
