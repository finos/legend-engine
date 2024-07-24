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

package org.finos.legend.pure.runtime.java.extension.relation;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is a tester for interpreted function, so we don't need to test via the Pure IDE, hence a faster dev feedback loop
 */
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
        return new FunctionExecutionInterpreted();
    }

    // TO FIX: this should work?
    //    @Test
    //    public void testColumnsOfRelationAny()
    //    {
    //        compileTestSource("fromString.pure",
    //                "function test():Any[*]\n" +
    //                        "{" +
    //                        "#TDS\n" +
    //                        "                  city, country, year, treePlanted\n" +
    //                        "                  NYC, USA, 2011, 5000\n" +
    //                        "                #->cast(@meta::pure::metamodel::relation::Relation<Any>)->columns();\n" +
    //                        "}");
    //        this.execute("test():Any[*]");
    //        runtime.delete("fromString.pure");
    //    }

    @Test
    public void testPlaygroundInterpretedFunction()
    {
//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "   city, country, year, treePlanted\n" +
//                        "   NYC, USA, 2011, 5000\n" +
//                        "   NYC, USA, 2000, 5000\n" +
//                        "   SAN, USA, 2000, 2000\n" +
//                        "   SAN, USA, 2011, 100\n" +
//                        "   LND, UK, 2011, 3000\n" +
//                        "   SAN, USA, 2011, 2500\n" +
//                        "   NYC, USA, 2000, 10000\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "#->pivot(~[country,city], ~[sum : x | $x.treePlanted : y | $y->plus(), count : x | $x : y | $y->size()])->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "   city, country, year, treePlanted\n" +
//                        "   NYC, USA, 2011, 5000\n" +
//                        "   NYC, USA, 2000, 5000\n" +
//                        "   SAN, USA, 2000, 2000\n" +
//                        "   SAN, USA, 2011, 100\n" +
//                        "   LND, UK, 2011, 3000\n" +
//                        "   SAN, USA, 2011, 2500\n" +
//                        "   NYC, USA, 2000, 10000\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "#->pivot(~[country,city], ~[sum : x | $x.treePlanted : y | $y->plus(), count : x | $x : y | $y->size()])->cast(@meta::pure::metamodel::relation::Relation<(year: Number)>)->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");

//--------------------------------------------------------------------

//        compileTestSource("fromString.pure",
//                "function test():Any[*]\n" +
//                        "{" +
//                        "print(#TDS\n" +
//                        "   city, country, year, treePlanted\n" +
//                        "   NYC, USA, 2011, 5000\n" +
//                        "   NYC, USA, 2000, 5000\n" +
//                        "   SAN, USA, 2000, 2000\n" +
//                        "   SAN, USA, 2011, 100\n" +
//                        "   LND, UK, 2011, 3000\n" +
//                        "   SAN, USA, 2011, 2500\n" +
//                        "   NYC, USA, 2000, 10000\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "   NYC, USA, 2012, 7600\n" +
//                        "#->pivot(~[year], ~['newCol' : x | $x.treePlanted : y | $y->plus()])->toString(),1);\n" +
//                        "}");
//        this.execute("test():Any[*]");
//        runtime.delete("fromString.pure");
    }
}
