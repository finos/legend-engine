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

package org.finos.legend.engine.pure.runtime.planExecuiton.interpreted.natives.test;

import org.finos.legend.engine.pure.runtime.execution.LegendExecuteTest;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.BeforeClass;

public class TestLegendExecuteInterpreted extends LegendExecuteTest
{
    @BeforeClass
    public static void setUp() throws Exception
    {
        functionExecution = interpretedExecution();
    }

    private static FunctionExecutionInterpreted interpretedExecution() throws Exception
    {
        System.out.println("starting preparing interpreted execution");
        FunctionExecutionInterpreted interpretedFunctionExecution = new FunctionExecutionInterpreted();
        interpretedFunctionExecution.init(setUpRuntime(null), new Message(""));
        //interpretedFunctionExecution.setProcessorSupport(new LegendCompileMixedProcessorSupport(interpretedFunctionExecution.getRuntime().getContext(), interpretedFunctionExecution.getRuntime().getModelRepository(), interpretedFunctionExecution.getProcessorSupport()));
        interpretedFunctionExecution.getConsole().disable();
        System.out.println("finish preparing interpreted execution");
        return interpretedFunctionExecution;
    }
}
