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

package org.finos.legend.engine.pure.preeval.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.engine.pure.preeval.PreevalStatistics;
import org.finos.legend.engine.pure.preeval.Preevaluator;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.pure.generated.Root_meta_pure_tools_DebugContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.lang.reflect.InvocationTargetException;

public final class CompiledPreeval
{
    private static final String PREVAL_RESULT = "meta::pure::functions::preeval::PrevalResult";

    private CompiledPreeval()
    {
    }

    public static CoreInstance preval(Object item, PureMap inScopeVars, PureMap rollingInScopeVars, CoreInstance hooks, CoreInstance debug, SourceInformation sourceInformation, ExecutionSupport executionSupport)
    {
        CompiledPrevalRuntime runtime = new CompiledPrevalRuntime(sourceInformation, executionSupport);
        PrevalState state = PrevalState.initial(toVars(inScopeVars), toVars(rollingInScopeVars), ((Root_meta_pure_tools_DebugContext) debug)._debug());
        PrevalResult result = new Preevaluator(runtime, PreevalStatistics.instrument(new CompiledPrevalHooks(hooks, executionSupport))).preval(item, state);
        CoreInstance pureResult = ((CompiledExecutionSupport) executionSupport).getProcessorSupport().newCoreInstance(null, PREVAL_RESULT, null);
        set(pureResult, "_value", Object.class, result.getValue());
        set(pureResult, "_canPreval", boolean.class, result.canPreval());
        set(pureResult, "_openVars", RichIterable.class, Lists.mutable.withAll(result.getOpenVars()));
        set(pureResult, "_modified", boolean.class, result.isModified());
        return pureResult;
    }

    public static String preevalImplementation()
    {
        return PreevalImplementation.current().name();
    }

    private static void set(CoreInstance instance, String setter, Class<?> parameterType, Object value)
    {
        try
        {
            instance.getClass().getMethod(setter, parameterType).invoke(instance, value);
        }
        catch (NoSuchMethodException | IllegalAccessException e)
        {
            throw new IllegalStateException("Cannot call " + setter + " on " + instance.getClass().getName(), e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException("Failed to call " + setter + " on " + instance.getClass().getName(), e.getCause());
        }
    }

    static ImmutableMap<String, ImmutableList<Object>> toVars(PureMap map)
    {
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.empty();
        map.getMap().forEachKeyValue((key, value) -> vars.put((String) key, Lists.immutable.withAll(((List<?>) value)._values())));
        return vars.toImmutable();
    }
}
