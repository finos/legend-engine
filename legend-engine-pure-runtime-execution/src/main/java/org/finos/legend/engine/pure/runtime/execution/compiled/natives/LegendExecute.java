// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.execution.compiled.natives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.ListAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.PairAccessor;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class LegendExecute extends AbstractNative
{
    public LegendExecute()
    {
        super("execute_String_1__Pair_MANY__String_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return this.getClass().getCanonicalName() + ".execute(" + transformedParams.makeString(", ") + ", es)";
    }

    public static String execute(String planAsJson, Object variableOrVariables, ExecutionSupport es)
    {
        RichIterable<Pair<String, Object>> variables;

        if (variableOrVariables instanceof RichIterable)
        {
            variables = (RichIterable<Pair<String, Object>>) variableOrVariables;
        }
        else if (variableOrVariables instanceof Pair)
        {
            variables = Lists.fixedSize.of((Pair<String, Object>) variableOrVariables);
        }
        else if (variableOrVariables == null)
        {
            variables = Lists.fixedSize.empty();
        }
        else
        {
            throw new UnsupportedOperationException("Cannot handle variable of type: " + variableOrVariables.getClass().getSimpleName());
        }

        Map<String, Object> planVariables = pureToPlanVariables(Optional.ofNullable(variables).orElse(Lists.fixedSize.empty()));
        return org.finos.legend.engine.pure.runtime.execution.shared.LegendExecute.doExecute(planAsJson, planVariables);
    }

    private static Map<String, Object> pureToPlanVariables(RichIterable<? extends Pair<? extends String, ?>> variables)
    {
        return variables.toMap(PairAccessor::_first, x -> pureToPlanValue(x._second()));
    }

    private static Object pureToPlanValue(Object pureValue)
    {
        if (pureValue instanceof StrictDate)
        {
            return ((StrictDate) pureValue).format("yyyy-MM-dd");
        }
        else if (pureValue instanceof DateTime)
        {
            return ((DateTime) pureValue).format("yyyy-MM-dd\"T\"HH:mm:ss.SSSZ");
        }
        else if (pureValue instanceof Boolean
                || pureValue instanceof Double // Pure Float
                || pureValue instanceof Long // Pure Integer
                || pureValue instanceof String
                || pureValue instanceof BigDecimal // Pure Decimal
        )
        {
            return pureValue;
        }
        else if (pureValue instanceof ListAccessor<?>)
        {
            return ((ListAccessor<?>) pureValue)._values().collect(LegendExecute::pureToPlanValue);
        }

        throw new UnsupportedOperationException("cannot convert value to plan value: " + pureValue + "(" + pureValue.getClass().getSimpleName() + ")");
    }
}
