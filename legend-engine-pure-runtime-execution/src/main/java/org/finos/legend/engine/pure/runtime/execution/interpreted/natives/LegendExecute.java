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

package org.finos.legend.engine.pure.runtime.execution.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Map;
import java.util.Stack;

public class LegendExecute extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    private final ModelRepository repository;

    public LegendExecute(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.functionExecution = functionExecution;
        this.repository = modelRepository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance planAsJson = params.get(0);
        CoreInstance vars = params.get(1);

        String planAsJsonValue = Instance.getValueForMetaPropertyToOneResolved(planAsJson, M3Properties.values, processorSupport).getName();
        Map<String, Object> planVars = this.pureToPlanVariables(vars, processorSupport);
        String result = org.finos.legend.engine.pure.runtime.execution.shared.LegendExecute.doExecute(planAsJsonValue, planVars);
        return ValueSpecificationBootstrap.newStringLiteral(this.repository, result, processorSupport);
    }

    private Map<String, Object> pureToPlanVariables(CoreInstance vars, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToManyResolved(vars, M3Properties.values, processorSupport)
                .toMap(
                    x -> Instance.getValueForMetaPropertyToOneResolved(x, "first", processorSupport).getName(),
                    x -> pureToPlanValue(Instance.getValueForMetaPropertyToOneResolved(x, "second", processorSupport), processorSupport)
                );
    }

    private static Object pureToPlanValue(CoreInstance coreInstance, ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(coreInstance, M3Paths.Date, processorSupport))
        {
            return PrimitiveUtilities.getDateValue(coreInstance).toString();
        }
        if (Instance.instanceOf(coreInstance, M3Paths.Integer, processorSupport))
        {
            return PrimitiveUtilities.getIntegerValue(coreInstance);
        }
        if (Instance.instanceOf(coreInstance, M3Paths.Float, processorSupport))
        {
            return PrimitiveUtilities.getFloatValue(coreInstance).doubleValue();
        }
        if (Instance.instanceOf(coreInstance, M3Paths.Boolean, processorSupport))
        {
            return PrimitiveUtilities.getBooleanValue(coreInstance);
        }
        if (Instance.instanceOf(coreInstance, M3Paths.Decimal, processorSupport))
        {
            return PrimitiveUtilities.getDecimalValue(coreInstance);
        }
        if (Instance.instanceOf(coreInstance, M3Paths.String, processorSupport))
        {
            return PrimitiveUtilities.getStringValue(coreInstance);
        }
        if (Instance.instanceOf(coreInstance, M3Paths.List, processorSupport))
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(coreInstance, M3Properties.values, processorSupport);
            return values.collect(x -> pureToPlanValue(x, processorSupport));
        }

        throw new UnsupportedOperationException("Cannot handle value: " + coreInstance);
    }
}
