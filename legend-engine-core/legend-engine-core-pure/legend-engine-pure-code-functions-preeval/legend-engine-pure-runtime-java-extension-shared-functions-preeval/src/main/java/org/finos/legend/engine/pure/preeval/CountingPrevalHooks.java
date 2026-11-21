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

package org.finos.legend.engine.pure.preeval;

import java.util.function.Supplier;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

final class CountingPrevalHooks implements PrevalHooks
{
    private final PrevalHooks delegate;

    CountingPrevalHooks(PrevalHooks delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return timed("stopPreeval", () -> this.delegate.stopPreeval(values));
    }

    @Override
    public boolean shouldInline(Object function)
    {
        return timed("shouldInline", () -> this.delegate.shouldInline(function));
    }

    @Override
    public boolean isGeneratedMilestoningProperty(Object function)
    {
        return timed("isGeneratedMilestoningProperty", () -> this.delegate.isGeneratedMilestoningProperty(function));
    }

    @Override
    public boolean isGetAllFunction(Object function)
    {
        return timed("isGetAllFunction", () -> this.delegate.isGetAllFunction(function));
    }

    @Override
    public ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)
    {
        return timed("resolveTdsSchema", () -> this.delegate.resolveTdsSchema(value, rollingInScopeVars));
    }

    private static <T> T timed(String hook, Supplier<T> call)
    {
        long start = System.nanoTime();
        try
        {
            return call.get();
        }
        finally
        {
            PreevalStatistics.hookCalled(hook, System.nanoTime() - start);
        }
    }
}
