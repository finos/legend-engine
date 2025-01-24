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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtensionLoader;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ParameterValidationContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ProtocolObjectValidationContext;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

class FunctionParametersNormalizer
{
    static void normalizeParameters(RichIterable<Variable> functionParameters,List<ParameterValidationContext> parameterValidationContexts, ExecutionState executionState)
    {
        if (parameterValidationContexts == null)
        {
            functionParameters.forEach(p -> normalizeParameter(p, null, executionState));
        }
        else
        {
            functionParameters.forEach(p -> normalizeParameter(p, parameterValidationContexts.stream().filter(x -> x.varName.equals(p.name)).findAny().orElse(null), executionState));
        }
    }

    private static void normalizeParameter(Variable parameter, ParameterValidationContext parameterValidationContext, ExecutionState executionState)
    {
        Result paramResult = executionState.getResult(parameter.name);
        if (paramResult instanceof ConstantResult)
        {
            Object paramValue = ((ConstantResult) paramResult).getValue();
            Object normalized = normalizeParameterValue(parameter, parameterValidationContext, paramValue);
            if (normalized != paramValue)
            {
                ConstantResult updatedDateTime = new ConstantResult(normalized);
                executionState.addResult(parameter.name, updatedDateTime);
            }
        }
    }

    public static Object normalizeParameterValue(Variable parameter, ParameterValidationContext parameterValidationContext, Object paramValue)
    {
        if (paramValue == null)
        {
            return null;
        }
        switch (((PackageableType) parameter.genericType.rawType).fullPath)
        {
            case "StrictDate":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeStrictDate);
            }
            case "DateTime":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeDateTime);
            }
            case "Date":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeDate);
            }
            case "Integer":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeInteger);
            }
            case "Float":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeFloat);
            }
            case "Decimal":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeDecimal);
            }
            case "Boolean":
            {
                return normalizeParameterValue(paramValue, FunctionParametersNormalizer::normalizeBoolean);
            }
            default:
            {
                if (parameterValidationContext instanceof ProtocolObjectValidationContext)
                {
                    return normalizeParameterValue(paramValue, x -> normalizeProtocolObjectParameterValue((ProtocolObjectValidationContext) parameterValidationContext, x));
                }
                return paramValue;
            }
        }
    }

    private static Object normalizeProtocolObjectParameterValue(ProtocolObjectValidationContext parameterValidationContext, Object paramValue)
    {
        String protocolObjectClassName = parameterValidationContext.protocolClassName;
        try
        {
            Class<?> protocolObjectClass = ExecutionPlanJavaCompilerExtensionLoader.extensions().stream()
                    .map(ExecutionPlanJavaCompilerExtension::dependencies)
                    .map(map -> map.get(protocolObjectClassName))
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Function Parameter class not found in package dependencies for class:." + protocolObjectClassName));

            if (protocolObjectClass.isInstance(paramValue))
            {
                return paramValue;
            }
            else if (paramValue instanceof String)
            {
                ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                return objectMapper.readValue((String) paramValue, protocolObjectClass);
            }
            else
            {
                throw new IllegalArgumentException("Function Parameter should be of type JSON String or " + protocolObjectClass.getName());
            }
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Object normalizeParameterValue(Object value, Function<Object, ?> normalizer)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Iterable)
        {
            boolean changed = false;
            List<Object> newValues = new ArrayList<>();
            for (Object v : (Iterable<?>) value)
            {
                Object normalized = normalizer.apply(v);
                changed |= (normalized != v);
                newValues.add(normalized);
            }
            return changed ? newValues : value;
        }
        return normalizer.apply(value);
    }

    private static EngineDate normalizeStrictDate(Object value)
    {
        if (value instanceof LocalDate)
        {
            return EngineDate.fromLocalDate((LocalDate) value);
        }
        if (value instanceof String)
        {
            return EngineDate.fromStrictDateString((String) value);
        }
        throw new IllegalArgumentException("Invalid StrictDate value: " + value);
    }

    private static EngineDate normalizeDateTime(Object value)
    {
        if (value instanceof LocalDateTime)
        {
            return EngineDate.fromLocalDateTime((LocalDateTime) value);
        }
        if (value instanceof ZonedDateTime)
        {
            return EngineDate.fromZonedDateTime((ZonedDateTime) value);
        }
        if (value instanceof Instant)
        {
            return EngineDate.fromInstant((Instant) value);
        }
        if (value instanceof String)
        {
            return EngineDate.fromDateTimeString((String) value);
        }
        throw new IllegalArgumentException("Invalid DateTime value: " + value);
    }

    private static EngineDate normalizeDate(Object value)
    {
        if (value instanceof LocalDateTime)
        {
            return EngineDate.fromLocalDateTime((LocalDateTime) value);
        }
        if (value instanceof LocalDate)
        {
            return EngineDate.fromLocalDate((LocalDate) value);
        }
        if (value instanceof ZonedDateTime)
        {
            return EngineDate.fromZonedDateTime((ZonedDateTime) value);
        }
        if (value instanceof Instant)
        {
            return EngineDate.fromInstant((Instant) value);
        }
        if (value instanceof String)
        {
            return EngineDate.fromDateString((String) value);
        }
        throw new IllegalArgumentException("Invalid Date value: " + value);
    }

    private static Object normalizeInteger(Object value)
    {
        if (value instanceof Long)
        {
            return value;
        }
        if (value instanceof Integer)
        {
            return ((Integer) value).longValue();
        }
        if (value instanceof String)
        {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Invalid Integer value: " + value);
    }

    private static Object normalizeFloat(Object value)
    {
        if (value instanceof Float)
        {
            return ((Float) value).doubleValue();
        }
        if (value instanceof Double)
        {
            return value;
        }
        if (value instanceof Integer)
        {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Long)
        {
            return ((Long) value).doubleValue();
        }
        if (value instanceof String)
        {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Invalid Double value: " + value);
    }

    private static Object normalizeDecimal(Object value)
    {
        if (value instanceof BigDecimal)
        {
            return value;
        }
        if (value instanceof String)
        {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("Invalid Decimal value: " + value);
    }

    private static Object normalizeBoolean(Object value)
    {
        if (value instanceof Boolean)
        {
            return value;
        }
        if (value instanceof String)
        {
            return Boolean.valueOf((String) value);
        }
        throw new IllegalArgumentException("Invalid Boolean value: " + value);
    }
}