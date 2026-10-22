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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.util.function.BiFunction;

final class LambdaHolders
{
    private final PrevalRuntime runtime;
    private final BiFunction<Object, PrevalState, PrevalResult> preval;

    LambdaHolders(PrevalRuntime runtime, BiFunction<Object, PrevalState, PrevalResult> preval)
    {
        this.runtime = runtime;
        this.preval = preval;
    }

    PrevalResult prevalHolder(Object item, PrevalState state)
    {
        if (this.runtime.isInstanceOf(item, MetamodelPaths.COLUMN_SPECIFICATION))
        {
            return this.runtime.isInstanceOf(item, MetamodelPaths.BASIC_COLUMN_SPECIFICATION) ? prevalBasicColumnSpecification(item, state) : PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.TDS_AGGREGATE_VALUE) || this.runtime.isInstanceOf(item, MetamodelPaths.COLLECTION_AGGREGATE_VALUE))
        {
            return prevalPair(item, "mapFn", "aggregateFn", state);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.AGG_COL_SPEC))
        {
            return prevalPair(item, "map", "reduce", state);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.AGG_COL_SPEC_ARRAY))
        {
            ListIterable<PrevalResult> specs = this.runtime.getPropertyValues(item, "aggSpecs").collect(s -> this.preval.apply(s, state));
            return PrevalResult.anyModified(specs)
                    ? new PrevalResult(this.runtime.withPropertyValues(item, "aggSpecs", specs.collect(PrevalResult::getValue)), true, Lists.immutable.empty(), true)
                    : PrevalResult.unmodified(item);
        }
        return null;
    }

    private PrevalResult prevalBasicColumnSpecification(Object item, PrevalState state)
    {
        Object function = this.runtime.getPropertyValue(item, "func");
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.withMapIterable(state.getInScopeVars());
        if (function instanceof LambdaFunction)
        {
            this.runtime.openVariableValues((LambdaFunction<?>) function).forEachKeyValue(vars::put);
        }
        PrevalResult result = this.preval.apply(function, state.withInScopeTypeParams(Maps.immutable.empty()).withInScopeVars(vars.toImmutable()));
        return result.isModified()
                ? new PrevalResult(this.runtime.withPropertyValues(item, "func", Lists.immutable.with(result.getValue())), true, Lists.immutable.empty(), true)
                : PrevalResult.unmodified(item);
    }

    private PrevalResult prevalPair(Object item, String first, String second, PrevalState state)
    {
        PrevalResult firstResult = this.preval.apply(this.runtime.getPropertyValue(item, first), state);
        PrevalResult secondResult = this.preval.apply(this.runtime.getPropertyValue(item, second), state);
        if (!firstResult.isModified() && !secondResult.isModified())
        {
            return PrevalResult.unmodified(item);
        }
        Object withFirst = this.runtime.withPropertyValues(item, first, Lists.immutable.with(firstResult.getValue()));
        return new PrevalResult(this.runtime.withPropertyValues(withFirst, second, Lists.immutable.with(secondResult.getValue())), true, Lists.immutable.empty(), true);
    }
}
