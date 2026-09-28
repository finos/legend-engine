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
import org.finos.legend.engine.pure.preeval.Multiplicities;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalRuntime;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public final class ConcatenateRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        return runtime.isFunction(prologue.rewritten()._func(), MetamodelPaths.CONCATENATE_FUNCTION)
                && prologue.rewrittenParameters().allSatisfy(p -> hasExactSize(p._multiplicity(), runtime))
                && services.scope().areAllInScope(
                        prologue.parameters()
                                .select(r -> r.getValue() instanceof VariableExpression && runtime.isPureZero(((VariableExpression) r.getValue())._multiplicity()))
                                .flatCollect(PrevalResult::getOpenVars)
                                .distinct(),
                        prologue.state().getInScopeVars());
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        services.trace(prologue.state(), "Handling expand: " + runtime.typeDescription(prologue.rewritten()._func()));
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        Object value;
        if (runtime.isPureZero(parameters.get(0)._multiplicity()))
        {
            value = parameters.get(1);
        }
        else if (runtime.isPureZero(parameters.get(1)._multiplicity()))
        {
            value = parameters.get(0);
        }
        else
        {
            MutableList<Object> values = Lists.mutable.empty();
            parameters.forEach(p ->
            {
                if (p instanceof InstanceValue)
                {
                    values.addAllIterable(runtime.values((InstanceValue) p));
                }
                else
                {
                    values.add(p);
                }
            });
            GenericType genericType = (GenericType) services.genericTypes().resolveGenericType(prologue.rewritten()._genericType(), prologue.state()).getValue();
            value = runtime.newInstanceValue(genericType, runtime.exactly(values.size()), values);
        }
        return new PrevalResult(value, PrevalResult.allCanPreval(prologue.parameters()), prologue.openVars(), true);
    }

    private static boolean hasExactSize(Multiplicity multiplicity, PrevalRuntime runtime)
    {
        return runtime.isPureOne(multiplicity)
                || runtime.isPureZero(multiplicity)
                || (runtime.isMultiplicityConcrete(multiplicity)
                    && Multiplicities.hasUpperBound(runtime, multiplicity)
                    && Multiplicities.hasLowerBound(runtime, multiplicity)
                    && runtime.lowerBound(multiplicity).equals(runtime.upperBound(multiplicity)));
    }
}
