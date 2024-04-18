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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.util.List;
import java.util.Optional;

public class ServicePatternValidation
{
    private static final Function<String, String> ADD_QUOTES = string -> "'" + string + "'";

    public static void validateServicePattern(RichIterable<String> servicePatternPathParameters, List<Variable> functionParameters)
    {
        Optional<IllegalArgumentException> nonExistentServicePatternParameters = validateServicePatternParametersExistInFunctionParameterSpecification(servicePatternPathParameters, functionParameters);
        Optional<IllegalArgumentException> optionalServicePatternParameters = validateServicePatternReferencesMandatoryPropertiesOnly(servicePatternPathParameters, functionParameters);

        RichIterable<IllegalArgumentException> exceptions = Lists.immutable.with(nonExistentServicePatternParameters, optionalServicePatternParameters).select(o -> o.isPresent()).collect(o -> o.get());
        if (!exceptions.isEmpty())
        {
            throw new IllegalArgumentException(exceptions.collect(Throwable::getMessage).makeString(","));
        }
    }

    private static Optional<IllegalArgumentException> validateServicePatternParametersExistInFunctionParameterSpecification(RichIterable<String> servicePatternPathParameters, List<Variable> functionParameters)
    {
        RichIterable<String> functionParametersByName = ListIterate.collect(functionParameters, fp -> fp.name);
        RichIterable<String> serviceParametersForNonExistantFunctionParamaters = servicePatternPathParameters.select(sp -> !functionParametersByName.contains(sp));
        return serviceParametersForNonExistantFunctionParamaters.isEmpty() ? Optional.empty() : Optional.of(new IllegalArgumentException("Service pattern references non existant function parameters: " + serviceParametersForNonExistantFunctionParamaters.collect(ADD_QUOTES).makeString(", ")));
    }

    private static Optional<IllegalArgumentException> validateServicePatternReferencesMandatoryPropertiesOnly(RichIterable<String> servicePatternPathParameters, List<Variable> functionParameters)
    {
        RichIterable<String> optionalNonInfiniteFunctionParameters = ListIterate.select(functionParameters, fp -> fp.multiplicity.lowerBound == 0 && !fp.multiplicity.isInfinite()).collect(v -> v.name);
        RichIterable<String> optionalServicePathParamers = servicePatternPathParameters.select(sp -> optionalNonInfiniteFunctionParameters.contains(sp));
        return optionalServicePathParamers.isEmpty() ? Optional.empty() : Optional.of(new IllegalArgumentException("Service pattern references optional function parameters: " + optionalServicePathParamers.collect(ADD_QUOTES).makeString(", ")));
    }
}
