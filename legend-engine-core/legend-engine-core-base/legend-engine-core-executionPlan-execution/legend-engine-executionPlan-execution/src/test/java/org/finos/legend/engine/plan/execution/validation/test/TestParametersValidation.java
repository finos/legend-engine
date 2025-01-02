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

import com.google.common.collect.Streams;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.date.EngineDate;
import org.finos.legend.engine.plan.execution.validation.FunctionParametersParametersValidation;
import org.finos.legend.engine.protocol.pure.v1.model.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestParametersValidation
{
    @Test
    public void testStringParameter()
    {
        testRequiredToOneParameter(
            "String",
            Arrays.asList("the quick brown fox", "ABCDE", "5", "6.0", "true"),
            Arrays.asList("the quick brown fox", "ABCDE", "5", "6.0", "true"),
            Arrays.asList(5, 6.0, true, false, Instant.now(), LocalDate.now())
        );
        testToManyParameter(
            "String",
            Arrays.asList(null, Collections.emptyList(), Arrays.asList("a", "b", "c"), Collections.singletonList("the quick brown fox")),
            Arrays.asList(null, Collections.emptyList(), Arrays.asList("a", "b", "c"), Collections.singletonList("the quick brown fox")),
            Arrays.asList(Arrays.asList("a", "b", true), Arrays.asList(5, 6.0, "string", false))
        );
    }

    @Test
    public void testBooleanParameter()
    {
        testRequiredToOneParameter(
            "Boolean",
            Arrays.asList(true, false, "true", "false"),
            Arrays.asList(true, false, true, false),
            Arrays.asList(5, 6.0, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now())
        );
        testToManyParameter(
            "Boolean",
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(true, true), Arrays.asList(true, "false"), Collections.singletonList(false)),
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(true, true), Arrays.asList(true, false), Collections.singletonList(false)),
            Arrays.asList(Arrays.asList(true, "b"), Arrays.asList("c", 5, "e"))
        );
    }

    @Test
    public void testIntegerParameter()
    {
        testRequiredToOneParameter(
            "Integer",
            Arrays.asList(5, 6L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE, "-1", "5", "6"),
            Arrays.asList(5L, 6L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, ((Integer)(Integer.MAX_VALUE)).longValue(), -1L, 5L, 6L),
            Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now())
        );
        testToManyParameter(
            "Integer",
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(1, "2", 3L), Collections.singletonList(6L), Arrays.asList(Long.MAX_VALUE, Long.MIN_VALUE)),
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(1L, 2L, 3L), Collections.singletonList(6L), Arrays.asList(Long.MAX_VALUE, Long.MIN_VALUE)),
            Arrays.asList(Arrays.asList(1, 2, "b"), Arrays.asList("c", false))
        );
    }

    @Test
    public void testDecimalParameter()
    {
        testRequiredToOneParameter(
            "Decimal",
            Arrays.asList(BigDecimal.valueOf(3.14d), BigDecimal.valueOf(5L), "-1.23", "2.73"),
            Arrays.asList(BigDecimal.valueOf(3.14d), BigDecimal.valueOf(5L), BigDecimal.valueOf(-1.23), BigDecimal.valueOf(2.73)),
            Arrays.asList(5L, 2.73d, 1.23f, true, false, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now())
        );
        testToManyParameter(
            "Decimal",
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(BigDecimal.valueOf(3.14d)), Arrays.asList(BigDecimal.valueOf(5L), "-1.23")),
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(BigDecimal.valueOf(3.14d)), Arrays.asList(BigDecimal.valueOf(5L), BigDecimal.valueOf(-1.23))),
            Arrays.asList(Arrays.asList(1, 2, "b"), Arrays.asList("c", false, 5L))
        );
    }

    @Test
    public void testFloatParameter()
    {
        testRequiredToOneParameter(
            "Float",
            Arrays.asList(5.0, 6.12d, Double.MAX_VALUE, Double.MIN_VALUE, "-1.0", "5.234", "678978678", 5, 4, Long.MAX_VALUE),
            Arrays.asList(5.0d, 6.12d, Double.MAX_VALUE, Double.MIN_VALUE, -1.0d, 5.234d, 678978678d, 5d, 4d, ((Long) Long.MAX_VALUE).doubleValue()),
            Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", Instant.now(), LocalDate.now())
        );
        testToManyParameter(
            "Float",
            Arrays.asList(null, Collections.emptyList(), Arrays.asList("5.0", 6.12d, -2.71), Collections.singletonList(0.0), Arrays.asList(Double.MAX_VALUE, Double.MIN_VALUE)),
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(5.0d, 6.12d, -2.71d), Collections.singletonList(0.0d), Arrays.asList(Double.MAX_VALUE, Double.MIN_VALUE)),
            Arrays.asList(Arrays.asList(5.0, 6.12d, "c"), Arrays.asList(false, true, "a", "B"))
        );
    }

    @Test
    public void testDateParameter()
    {
        LocalDate today = LocalDate.now();
        EngineDate engineToday = EngineDate.fromLocalDate(today);
        Instant now = Instant.now();
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        EngineDate engineNow = EngineDate.fromInstant(now);

        testRequiredToOneParameter(
            "Date",
            Arrays.asList(now, today, LocalDateTime.ofInstant(now, ZoneOffset.UTC), zonedNow, "2020-07-14", "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23-0300"),
            Arrays.asList(engineNow, engineToday, engineNow, engineNow, EngineDate.fromDateString("2020-07-14"), EngineDate.fromDateTimeString("2020-07-14 15:18:23"), EngineDate.fromDateTimeString("2020-07-14 15:18:23"), EngineDate.fromDateTimeString("2020-07-14 15:18:23.992"), EngineDate.fromDateTimeString("2020-07-14 15:18:23.123"), EngineDate.fromDateTimeString("2020-07-14 18:18:23")),
            Arrays.asList(true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3),
            "Expected formats: [yyyy-MM-dd,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ssZ]"
        );
        testToManyParameter(
            "Date",
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(now), Arrays.asList(now, today), Arrays.asList(zonedNow, "2020-07-14")),
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(engineNow), Arrays.asList(engineNow, engineToday), Arrays.asList(engineNow, EngineDate.fromDateString("2020-07-14"))),
            Arrays.asList(Arrays.asList(5, 2, 3), Arrays.asList(Instant.now(), 2))
        );
    }

    @Test
    public void testStrictDateParameter()
    {
        LocalDate today = LocalDate.now();
        EngineDate engineToday = EngineDate.fromLocalDate(today);

        testRequiredToOneParameter(
            "StrictDate",
            Arrays.asList(today, "2020-07-14"),
            Arrays.asList(engineToday, EngineDate.fromDateString("2020-07-14")),
            Arrays.asList(Instant.now(), LocalDateTime.now(), ZonedDateTime.now(), true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3, "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123"),
            "Expected formats: [yyyy-MM-dd]"
        );
        testToManyParameter(
            "StrictDate",
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(today), Arrays.asList(today, "2020-07-14"), Arrays.asList("2020-07-14", "2020-08-06")),
            Arrays.asList(null, Collections.emptyList(), Collections.singletonList(engineToday), Arrays.asList(engineToday, EngineDate.fromDateString("2020-07-14")), Arrays.asList(EngineDate.fromDateString("2020-07-14"),EngineDate.fromDateString("2020-08-06"))),
            Arrays.asList(Arrays.asList(5, 2, "c", false), Arrays.asList(LocalDate.now(), ZonedDateTime.now()))
        );
    }

    @Test
    public void testDateTimeParameter()
    {
        Instant now = Instant.now();
        ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        EngineDate engineNow = EngineDate.fromInstant(now);

        testRequiredToOneParameter(
            "DateTime",
            Arrays.asList(now, LocalDateTime.ofInstant(now, ZoneOffset.UTC), zonedNow, "2020-07-14 15:18:23", "2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23.123-0300", "2020-07-14T15:18:23.123+0500"),
            Arrays.asList(engineNow, engineNow, engineNow, EngineDate.fromDateTimeString("2020-07-14 15:18:23"), EngineDate.fromDateTimeString("2020-07-14 15:18:23"), EngineDate.fromDateTimeString("2020-07-14 15:18:23.992"), EngineDate.fromDateTimeString("2020-07-14 15:18:23.123"), EngineDate.fromDateTimeString("2020-07-14 18:18:23.123"), EngineDate.fromDateTimeString("2020-07-14 10:18:23.123")),
            Arrays.asList(LocalDate.now(), true, false, "the quick brown fox", "jumped over the lazy dog", 4.2, -3.14, 5, 4, 3, "2020-07-14"),
            "Expected formats: [yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss.SSS,yyyy-MM-dd HH:mm:ss,yyyy-MM-dd'T'HH:mm:ss.SSSZ,yyyy-MM-dd'T'HH:mm:ssZ]"
        );
        testToManyParameter(
            "DateTime",
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(zonedNow, now), Collections.singletonList(zonedNow), Arrays.asList("2020-07-14T15:18:23", "2020-07-14 15:18:23.992", "2020-07-14T15:18:23.123", "2020-07-14T15:18:23.123-0300", "2020-07-14T15:18:23.123+0500")),
            Arrays.asList(null, Collections.emptyList(), Arrays.asList(engineNow, engineNow), Collections.singletonList(engineNow), Arrays.asList(EngineDate.fromDateTimeString("2020-07-14T15:18:23"), EngineDate.fromDateTimeString("2020-07-14 15:18:23.992"), EngineDate.fromDateTimeString("2020-07-14T15:18:23.123"), EngineDate.fromDateTimeString("2020-07-14T15:18:23.123-0300"), EngineDate.fromDateTimeString("2020-07-14T15:18:23.123+0500"))),
            Arrays.asList(Arrays.asList(5, "b", 3), Arrays.asList(ZonedDateTime.now(), "b"), Arrays.asList(LocalDate.now(), ZonedDateTime.now()))
        );
    }

    private void testRequiredToOneParameter(String cls, List<?> validValues, List<?> expectedValues, List<?> invalidValues)
    {
        testRequiredToOneParameter(cls, validValues, expectedValues, invalidValues, null);
    }

    private void testRequiredToOneParameter(String cls, List<?> validValues, List<?> expectedValues, List<?> invalidValues, String exceptionSuffix)
    {
        Variable parameter = newVariable("p", cls, 1, 1);

        Streams.zip(validValues.stream(), expectedValues.stream(), (validValue, expectedValue) ->
        {
            ExecutionState state = newExecutionState(parameter.name, validValue);
            FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), state);
            Object actualValue = ((ConstantResult) state.getResult(parameter.name)).getValue();
            Assert.assertEquals(String.valueOf(validValue), expectedValue, actualValue);
            return true;
        }).collect(Collectors.toList());

        for (Object invalidValue : invalidValues)
        {
            IllegalArgumentException e = Assert.assertThrows(String.valueOf(invalidValue), IllegalArgumentException.class, () -> FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), newExecutionState(parameter.name, invalidValue)));
            Assert.assertEquals(getExpectedExceptionMessage(cls, invalidValue, exceptionSuffix), e.getMessage());
        }
    }
    
    private void testToManyParameter(String cls, List<? extends List<?>> validValues, List<? extends List<?>> expectedValues, List<?> invalidValues)
    {
        Variable parameter = newVariable("p", cls, 0, null);

        Streams.zip(validValues.stream(), expectedValues.stream(), (validValue, expectedValue) ->
        {
            ExecutionState state = newExecutionState(parameter.name, validValue);
            FunctionParametersParametersValidation.validate(Lists.immutable.with(parameter), Collections.emptyList(), state);
            Object actualValue = ((ConstantResult) state.getResult(parameter.name)).getValue();
            Assert.assertEquals(String.valueOf(validValue), expectedValue, actualValue);
            return null;
        }).collect(Collectors.toList());

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
