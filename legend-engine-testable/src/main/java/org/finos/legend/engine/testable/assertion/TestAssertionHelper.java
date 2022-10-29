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

package org.finos.legend.engine.testable.assertion;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.test.runner.shared.JsonNodeComparator;

import java.io.IOException;

public class TestAssertionHelper
{
    public static ObjectMapper buildObjectMapperForJSONComparison()
    {
        return new ObjectMapper()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    }

    protected static AssertionStatus compareAssertionJSON(TestAssertion parentAssertion, String _expected, String _actual) throws IOException
    {
        ObjectMapper objectMapper = buildObjectMapperForJSONComparison();
        JsonNode expectedJsonNode = objectMapper.readTree(_expected.getBytes());
        JsonNode actualJsonNode = objectMapper.readTree(_actual.getBytes());

        AssertionStatus assertionStatus;
        if (JsonNodeComparator.NULL_MISSING_EQUIVALENT.compare(expectedJsonNode, actualJsonNode) == 0)
        {
            assertionStatus = new AssertPass();
        }
        else
        {
            EqualToJsonAssertFail fail = new EqualToJsonAssertFail();
            fail.expected = objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(expectedJsonNode);
            fail.actual = objectMapper.writer(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(actualJsonNode);
            fail.message = "Actual result does not match Expected result";
            assertionStatus = fail;
        }
        assertionStatus.id = parentAssertion.id;
        return assertionStatus;
    }
}
