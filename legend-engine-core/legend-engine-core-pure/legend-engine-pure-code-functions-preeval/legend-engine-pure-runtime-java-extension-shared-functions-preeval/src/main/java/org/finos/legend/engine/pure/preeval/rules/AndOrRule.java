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

public final class AndOrRule implements PreParameterRule
{
    @Override
    public boolean matches(FunctionExpression expression, PrevalServices services)
    {
        return services.runtime().isFunction(expression._func(), MetamodelPaths.AND_FUNCTION) || services.runtime().isFunction(expression._func(), MetamodelPaths.OR_FUNCTION);
    }

    @Override
    public PrevalResult apply(FunctionExpression expression, PrevalState state, PrevalServices services)
    {
        boolean shortcut = !services.runtime().isFunction(expression._func(), MetamodelPaths.AND_FUNCTION);
        ImmutableList<ValueSpecification> parameters = Lists.immutable.withAll(expression._parametersValues());
        PrevalResult first = services.preval(parameters.get(0), state);
        if (services.isInstanceValue(first.getValue(), state.getInScopeVars()))
        {
            return RuleSupport.singleBooleanEquals(services, first.getValue(), shortcut)
                    ? first.markModified()
                    : services.preval(parameters.get(1), state).markModified();
        }
        PrevalResult second = services.preval(parameters.get(1), state);
        if (services.isInstanceValue(second.getValue(), state.getInScopeVars()))
        {
            return RuleSupport.singleBooleanEquals(services, second.getValue(), shortcut) ? second.markModified() : first.markModified();
        }
        ImmutableList<PrevalResult> both = Lists.immutable.with(first, second);
        FunctionExpression rewritten = services.runtime().withParametersAndGenericType(expression, both.collect(r -> (ValueSpecification) r.getValue()), expression._genericType());
        return new PrevalResult(rewritten, PrevalResult.allCanPreval(both), services.openVars(both, state), PrevalResult.anyModified(both));
    }
}
