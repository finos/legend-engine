// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.service.assertion;

import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.finos.legend.engine.testable.service.helper.PrimitiveValueSpecificationToObjectVisitor;

public class ServiceTestAssertionEvaluator implements TestAssertionEvaluator
{
    private final String serializationFormat;
    private final Result result;

    public ServiceTestAssertionEvaluator(Result result, String serializationFormat)
    {
        this.result = result;
        this.serializationFormat = serializationFormat;

    }

    public ServiceTestAssertionEvaluator(Result result)
    {
        this(result, null);
    }


    private SerializationFormat getSerializationFormat()
    {
        if (this.serializationFormat == null)
        {
            return SerializationFormat.defaultFormat;
        }
        try
        {
            return SerializationFormat.valueOf(this.serializationFormat);
        }
        catch (IllegalArgumentException exception)
        {
            throw new    UnsupportedOperationException("Unsupported serialization format '" + this.serializationFormat
                + "'." + "Supported formats are:" + Stream.of(SerializationFormat.values()).map(SerializationFormat::name).collect(Collectors.joining(",")));
        }
    }


    @Override
    public AssertionStatus visit(TestAssertion testAssertion)
    {
        SerializationFormat serializationFormat = this.getSerializationFormat();
        if (testAssertion instanceof EqualTo)
        {
            if (!(result instanceof ConstantResult))
            {
                throw new UnsupportedOperationException("Result type - " + result.getClass().getSimpleName() + " not supported with EqualToJson Assert !!");
            }

            Object expected = ((EqualTo) testAssertion).expected.accept(new PrimitiveValueSpecificationToObjectVisitor());
            Object actual = ((ConstantResult) result).getValue();

            AssertionStatus assertionStatus;
            if (expected.equals(actual))
            {
                assertionStatus = new AssertPass();
            }
            else
            {
                AssertFail assertFail = new AssertFail();
                assertFail.message = "Expected : " + expected + ", Found : " + actual.toString();

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
                actualJson = ((StreamingResult) result).flush(((StreamingResult) result).getSerializer(serializationFormat));
            }
            else
            {
                throw new UnsupportedOperationException("Result type - " + result.getClass().getSimpleName() + " not supported with EqualToJson Assert !!");
            }
            try
            {
                return ServiceTestAssertionHelper.compareAssertionJSON(testAssertion, ((EqualToJson) testAssertion).expected.data, actualJson);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
