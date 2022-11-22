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

package org.finos.legend.engine.plan.execution.nodes.helpers.platform;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.finos.legend.engine.plan.dependencies.domain.date.AbstractPureDate;
import org.finos.legend.engine.plan.dependencies.domain.date.LatestDate;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.shared.IResult;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class DefaultExecutionNodeContext implements ExecutionNodeContext
{
    protected final ExecutionState state;
    private final Result childResult;

    public DefaultExecutionNodeContext(ExecutionState state, Result childResult)
    {
        this.state = state;
        this.childResult = childResult;
    }

    public static ExecutionNodeJavaPlatformHelper.ExecutionNodeContextFactory factory()
    {
        return DefaultExecutionNodeContext::new;
    }

    @Override
    public <T> T getResult(String key, Type type)
    {
        Result result = state.getResult(key);
        if (result == null)
        {
            return null;
        }
        else if (type instanceof Class)
        {
            return getResultAsClass((Class<T>) type, result);
        }
        else if (type instanceof ParameterizedType)
        {
            return getResultAsParameterizedType((ParameterizedType) type, result);
        }

        throw new IllegalArgumentException("Unable to convert " + result.getClass().getSimpleName() + " to unknown type " + TypeUtils.toString(type));
    }

    private <T> T getResultAsClass(Class<T> clazz, Result result)
    {
        if (result instanceof ConstantResult)
        {
            Object value = ((ConstantResult) result).getValue();
            boolean isDateType = clazz.equals(AbstractPureDate.class) || clazz.equals(PureDate.class);
            if (clazz.isInstance(value))
            {
                return (T) value;
            }
            else if (isDateType && value instanceof String)
            {
                return (T) PureDate.parsePureDate((String) value);
            }
            else if (isDateType && value instanceof EngineDate)
            {
                return (T) ((EngineDate) value).transformToPureDate();
            }
        }
        throw new IllegalArgumentException("Unable to convert " + result.getClass().getSimpleName() + " to " + TypeUtils.toString(clazz));
    }

    private <T> T getResultAsParameterizedType(ParameterizedType type, Result result)
    {
        if (!(type.getRawType() instanceof Class) && type.getActualTypeArguments().length == 1 && type.getActualTypeArguments()[0] instanceof Class)
        {
            throw new IllegalArgumentException("Invalid parameterized type: " + TypeUtils.toString(type));
        }
        Class<?> rawType = (Class<?>) type.getRawType();
        Class<?> elementType = (Class<?>) type.getActualTypeArguments()[0];

        if (result instanceof ConstantResult)
        {
            Object value = ((ConstantResult) result).getValue();
            if (rawType.isInstance(value))
            {
                return (T) value;
            }
            else if (elementType.isInstance(value))
            {
                if (rawType.equals(List.class))
                {
                    return (T) Collections.singletonList(value);
                }
                else if (rawType.equals(Stream.class))
                {
                    return (T) Stream.of(value);
                }
            }
            else if (value == null)
            {
                if (rawType.equals(List.class))
                {
                    return (T) Collections.emptyList();
                }
                else if (rawType.equals(Stream.class))
                {
                    return (T) Stream.empty();
                }
            }
        }
        else if (result instanceof StreamingObjectResult)
        {
            if (rawType.equals(Stream.class))
            {
                return (T) ((StreamingObjectResult) result).getObjectStream();
            }
        }
        throw new IllegalArgumentException("Unable to convert " + result.getClass().getSimpleName() + " to " + TypeUtils.toString(type));
    }

    @Override
    public Result getResult(String key)
    {
        return state.getResult(key);
    }

    @Override
    public IResult getChildResult()
    {
        return childResult;
    }

    @Override
    public Type listType(Type elementType)
    {
        if (!(elementType instanceof Class))
        {
            throw new IllegalArgumentException("Only handling lists of class types");
        }
        return TypeUtils.parameterize(List.class, elementType);
    }

    @Override
    public Type streamType(Type elementType)
    {
        if (!(elementType instanceof Class))
        {
            throw new IllegalArgumentException("Only handling lists of class types");
        }
        return TypeUtils.parameterize(Stream.class, elementType);
    }
}
