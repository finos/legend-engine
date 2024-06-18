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
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataEager;
import org.junit.Assert;

public class CompiledMetadataStateVerifier implements RuntimeVerifier.FunctionExecutionStateVerifier
{
    private int classCacheSizeBefore;
    private int metadataCount;

    @Override
    public void snapshotState(FunctionExecution functionExecution)
    {
        FunctionExecutionCompiled functionExecutionCompiled = (FunctionExecutionCompiled)functionExecution;

        MetadataEager metamodel = this.getMetamodel(functionExecutionCompiled);
        this.classCacheSizeBefore = functionExecutionCompiled.getClassCacheSize();
        this.metadataCount = metamodel.getSize();
    }

    @Override
    public void assertStateSame(FunctionExecution functionExecution)
    {
        FunctionExecutionCompiled functionExecutionCompiled = (FunctionExecutionCompiled)functionExecution;
        MetadataEager metamodel = this.getMetamodel(functionExecutionCompiled);
        Assert.assertEquals(this.classCacheSizeBefore, functionExecutionCompiled.getClassCacheSize());
        Assert.assertEquals(this.metadataCount, metamodel.getSize());
    }

    private MetadataEager getMetamodel(FunctionExecutionCompiled functionExecutionCompiled)
    {
        CompiledProcessorSupport compiledProcessorSupport = (CompiledProcessorSupport)functionExecutionCompiled.getProcessorSupport();
        return (MetadataEager)compiledProcessorSupport.getMetadata();
    }
}
