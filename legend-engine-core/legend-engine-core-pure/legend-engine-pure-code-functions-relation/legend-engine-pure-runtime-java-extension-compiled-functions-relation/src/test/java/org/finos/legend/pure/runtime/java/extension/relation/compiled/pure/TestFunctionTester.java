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
import org.junit.Test;

public class TestFunctionTester extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        FunctionExecution execution = getFunctionExecution();
        setUpRuntime(execution);
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    @Test
    public void sortOnTdsWithVariant()
    {
        compileTestSource("fromString.pure",
                "import meta::pure::functions::variant::convert::*;\n" +
                        "function test():Any[*]\n" +
                        "{" +
                        "   println(#TDS\n" +
                        "     id, payload:meta::pure::metamodel::variant::Variant\n" +
                        "     1, \"[1,2,3]\"\n" +
                        "     2, \"[4,5,6]\"\n" +
                        "     3, \"[7,8,9]\"\n" +
                        "     4, \"[10,11,12]\"\n" +
                        "     5, \"[13,14,15]\"\n" +
                        "   #->extend(~reversed:x | $x.payload->toMany(@Integer)->reverse()->toVariant()));\n" +
                        "}");
        execute("test():Any[*]");
    }

    @Test
    public void testPlaygroundCompiledFunction()
    {
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "                  city, country, year, treePlanted\n" +
//                        "                  NYC, USA, 2011, 5000\n" +
//                        "                  NYC, USA, 2000, 5000\n" +
//                        "                  SAN, USA, 2000, 2000\n" +
//                        "                  SAN, USA, 2011, 100\n" +
//                        "                  LND, UK, 2011, 3000\n" +
//                        "                  SAN, USA, 2011, 2500\n" +
//                        "                  NYC, USA, 2000, 10000\n" +
//                        "                  NYC, USA, 2012, 7600\n" +
//                        "                  NYC, USA, 2012, 7600\n" +
//                        "                #->pivot(~year, ~'newCol' : x | $x.treePlanted : y | $y->plus())->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "                  city, country, year, treePlanted\n" +
//                        "                  NYC, USA, 2011, 5000\n" +
//                        "                  NYC, USA, 2000, 5000\n" +
//                        "                #->cast(@meta::pure::metamodel::relation::Relation<(year:Integer)>)->filter(x|$x.year == 2000)->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                                "function test():Any[*]\n" +
//                                        "{" +
//                                        "print(#TDS\n" +
//                                "                  value, str\n" +
//                                "                  1, a\n" +
//                                "                  3, ewe\n" +
//                                "                  4, qw\n" +
//                                "                #\n" +
//                                "                ->filter\n" +
//                                "                (\n" +
//                                "                  x|$x.value == 1" +
//                                        "        )->toString(),1);\n" +
//                                "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                                "function test():Any[*]\n" +
//                                        "{" +
//                                        "print(#TDS\n" +
//                                "                  value, str\n" +
//                                "                  1, a\n" +
//                                "                  3, ewe\n" +
//                                "                  4, qw\n" +
//                                "                #\n" +
//                                "                ->filter\n" +
//                                "                (\n" +
//                                "                  x|$x.value == 1" +
//                                        "        )->toString(),1);\n" +
//                                "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                                "function test():Any[*]\n" +
//                                        "{" +
//                                        "print(#TDS\n" +
//                                "                  value, str\n" +
//                                "                  1, a\n" +
//                                "                  3, ewe\n" +
//                                "                  4, qw\n" +
//                                "                #\n" +
//                                "                ->filter\n" +
//                                "                (\n" +
//                                "                  x|$x.value == 1" +
//                                        "        )->toString(),1);\n" +
//                                "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

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
//                        "       $tds->limit(2)->size()" +
//                        "   ,2);" +
//                        "}\n");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

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

//--------------------------------------------------------------------

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

//--------------------------------------------------------------------

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

//--------------------------------------------------------------------

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
    }
}
