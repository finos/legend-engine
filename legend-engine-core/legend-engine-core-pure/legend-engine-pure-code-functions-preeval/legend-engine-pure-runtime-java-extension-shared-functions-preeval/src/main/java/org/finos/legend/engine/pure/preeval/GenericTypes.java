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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public final class GenericTypes
{
    private final PrevalRuntime runtime;

    public GenericTypes(PrevalRuntime runtime)
    {
        this.runtime = runtime;
    }

    public PrevalResult resolveGenericType(GenericType genericType, PrevalState state)
    {
        if (genericType._typeParameter() == null)
        {
            if (genericType._rawType() instanceof FunctionType)
            {
                // parity: Pure reports FunctionType resolution as modified with canPreval=false even when nothing changes
                return new PrevalResult(this.runtime.withRawType(genericType, resolveFunctionType((FunctionType) genericType._rawType(), state)), false, Lists.immutable.empty(), true);
            }
            ListIterable<PrevalResult> arguments = Lists.mutable.withAll(genericType._typeArguments()).collect(argument -> resolveGenericType(argument, state));
            if (!PrevalResult.anyModified(arguments))
            {
                return PrevalResult.unmodified(genericType);
            }
            return new PrevalResult(this.runtime.withTypeArguments(genericType, arguments.collect(a -> (GenericType) a.getValue())), PrevalResult.allCanPreval(arguments), Lists.immutable.empty(), true);
        }
        Object bound = state.getInScopeTypeParams().get(genericType._typeParameter()._name());
        return bound == null ? PrevalResult.unmodified(genericType) : new PrevalResult(bound, true, Lists.immutable.empty(), true);
    }

    public FunctionType resolveFunctionType(FunctionType functionType, PrevalState state)
    {
        ListIterable<VariableExpression> parameters = Lists.mutable.<VariableExpression>withAll(functionType._parameters()).collect(p -> this.runtime.withGenericType(p, (GenericType) resolveGenericType(p._genericType(), state).getValue()));
        return this.runtime.withSignature(functionType, parameters, (GenericType) resolveGenericType(functionType._returnType(), state).getValue());
    }
}
