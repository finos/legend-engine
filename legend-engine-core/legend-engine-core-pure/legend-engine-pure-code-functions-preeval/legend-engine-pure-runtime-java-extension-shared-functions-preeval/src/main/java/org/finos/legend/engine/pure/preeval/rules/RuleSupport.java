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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

final class RuleSupport
{
    private RuleSupport()
    {
    }

    static boolean singleBooleanEquals(PrevalServices services, Object instanceValue, boolean expected)
    {
        if (!(instanceValue instanceof InstanceValue))
        {
            return false;
        }
        ImmutableList<Object> values = services.runtime().values((InstanceValue) instanceValue);
        if (values.size() != 1)
        {
            return false;
        }
        Boolean value = services.runtime().booleanValue(values.getOnly());
        return value != null && value == expected;
    }

    static PrevalResult prevalBody(FunctionDefinition<?> function, PrevalState state, PrevalServices services)
    {
        ValueSpecification body = Lists.immutable.<ValueSpecification>withAll(function._expressionSequence()).getOnly();
        ValueSpecification typed = services.runtime().withGenericType(body, (GenericType) services.genericTypes().resolveGenericType(body._genericType(), state).getValue());
        return services.preval(typed, state);
    }
}
