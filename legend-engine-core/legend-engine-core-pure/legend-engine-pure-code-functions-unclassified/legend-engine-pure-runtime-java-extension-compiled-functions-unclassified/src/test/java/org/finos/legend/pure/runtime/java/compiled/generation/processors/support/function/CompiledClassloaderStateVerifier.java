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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function;

import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.junit.Assert;

public class CompiledClassloaderStateVerifier implements RuntimeVerifier.FunctionExecutionStateVerifier
{
    private int classLoaderClassCount;

    @Override
    public void snapshotState(FunctionExecution functionExecution)
    {
        FunctionExecutionCompiled functionExecutionCompiled = (FunctionExecutionCompiled)functionExecution;
        this.classLoaderClassCount = functionExecutionCompiled.getJavaCompiler().getClassLoader().loadedClassCount();
    }

    @Override
    public void assertStateSame(FunctionExecution functionExecution)
    {
        FunctionExecutionCompiled functionExecutionCompiled = (FunctionExecutionCompiled)functionExecution;
        Assert.assertEquals(this.classLoaderClassCount, functionExecutionCompiled.getJavaCompiler().getClassLoader().loadedClassCount());
    }
}

