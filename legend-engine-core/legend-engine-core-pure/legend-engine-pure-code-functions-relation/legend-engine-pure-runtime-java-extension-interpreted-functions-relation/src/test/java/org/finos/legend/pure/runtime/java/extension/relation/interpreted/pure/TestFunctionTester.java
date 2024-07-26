// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.relation.interpreted.pure;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
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
        FunctionExecutionInterpreted fi = new FunctionExecutionInterpreted();
        fi.getConsole().enable();
        return fi;
    }

    @org.junit.Test
    public void testFunction()
    {
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "            id, grp, name\n" +
//                        "        1, 2, A\n" +
//                        "        2, 1, B\n" +
//                        "        3, 3, C\n" +
//                        "        4, 4, D\n" +
//                        "        5, 2, E\n" +
//                        "        6, 1, F\n" +
//                        "        7, 3, G\n" +
//                        "        8, 1, H\n" +
//                        "        9, 5, I\n" +
//                        "        10, 0, J\n" +
//                        "                #->groupBy(~grp, ~[newCol : x | $x.name : y | $y->joinStrings(''), YoCol : x | $x.id : y | $y->plus()])->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
//
//
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "                val, str\n" +
//                        "            1, a\n" +
//                        "            3, ewe\n" +
//                        "            4, qw\n" +
//                        "            5, wwe\n" +
//                        "            6, weq\n" +
//                        "                #->extend(~[name:c|$c.val->toOne() + 1, other:x|$x.str->toOne()+'_ext'])->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "      id, grp, name\n" +
//                        "      1, 2, A\n" +
//                        "      2, 1, B\n" +
//                        "      3, 3, C\n" +
//                        "      4, 4, D\n" +
//                        "      5, 2, E\n" +
//                        "      6, 1, F\n" +
//                        "      7, 3, G\n" +
//                        "      8, 1, H\n" +
//                        "      9, 5, I\n" +
//                        "      10, 0, J\n" +
//                        "      #" +
//                        //"    ->extend(~newOne:{x|$x.id}:y|$y->plus())" +
//                        //"     ->extend(win(~grp), ~t:{w,z|$z.id}:y|$y->plus())" +
//                        "     ->extend(win(~grp, [~id->descending()]), ~te:{w,z|$w->lead($z).id})" +
//                        "     ->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");


        compileTestSource("fromString.pure",
                "function test():Any[*]\n" +
                        "{" +
                        "print(                 #TDS\n" +
                        "                  id, grp, name\n" +
                        "                  1, 2, A\n" +
                        "                  2, 1, B\n" +
                        "                  3, 3, C\n" +
                        "                  4, 4, D\n" +
                        "                  5, 2, E\n" +
                        "                  6, 1, F\n" +
                        "                  7, 3, G\n" +
                        "                  8, 1, H\n" +
                        "                  9, 5, I\n" +
                        "                  10, 0, J\n" +
                        "                #->extend(over(~grp), ~newCol:{w,c|$c.id}:y|$y->plus())" +
                        "     ->toString(),1);\n" +
                        "}");
        this.execute("test():Any[*]");
        runtime.delete("fromString.pure");

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
//                        "       $tds->limit(2)->size()" +
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
