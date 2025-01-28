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

package org.finos.legend.engine.pure.runtime.planExecuiton.compiled.natives.test;

import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.engine.pure.runtime.execution.LegendExecuteTest;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.BeforeClass;

public class TestLegendExecuteCompiled extends LegendExecuteTest
{
    @BeforeClass
    public static void setUp() throws Exception
    {
        functionExecution = compileExecution();
    }

    private static FunctionExecutionCompiled compileExecution() throws Exception
    {
        PureRuntime runtime = setUpRuntime(JavaModelFactoryRegistryLoader.loader());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        // The core_* java code is on the class path from the original jar.  That code is generated using distributed mode
        // We need to respect the java code generated in memory class loader as part of the FunctionExecutionCompiled init
        // Otherwise, we get metadata problems (monolithic vs distributed)
        ClassLoader classLoader = new ClassLoader(contextClassLoader == null ? TestLegendExecuteCompiled.class.getClassLoader() : contextClassLoader)
        {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                if (name.startsWith("org.finos.legend.pure.generated.core_"))
                {
                    throw new ClassNotFoundException("Generated should come from memory class loader: " + name);
                }
                return super.loadClass(name, resolve);
            }
        };

        Thread.currentThread().setContextClassLoader(classLoader);

        try
        {
            System.out.println("starting preparing compiled execution");
            FunctionExecutionCompiled compileFunctionExecution = new FunctionExecutionCompiledBuilder().build();
            compileFunctionExecution.getConsole().disable();
            compileFunctionExecution.init(runtime, new Message(""));
            System.out.println("finish preparing compiled execution");
            return compileFunctionExecution;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
