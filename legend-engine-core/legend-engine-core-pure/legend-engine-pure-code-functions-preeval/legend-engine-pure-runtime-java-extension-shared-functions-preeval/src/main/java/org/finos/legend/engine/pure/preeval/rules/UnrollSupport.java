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
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

final class UnrollSupport
{
    private UnrollSupport()
    {
    }

    static boolean allParametersAreInstanceValues(Prologue prologue, PrevalServices services)
    {
        return prologue.rewrittenParameters().allSatisfy(p -> services.isInstanceValue(p, prologue.state().getInScopeVars()));
    }

    static ImmutableList<ValueSpecification> inputs(ValueSpecification collection, PrevalState state, PrevalServices services)
    {
        return services.runtime().reactivate(collection, state.getInScopeVars()).collect(v -> v instanceof ValueSpecification
                ? (ValueSpecification) v
                : services.runtime().newInstanceValue(collection._genericType(), services.runtime().pureOne(), Lists.immutable.with(v)));
    }

    static FunctionDefinition<?> lambda(ValueSpecification parameter, PrevalServices services)
    {
        if (!(parameter instanceof InstanceValue))
        {
            throw services.runtime().error("Cast exception: " + services.runtime().typeDescription(parameter) + " cannot be cast to InstanceValue");
        }
        ImmutableList<Object> values = services.runtime().values((InstanceValue) parameter);
        if (values.size() != 1)
        {
            throw services.runtime().error("Cannot cast a collection of size " + values.size() + " to multiplicity [1]");
        }
        Object function = values.getOnly();
        if (!(function instanceof FunctionDefinition))
        {
            throw services.runtime().error("Cast exception: " + services.runtime().typeDescription(function) + " cannot be cast to FunctionDefinition");
        }
        if (Lists.mutable.withAll(((FunctionDefinition<?>) function)._expressionSequence()).size() != 1)
        {
            throw services.runtime().error("Assert failure");
        }
        return (FunctionDefinition<?>) function;
    }

    static PrevalResult applyBody(FunctionDefinition<?> lambda, ImmutableList<ValueSpecification> arguments, FunctionExpression original, PrevalState state, PrevalServices services)
    {
        PrevalState scoped = services.addToScope(state, lambda, Lists.immutable.empty(), arguments, false);
        ValueSpecification body = Lists.immutable.<ValueSpecification>withAll(lambda._expressionSequence()).getOnly();
        // parity: Pure's map and fold handlers type the lambda body with the original map/fold expression's genericType, not the body's own
        ValueSpecification typed = services.runtime().withGenericType(body, (GenericType) services.genericTypes().resolveGenericType(original._genericType(), scoped).getValue());
        return services.preval(typed, scoped);
    }
}
