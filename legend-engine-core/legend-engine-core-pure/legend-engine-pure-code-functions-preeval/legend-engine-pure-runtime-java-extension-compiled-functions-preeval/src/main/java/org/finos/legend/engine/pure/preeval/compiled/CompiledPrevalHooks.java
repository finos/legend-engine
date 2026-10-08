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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.pure.preeval.PrevalHooks;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

final class CompiledPrevalHooks implements PrevalHooks
{
    private final Function<?> stopPreeval;
    private final Function<?> shouldInline;
    private final Function<?> isGeneratedMilestoningProperty;
    private final Function<?> isGetAllFunction;
    private final Function<?> resolveTdsSchema;
    private final ExecutionSupport executionSupport;

    CompiledPrevalHooks(CoreInstance hooks, ExecutionSupport executionSupport)
    {
        this.stopPreeval = (Function<?>) hooks.getValueForMetaPropertyToOne("stopPreeval");
        this.shouldInline = (Function<?>) hooks.getValueForMetaPropertyToOne("shouldInline");
        this.isGeneratedMilestoningProperty = (Function<?>) hooks.getValueForMetaPropertyToOne("isGeneratedMilestoningProperty");
        this.isGetAllFunction = (Function<?>) hooks.getValueForMetaPropertyToOne("isGetAllFunction");
        this.resolveTdsSchema = (Function<?>) hooks.getValueForMetaPropertyToOne("resolveTdsSchema");
        this.executionSupport = executionSupport;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return (Boolean) Pure.evaluate(this.executionSupport, this.stopPreeval, CoreGen.bridge, Lists.mutable.withAll(values));
    }

    @Override
    public boolean shouldInline(Object function)
    {
        return (Boolean) Pure.evaluate(this.executionSupport, this.shouldInline, CoreGen.bridge, function);
    }

    @Override
    public boolean isGeneratedMilestoningProperty(Object function)
    {
        return (Boolean) Pure.evaluate(this.executionSupport, this.isGeneratedMilestoningProperty, CoreGen.bridge, function);
    }

    @Override
    public boolean isGetAllFunction(Object function)
    {
        return (Boolean) Pure.evaluate(this.executionSupport, this.isGetAllFunction, CoreGen.bridge, function);
    }

    @Override
    public ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)
    {
        PureMap vars = new PureMap(Maps.mutable.empty());
        rollingInScopeVars.forEachKeyValue((name, values) -> vars.getMap().put(name, CoreGen.bridge.buildList()._valuesAddAll(values)));
        return Lists.immutable.withAll(CompiledSupport.toPureCollection(Pure.evaluate(this.executionSupport, this.resolveTdsSchema, CoreGen.bridge, value, vars)));
    }
}
