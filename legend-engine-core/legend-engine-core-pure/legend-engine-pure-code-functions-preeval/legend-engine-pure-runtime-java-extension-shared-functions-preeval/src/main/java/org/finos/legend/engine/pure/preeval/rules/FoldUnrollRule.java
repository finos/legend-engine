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
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class FoldUnrollRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        return services.runtime().isFunction(prologue.rewritten()._func(), MetamodelPaths.FOLD_FUNCTION)
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
        PrevalResult accumulator = services.preval(parameters.get(2), state);
        for (ValueSpecification input : inputs)
        {
            accumulator = UnrollSupport.applyBody(lambda, Lists.immutable.with(input, (ValueSpecification) accumulator.getValue()), prologue.original(), state, services);
        }
        return accumulator.markModified();
    }
}
