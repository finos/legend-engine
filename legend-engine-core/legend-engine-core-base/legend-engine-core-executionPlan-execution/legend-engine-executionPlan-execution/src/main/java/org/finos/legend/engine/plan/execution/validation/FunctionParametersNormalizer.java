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
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class FunctionParametersNormalizer
{
    static void normalizeParameters(RichIterable<Variable> functionParameters, ExecutionState executionState)
    {
        functionParameters.forEach(p -> normalizeParameter(p, executionState));
    }

    private static void normalizeParameter(Variable parameter, ExecutionState executionState)
    {
        Result paramResult = executionState.getResult(parameter.name);
        if (paramResult instanceof ConstantResult)
        {
            Object paramValue = ((ConstantResult) paramResult).getValue();
            Object normalized = normalizeParameterValue(parameter, paramValue);
            if (normalized != paramValue)
            {
                ConstantResult updatedDateTime = new ConstantResult(normalized);
                executionState.addResult(parameter.name, updatedDateTime);
            }
        }
    }

    public static Object normalizeParameterValue(Variable parameter, Object paramValue)
    {
        if (paramValue == null)
        {
            return null;
        }
        switch (parameter._class)
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
            default:
            {
                return paramValue;
            }
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
        return (value instanceof Integer) ? ((Integer) value).longValue() : value;
    }

    private static Object normalizeFloat(Object value)
    {
        return (value instanceof Float) ? ((Float) value).doubleValue() : value;
    }
}