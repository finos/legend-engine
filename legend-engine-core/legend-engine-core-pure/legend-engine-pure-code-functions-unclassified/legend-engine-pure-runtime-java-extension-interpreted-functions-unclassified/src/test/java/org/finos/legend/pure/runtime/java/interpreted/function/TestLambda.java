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
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLambda extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());

        //set observer
//        System.setProperty("pure.typeinference.test", "true");
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("inferenceTest.pure");
    }

    @AfterClass
    public static void unsetObserver()
    {
        System.clearProperty("pure.typeinference.test");
    }

    @Test
    public void testLambdaParametersInferenceWithLet()
    {
        try
        {
            compileTestSource("inferenceTest.pure", "" +
                    "function myFunc(func:Function<{String[1],Boolean[1]->String[1]}>[1], b: Boolean[1]):String[1]\n" +
                    "{\n" +
                    "    $func->eval('ok', $b);\n" +
                    "}\n" +
                    "\n" +
                    "function testMany():Nil[0]\n" +
                    "{\n" +
                    "    let l = {a,b|$a+if($b,|'eee',|'rrrr')};\n" +
                    "    print($l->myFunc(true)+$l->myFunc(false));\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureCompilationException);
            Assert.assertEquals("Can't infer the parameters' types for the lambda. Please specify it in the signature.", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assert.assertNotNull(sourceInfo);
            Assert.assertEquals(8, sourceInfo.getLine());
            Assert.assertEquals(14, sourceInfo.getColumn());
        }
    }

    @Test
    public void testLambdaParametersInferenceWithFunctionAnyAsTemplate()
    {
        try
        {
            compileTestSource("inferenceTest.pure", "" +
                    "function myFunc(func:Function<Any>[1]):String[1]\n" +
                    "{\n" +
                    "    'ok'\n" +
                    "}\n" +
                    "" +
                    "function testMany():String[1]\n" +
                    "{\n" +
                    "    myFunc(a|$a+'eee');\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureCompilationException);
            Assert.assertEquals("Can't infer the parameters' types for the lambda. Please specify it in the signature.", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assert.assertNotNull(sourceInfo);
            Assert.assertEquals(7, sourceInfo.getLine());
            Assert.assertEquals(12, sourceInfo.getColumn());
        }
    }

    @Test
    public void testLambdaWithUnknownTypeAsParameter()
    {
        try
        {
            compileTestSource("inferenceTest.pure", "" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "    print({a:Employee[1], b:Integer[1]|$b});\n" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureCompilationException);
            Assert.assertEquals("Employee has not been defined!", pe.getInfo());

            SourceInformation sourceInfo = pe.getSourceInformation();
            Assert.assertNotNull(sourceInfo);
            Assert.assertEquals(3, sourceInfo.getLine());
            Assert.assertEquals(14, sourceInfo.getColumn());
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }
}
