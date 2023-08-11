// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.nodes.helpers;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecuteNodeParameterTransformationHelper
{
    public static void buildParameterToConstantResult(SingleExecutionPlan plan, Map<String, ?> parameterToValues, MutableMap<String, Result> parametersToConstantResult)
    {
        List<Variable> functionParameters = plan.rootExecutionNode.executionNodes.stream().filter(node -> node instanceof FunctionParametersValidationNode).map(executionNode -> ((FunctionParametersValidationNode) executionNode).functionParameters).flatMap(Collection::stream).collect(Collectors.toList());
        Multiplicity paraMultiplicity = new Multiplicity();
        for (Map.Entry<String, ?> parameterToValue : parameterToValues.entrySet())
        {
            paraMultiplicity = getFunctionParameterMultiplicity(functionParameters, paraMultiplicity, parameterToValue.getKey());
            Object parameterValue = parameterToValue.getValue();
            boolean isSingularListValue = parameterValue instanceof FastList && ((FastList) parameterValue).size() == 1 && paraMultiplicity.isUpperBoundEqualTo(1);
            if (isSingularListValue)
            {
                parametersToConstantResult.put(parameterToValue.getKey(), new ConstantResult(((FastList) parameterValue).getFirst()));
            }
            else
            {
                parametersToConstantResult.put(parameterToValue.getKey(), new ConstantResult(parameterValue));
            }
        }
    }

    private static Multiplicity getFunctionParameterMultiplicity(List<Variable> functionParameters, Multiplicity paraMultiplicity, String parameterName)
    {
        for (Variable var : functionParameters)
        {
            if (var.name.equals(parameterName))
            {
                paraMultiplicity = var.multiplicity;
                break;
            }
        }
        return paraMultiplicity;
    }
}
