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

package org.finos.legend.pure.runtime.java.extension.functions.standard.interpreted;

import org.apache.commons.io.IOUtils;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestFunction_TesterHelper_Interpreted extends PureExpressionTest
{
    @BeforeClass
    public static void setUp()
    {
        FunctionExecution f = getFunctionExecution();
        setUpRuntime(f);
        f.getConsole().setPrintStream(System.out);
        f.getConsole().enable();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testHelper.pure");
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionInterpreted();
    }

    @Test
    public void testNativeFunctionTesterHelperBeforeAddingToPCT() throws IOException
    {
        String pureCode = IOUtils.toString(ClassLoader.getSystemResource("testHelperScratch.pure"), StandardCharsets.UTF_8);
        compileTestSource("testHelper.pure", pureCode);
        this.execute("test():Any[*]");
        runtime.delete("testHelper.pure");
    }
}


