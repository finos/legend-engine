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

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.EnumValidationContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ParameterValidationContextVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.util.List;

public class ParameterValidationContextExecutor implements ParameterValidationContextVisitor<ValidationResult>
{
    private final Variable var;
    private final Object value;

    public ParameterValidationContextExecutor(Variable var, Object value)
    {
        this.var = var;
        this.value = value;
    }

    @Override
    public ValidationResult visit(EnumValidationContext enumValidationContext)
    {
        List<String> validEnumValues = enumValidationContext.validEnumValues;
        if (value instanceof List)
        {
            throw new IllegalArgumentException("Collection of Enums (" + value + ") is not supported as service parameter");
        }
        return (validEnumValues.contains(value.toString())) ? ValidationResult.successValidationResult() : ValidationResult.errorValidationResult("Invalid enum value " + value + " for " + var._class + ", valid enum values: " + validEnumValues);
    }
}