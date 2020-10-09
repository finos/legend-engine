// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.validation;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

class FunctionParameterProcessor
{
    public static void processParameters(RichIterable<Variable> functionParameters, ExecutionState executionState)
    {
        getAllVariablesProvidedAsStream(functionParameters.toList(), executionState).forEach((Variable param) -> processStreamParameter(param, executionState));
    }

    private static RichIterable<Variable> getAllVariablesProvidedAsStream(RichIterable<Variable> functionParameters, ExecutionState executionState)
    {
        return functionParameters.select(param ->
        {
            Result result = executionState.getResult(param.name);
            return result instanceof ConstantResult && ((ConstantResult) result).getValue() instanceof Stream;
        });
    }

    private static void processStreamParameter(Variable param, ExecutionState executionState)
    {
        String paramName = param.name;
        try
        {
            Stream paramValue = (Stream) ((ConstantResult) executionState.getResult(paramName)).getValue();
            Stream updatedParamValue = paramValue.map(val ->
            {
                ValidationResult validationResult = FunctionParametersParametersValidation.validate(param, val);
                if (validationResult.isValid())
                {
                    return FunctionParametersNormalizer.normalizeParameterValue(param, val);
                }
                else
                {
                    throw new IllegalArgumentException(validationResult.toString());
                }
            });
            ConstantResult updatedParam = param.supportsStream != null && param.supportsStream ? new ConstantResult(updatedParamValue) : new ConstantResult(updatedParamValue.collect(Collectors.toList()));
            executionState.addResult(paramName, updatedParam);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to process " + param._class + " parameter in plan for " + paramName + ", message: " + e.getMessage());
        }
    }
}
