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

package org.finos.legend.engine.pure.preeval.rules;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class MapUnrollRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return MetamodelPaths.MAP_FUNCTIONS.anySatisfy(path -> services.runtime().isFunction(function, path))
                && UnrollSupport.allParametersAreInstanceValues(prologue, services);
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalState state = prologue.state();
        services.trace(state, () -> "Expanding: " + services.runtime().typeDescription(prologue.rewritten()._func()));
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        ImmutableList<ValueSpecification> inputs = UnrollSupport.inputs(parameters.get(0), state, services);
        FunctionDefinition<?> lambda = UnrollSupport.lambda(parameters.get(1), services);
        ImmutableList<PrevalResult> results = inputs.collect(v -> UnrollSupport.applyBody(lambda, Lists.immutable.with(v), prologue.original(), state, services));
        MutableList<Object> values = Lists.mutable.empty();
        results.forEach(r ->
        {
            Object v = r.getValue();
            if (v instanceof InstanceValue)
            {
                values.addAllIterable(services.runtime().values((InstanceValue) v));
            }
            else
            {
                values.add(v);
            }
        });
        Object value = services.runtime().newInstanceValue(prologue.rewritten()._genericType(), services.runtime().exactly(values.size()), values);
        return new PrevalResult(value, PrevalResult.allCanPreval(results), services.openVars(results, state), true);
    }
}
