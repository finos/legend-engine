// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.compiler.compiled.natives;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionEndListener;
import org.finos.legend.pure.runtime.java.shared.listeners.ExecutionListeners;
import org.finos.legend.pure.runtime.java.shared.listeners.IdentifiableExecutionEndListener;

class LayerExecutionSupport extends CompiledExecutionSupport
{
    private final CompiledExecutionSupport calling;

    LayerExecutionSupport(CompiledExecutionSupport calling, Metadata layer)
    {
        super(new JavaCompilerState(calling.getMemoryFileManager(), calling.getClassLoader()),
                new CompiledProcessorSupport(calling.getClassLoader(), layer, calling.getExtraSupportedTypes()),
                calling.getSourceRegistry(),
                calling.getCodeStorage(),
                calling.getIncrementalCompiler(),
                calling.getExecutionActivityListener(),
                calling.getConsole(),
                calling.getFunctionCache(),
                calling.getClassCache(),
                calling.getMetadataProvider(),
                Sets.mutable.withAll(calling.getExtraSupportedTypes()),
                calling.getCompiledExtensions(),
                calling.getRuntimeOptions());
        this.calling = calling;
    }

    // Code run in a layer is part of the calling execution, so its end listeners fire when that execution ends.
    @Override
    public void registerExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.calling.registerExecutionEndListener(executionEndListener);
    }

    @Override
    public void registerIdentifiableExecutionEndListener(IdentifiableExecutionEndListener identifiableExecutionEndListener)
    {
        this.calling.registerIdentifiableExecutionEndListener(identifiableExecutionEndListener);
    }

    @Override
    public void unRegisterIdentifiableExecutionEndListener(String listenerId)
    {
        this.calling.unRegisterIdentifiableExecutionEndListener(listenerId);
    }

    @Override
    public void executionEnd(Exception exception)
    {
        this.calling.executionEnd(exception);
    }

    @Override
    public ExecutionListeners getExecutionListeners()
    {
        return this.calling.getExecutionListeners();
    }
}
