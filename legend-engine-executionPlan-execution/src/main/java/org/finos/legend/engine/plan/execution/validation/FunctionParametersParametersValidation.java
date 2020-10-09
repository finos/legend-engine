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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.util.Map;
import java.util.stream.Stream;

public class FunctionParametersParametersValidation
{

    public static void validate(RichIterable<Variable> functionParameters, ExecutionState executionState)
    {
        Map<String, Result> providedParameterValues = executionState.getResults();
        validateNoMissingMandatoryParamaters(functionParameters, providedParameterValues);
        validateParameterValues(functionParameters, providedParameterValues);
        FunctionParametersNormalizer.normalizeParameters(functionParameters, executionState);
        FunctionParameterProcessor.processParameters(functionParameters, executionState);
    }

    private static void validateNoMissingMandatoryParamaters(RichIterable<Variable> externalParameters, Map<String, Result> providedParameterValues)
    {
        RichIterable<Variable> missingExternalParameters = externalParameters.select(p -> p.multiplicity.lowerBound > 0 && !providedParameterValues.containsKey(p.name));

        if (!missingExternalParameters.isEmpty())
        {
            throw new IllegalArgumentException("Missing external parameter(s): " + missingExternalParameters.collect(a -> a.name + ":" + a._class + "[" + renderMultiplicity(a.multiplicity) + "]").makeString(","));
        }
    }

    private static String renderMultiplicity(Multiplicity multiplicity)
    {
        return multiplicity.lowerBound == 0 && multiplicity.getUpperBoundInt() == Integer.MAX_VALUE ? "*" : multiplicity.lowerBound == multiplicity.getUpperBoundInt() ? String.valueOf(multiplicity.lowerBound) : multiplicity.lowerBound + ".." + (multiplicity.getUpperBoundInt() == Integer.MAX_VALUE ? "*" : multiplicity.getUpperBoundInt());
    }

    private static void validateParameterValues(RichIterable<Variable> externalParameters, Map<String, Result> providedParameterValues)
    {
        MutableList<ValidationResult> inValidProvidedParameters = externalParameters
                .asLazy()
                .select(ep -> providedParameterValues.containsKey(ep.name))
                .collect(v -> FunctionParametersParametersValidation.validate(v, ((ConstantResult) providedParameterValues.get(v.name)).getValue()))
                .reject(ValidationResult::isValid)
                .toList();
        if (!inValidProvidedParameters.isEmpty())
        {
            throw new IllegalArgumentException(inValidProvidedParameters.makeString("Invalid provided parameter(s): [", ",", "]"));
        }
    }

    public static ValidationResult validate(Variable var, Object value)
    {
        FunctionParameterTypeValidator validator = FunctionParameterTypeValidator.externalParameterTypeValidator(var._class);
        if (validator == null)
        {
            return ValidationResult.errorValidationResult("Unknown external parameter type: " + var._class + ", valid external parameter types: " + FunctionParameterTypeValidator.getExternalParameterTypes().makeString("[", ", ", "]"));
        }
        if (value instanceof Stream)
        {
            return ValidationResult.successValidationResult();
        }
        if (value instanceof Iterable)
        {
            ValidationResult result = LazyIterate.collect((Iterable<?>) value, validator::validate).reject(ValidationResult::isValid).getAny();
            return (result == null) ? ValidationResult.successValidationResult() : result;
        }
        return validator.validate(value);
    }
}
