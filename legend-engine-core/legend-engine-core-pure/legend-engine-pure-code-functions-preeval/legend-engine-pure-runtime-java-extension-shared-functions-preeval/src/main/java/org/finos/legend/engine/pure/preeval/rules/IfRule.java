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
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PreParameterRule;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class IfRule implements PreParameterRule
{
    @Override
    public boolean matches(FunctionExpression expression, PrevalServices services)
    {
        return services.runtime().isFunction(expression._func(), MetamodelPaths.IF_FUNCTION);
    }

    @Override
    public PrevalResult apply(FunctionExpression expression, PrevalState state, PrevalServices services)
    {
        ImmutableList<ValueSpecification> parameters = Lists.immutable.withAll(expression._parametersValues());
        PrevalResult condition = services.preval(parameters.get(0), state);
        if (!services.isInstanceValue(condition.getValue(), state.getInScopeVars()))
        {
            return null;
        }
        // parity: Pure casts the condition to an InstanceValue, so an in-scope VariableExpression condition fails here
        ValueSpecification branch = RuleSupport.singleBooleanEquals(services, condition.getValue(), true) ? parameters.get(1) : parameters.get(2);
        FunctionExpression evaluation = services.runtime().withFuncAndParameters(expression, services.runtime().function(MetamodelPaths.EVAL_FUNCTION), Lists.immutable.with(branch));
        return services.preval(evaluation, state).markModified();
    }
}
