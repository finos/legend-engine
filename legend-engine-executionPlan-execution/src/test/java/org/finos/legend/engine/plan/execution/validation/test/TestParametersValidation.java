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

package org.finos.legend.engine.plan.execution.validation.test;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.finos.legend.engine.plan.execution.validation.FunctionParametersParametersValidation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestParametersValidation
{
    @Test
    public void testStringParameter()
    {
        Function<Object, ?> normalizer = null;
        testRequiredToOneParameter(
                "String",
                Arrays.asList("the quick brown fox", "ABCDE", "5", "6.0", "true"),
                Arrays.asList(5, 6.0, true, false, Instant.now(), LocalDate.now()),
                normalizer
        );
        testToManyParameter(
                "String",
                Arrays.asList(null, Collections.emptyList(), Arrays.asList("a", "b", "c"), Collections.singletonList("the quick brown fox")),
                Arrays.asList(Arrays.asList("a", "b", true), Arrays.asList(5, 6.0, "string", false)),
                normalizer
        );
    }

    @Test
    public void testBooleanParameter()
    {
        Function<Object, ?> normalizer = null;
        testRequiredToOneParameter(
                "Boolean",
                Arrays.asList(true, false, "true", "false"),
                Arrays.asList(5, 6.0, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now()),
                normalizer
        );
        testToManyParameter(
                "Boolean",
                Arrays.asList(null, Collections.emptyList(), Arrays.asList(true, true), Arrays.asList(true, "false"), Collections.singletonList(false)),
                Arrays.asList(Arrays.asList(true, "b"), Arrays.asList("c", 5, "e")),
                normalizer
        );
    }

    @Test
    public void testIntegerParameter()
    {
        Function<Object, ?> normalizer = x -> (x instanceof Integer) ? ((Integer) x).longValue() : x;
        testRequiredToOneParameter(
                "Integer",
                Arrays.asList(5, 6L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE, "-1", "5", "6"),
                Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now()),
                normalizer
        );
        testToManyParameter(
                "Integer",
                Arrays.asList(null, Collections.emptyList(), Arrays.asList(1, "2", 3L), Collections.singletonList(6L), Arrays.asList(Long.MAX_VALUE, Long.MIN_VALUE)),
                Arrays.asList(Arrays.asList(1, 2, "b"), Arrays.asList("c", false)),
                normalizer
        );
    }

    @Test
    public void testFloatParameter()
    {
        Function<Object, ?> normalizer = x -> (x instanceof Float) ? ((Float) x).doubleValue() : x;
        testRequiredToOneParameter(
                "Float",
                Arrays.asList(5.0, 6.12d, -2.71f, Double.MAX_VALUE, Double.MIN_VALUE, Float.MAX_VALUE, "-1.0", "5.234", "678978678", 5, 4, Long.MAX_VALUE),
                Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now()),
                normalizer
        );
        testToManyParameter(
                "Float",
                Arrays.asList(null, Collections.emptyList(), Arrays.asList("5.0", 6.12d, -2.71f), Collections.singletonList(0.0), Arrays.asList(Double.MAX_VALUE, Double.MIN_VALUE)),
                Arrays.asList(Arrays.asList(5.0, 6.12d, "c"), Arrays.asList(false, true, "a", "B")),
                normalizer
        );
    }

    @Test
    public void testDateParameter()
    {
        testRequiredToOneParameter(
                "Date",
                Arrays.asList(Instant.now(), LocalDate.now(), LocalDateTime.now(), ZonedDateTime.now(), "2020-07-14", "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23-0300"),
                Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3),
                this::normalizeDate,
                "Expected formats: [yyyy-MM-dd,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ssZ]"
        );
        testToManyParameter(
                "Date",
                Arrays.asList(null, Collections.emptyList(), Collections.singletonList(Instant.now()), Arrays.asList(Instant.now(), LocalDate.now()), Arrays.asList(ZonedDateTime.now(), "2020-07-14")),
                Arrays.asList(Arrays.asList(5, 2, 3), Arrays.asList(Instant.now(), 2)),
                this::normalizeDate
        );
    }

    @Test
    public void testStrictDateParameter()
    {
        testRequiredToOneParameter(
                "StrictDate",
                Arrays.asList(LocalDate.now(), "2020-07-14"),
                Arrays.asList(Instant.now(), LocalDateTime.now(), ZonedDateTime.now(), true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3, "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123"),
                this::normalizeDate,
                "Expected formats: [yyyy-MM-dd]"
        );
        testToManyParameter(
                "StrictDate",
                Arrays.asList(null, Collections.emptyList(), Collections.singletonList(LocalDate.now()), Arrays.asList(LocalDate.now(), "2020-07-14"), Arrays.asList("2020-07-14", "2020-08-06")),
                Arrays.asList(Arrays.asList(5, 2, "c", false), Arrays.asList(LocalDate.now(), ZonedDateTime.now())),
                this::normalizeDate
        );
    }

    @Test
    public void testDateTimeParameter()
    {
        testRequiredToOneParameter(
                "DateTime",
                Arrays.asList(Instant.now(), LocalDateTime.now(), ZonedDateTime.now(), "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23.123-0300", "2020-07-14T15:18:23.123+0500"),
                Arrays.asList(LocalDate.now(), true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3, "2020-07-14"),
                this::normalizeDate,
                "Expected formats: [yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ssZ]"
        );
        testToManyParameter(
                "DateTime",
                Arrays.asList(null, Collections.emptyList(), Arrays.asList(ZonedDateTime.now(), Instant.now()), Collections.singletonList(ZonedDateTime.now()), Arrays.asList("2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23.123-0300", "2020-07-14T15:18:23.123+0500")),
                Arrays.asList(Arrays.asList(5, "b", 3), Arrays.asList(ZonedDateTime.now(), "b"), Arrays.asList(LocalDate.now(), ZonedDateTime.now())),
                this::normalizeDate
        );
    }

    private EngineDate normalizeDate(Object value)
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
        throw new IllegalArgumentException(String.valueOf(value));
    }

    private void testRequiredToOneParameter(String cls, List<?> validValues, List<?> invalidValues, Function<Object, ?> normalizer)
    {
        testRequiredToOneParameter(cls, validValues, invalidValues, normalizer, null);
    }

    private void testRequiredToOneParameter(String cls, List<?> validValues, List<?> invalidValues, Function<Object, ?> normalizer, String exceptionSuffix)
    {
        Variable parameter = newVariable("p", cls, 1, 1);

        for (Object validValue : validValues)
        {
            ExecutionState state = newExecutionState(parameter.name, validValue);
            FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), state);
            Object actualValue = ((ConstantResult) state.getResult(parameter.name)).getValue();
            if (normalizer == null)
            {
                Assert.assertSame(String.valueOf(validValue), validValue, actualValue);
            }
            else
            {
                Assert.assertEquals(String.valueOf(validValue), normalizer.apply(validValue), actualValue);
            }
        }

        for (Object invalidValue : invalidValues)
        {
            IllegalArgumentException e = Assert.assertThrows(String.valueOf(invalidValue), IllegalArgumentException.class, () -> FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), newExecutionState(parameter.name, invalidValue)));
            Assert.assertEquals(getExpectedExceptionMessage(cls, invalidValue, exceptionSuffix), e.getMessage());
        }
    }

    private void testToManyParameter(String cls, List<? extends List<?>> validValues, List<?> invalidValues, Function<Object, ?> normalizer)
    {
        Variable parameter = newVariable("p", cls, 0, null);

        for (List<?> validValue : validValues)
        {
            ExecutionState state = newExecutionState(parameter.name, validValue);
            FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), state);
            Object actualValue = ((ConstantResult) state.getResult(parameter.name)).getValue();
            if ((normalizer == null) || (validValue == null))
            {
                Assert.assertSame(String.valueOf(validValue), validValue, actualValue);
            }
            else
            {
                Assert.assertEquals(String.valueOf(validValue), validValue.stream().map(normalizer).collect(Collectors.toList()), actualValue);
            }
        }

        for (Object invalidValue : invalidValues)
        {
            IllegalArgumentException e = Assert.assertThrows(String.valueOf(invalidValue), IllegalArgumentException.class, () -> FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), newExecutionState(parameter.name, invalidValue)));
            String expectedPrefix = getExpectedExceptionMessagePrefix(cls);
            String message = e.getMessage();
            if ((message == null) || !message.startsWith(expectedPrefix))
            {
                Assert.fail("Expected message to start with \"" + expectedPrefix + "\", got: " + message);
            }
        }
    }

    private Variable newVariable(String name, String cls, int lowerBound, Integer upperBound)
    {
        return new Variable(name, cls, new Multiplicity(lowerBound, upperBound));
    }

    private ExecutionState newExecutionState(String parameterName, Object parameterValue)
    {
        Map<String, Result> results = new HashMap<>();
        results.put(parameterName, new ConstantResult(parameterValue));
        return newExecutionState(results);
    }

    private ExecutionState newExecutionState(Map<String, Result> paramMap)
    {
        return new ExecutionState(paramMap, Collections.emptyList(), Collections.emptyList());
    }

    private StringBuilder getExpectedExceptionMessagePrefixBuilder(String cls)
    {
        return new StringBuilder("Invalid provided parameter(s): [Unable to process '").append(cls).append("' parameter, value: ");
    }

    private String getExpectedExceptionMessagePrefix(String cls)
    {
        return getExpectedExceptionMessagePrefixBuilder(cls).toString();
    }

    private String getExpectedExceptionMessage(String cls, Object value, String suffix)
    {
        StringBuilder builder = getExpectedExceptionMessagePrefixBuilder(cls);
        if (value instanceof String)
        {
            builder.append("'").append(value).append("'");
            if (!"String".equals(cls))
            {
                builder.append(" is not parsable");
            }
        }
        else
        {
            builder.append(value);
        }
        builder.append(".");
        if (suffix != null)
        {
            builder.append(" ").append(suffix);
        }
        builder.append("]");
        return builder.toString();
    }
}
