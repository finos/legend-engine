//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.function.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.extension.TestAssertionEvaluator;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.test.runner.shared.JsonNodeComparator;
import org.finos.legend.engine.testable.function.helper.PrimitiveValueSpecificationToObjectVisitor;

public class FunctionTestAssertionEvaluator implements TestAssertionEvaluator
{
    private Result result;

    public FunctionTestAssertionEvaluator(Result result)
    {
        this.result = result;
    }

    @Override
    public AssertionStatus visit(TestAssertion testAssertion)
    {
        if (testAssertion instanceof EqualTo)
        {
            if (!(result instanceof ConstantResult))
            {
                throw new UnsupportedOperationException("Result type - " + result.getClass().getSimpleName() + " not supported with EqualToJson Assert !!");
            }

            Object expected = ((EqualTo) testAssertion).expected.accept(new PrimitiveValueSpecificationToObjectVisitor());
            Object actual;
            if (((ConstantResult) result).getValue() instanceof PureDate)
            {
                 actual = ((ConstantResult) result).getValue().toString();
            }
            else
            {
                actual = ((ConstantResult) result).getValue();
            }

            AssertionStatus assertionStatus;
            if (expected.equals(actual))
            {
                assertionStatus = new AssertPass();
            }
            else
            {
                AssertFail assertFail = new AssertFail();
                assertFail.message = "Expected : " + expected.toString() + ", Found : " + actual.toString();

                assertionStatus = assertFail;
            }
            assertionStatus.id = testAssertion.id;

            return assertionStatus;
        }
        else if (testAssertion instanceof EqualToJson)
        {
            String actualJson;
            if (result instanceof ConstantResult)
            {
                actualJson = (String) ((ConstantResult) result).getValue();
            }
            else if (result instanceof StreamingResult)
            {
                actualJson = ((StreamingResult) result).flush(((StreamingResult) result).getSerializer(SerializationFormat.PURE));
            }
            else
            {
                throw new UnsupportedOperationException("Result type - " + result.getClass().getSimpleName() + " not supported with EqualToJson Assert !!");
            }

            try
            {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode expected = objectMapper.readTree(((EqualToJson) testAssertion).expected.data.getBytes());
                JsonNode actual = objectMapper.readTree(actualJson.getBytes());

                AssertionStatus assertionStatus;
                if (JsonNodeComparator.NULL_MISSING_EQUIVALENT.compare(expected, actual) == 0)
                {
                    assertionStatus = new AssertPass();
                }
                else
                {
                    EqualToJsonAssertFail fail = new EqualToJsonAssertFail();
                    fail.expected = objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(expected);
                    fail.actual = objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(actual);
                    fail.message = "Actual result does not match Expected result";

                    assertionStatus = fail;
                }
                assertionStatus.id = testAssertion.id;
                return assertionStatus;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            return null;
        }
    }
}
