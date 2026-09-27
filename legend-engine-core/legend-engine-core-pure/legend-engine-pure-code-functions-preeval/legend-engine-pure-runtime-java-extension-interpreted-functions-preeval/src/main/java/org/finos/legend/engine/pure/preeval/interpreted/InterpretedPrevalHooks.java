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

package org.finos.legend.engine.pure.preeval.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.pure.preeval.PrevalHooks;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;

public final class InterpretedPrevalHooks implements PrevalHooks
{
    private final InterpretedPrevalRuntime runtime;
    private final CoreInstance stopPreeval;
    private final VariableContext evaluationContext;

    public InterpretedPrevalHooks(InterpretedPrevalRuntime runtime, CoreInstance stopPreeval, VariableContext evaluationContext)
    {
        this.runtime = runtime;
        this.stopPreeval = stopPreeval;
        this.evaluationContext = evaluationContext;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return this.runtime.evaluateBoolean(this.stopPreeval, this.evaluationContext, values);
    }
}
