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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.pure.preeval.PrevalHooks;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;

public final class InterpretedPrevalHooks implements PrevalHooks
{
    private final InterpretedPrevalRuntime runtime;
    private final CoreInstance stopPreeval;
    private final VariableContext stopPreevalContext;
    private final CoreInstance shouldInline;
    private final VariableContext shouldInlineContext;
    private final CoreInstance isGeneratedMilestoningProperty;
    private final VariableContext isGeneratedMilestoningPropertyContext;

    public InterpretedPrevalHooks(InterpretedPrevalRuntime runtime, CoreInstance stopPreeval, VariableContext stopPreevalContext, CoreInstance shouldInline, VariableContext shouldInlineContext, CoreInstance isGeneratedMilestoningProperty, VariableContext isGeneratedMilestoningPropertyContext)
    {
        this.runtime = runtime;
        this.stopPreeval = stopPreeval;
        this.stopPreevalContext = stopPreevalContext;
        this.shouldInline = shouldInline;
        this.shouldInlineContext = shouldInlineContext;
        this.isGeneratedMilestoningProperty = isGeneratedMilestoningProperty;
        this.isGeneratedMilestoningPropertyContext = isGeneratedMilestoningPropertyContext;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return this.runtime.evaluateBoolean(this.stopPreeval, this.stopPreevalContext, values);
    }

    @Override
    public boolean shouldInline(Object function)
    {
        return this.runtime.evaluateBoolean(this.shouldInline, this.shouldInlineContext, Lists.immutable.with(function));
    }

    @Override
    public boolean isGeneratedMilestoningProperty(Object function)
    {
        return this.runtime.evaluateBoolean(this.isGeneratedMilestoningProperty, this.isGeneratedMilestoningPropertyContext, Lists.immutable.with(function));
    }
}
