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

import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestParameters extends AbstractPureTestWithCoreCompiled
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
        runtime.compile();
    }

    @Test
    public void testFunctionParametersTypes()
    {
        try
        {
            compileTestSource("fromString.pure", "function called(param:Integer[1]):Nil[0]\n" +
                    "{\n" +
                    "   print($param, 1);\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "    called('aaa');\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "called(_:String[1])\n" +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    "\tcalled(Integer[1]):Nil[0]\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE, 7, 5, e);
        }
    }


    @Test
    public void testFunctionParametersNestedValidation()
    {
        try
        {
            compileTestSource("fromString.pure", "Class Employee\n" +
                    "{\n" +
                    "    name:String[1];\n" +
                    "}\n" +
                    "function called(employee:Employee[1]):Nil[0]\n" +
                    "{\n" +
                    "   print($employee, 1);\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "    called(^Employee(name=['ee','err']));\n" +
                    "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertOriginatingPureException(PureCompilationException.class, "Multiplicity Error: [2] is not compatible with [1]", 11, 26, e);
        }
    }

    @Test
    public void testFunctionParameterTypeError()
    {
        try
        {
            compileTestSource("fromString.pure", "function test():Nil[0]\n" +
                    "{\n" +
                    "    print(a:String[1]|'a'+$a->eval('errre'));\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "eval(_:String[1],_:String[1])\n" +
                    PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                    PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                    "\tmeta::pure::functions::lang::eval(Function<{->V[m]}>[1]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{S[n], T[o], U[p], W[q], X[r], Y[s], Z[t]->V[m]}>[1], S[n], T[o], U[p], W[q], X[r], Y[s], Z[t]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n], U[p], W[q], X[r], Y[s], Z[t]->V[m]}>[1], T[n], U[p], W[q], X[r], Y[s], Z[t]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n], U[p], W[q], X[r], Y[s]->V[m]}>[1], T[n], U[p], W[q], X[r], Y[s]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n], U[p], W[q], X[r]->V[m]}>[1], T[n], U[p], W[q], X[r]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n], U[p], W[q]->V[m]}>[1], T[n], U[p], W[q]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n], U[p]->V[m]}>[1], T[n], U[p]):V[m]\n" +
                    "\tmeta::pure::functions::lang::eval(Function<{T[n]->V[m]}>[1], T[n]):V[m]\n", 3, 31, e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
