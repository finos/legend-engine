// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.test.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class TestJsonStreamingResult
{
    @Test
    public void doesNotAutocloseJsonArraysForDefaultFormat() throws Exception
    {
        doesNotAutocloseJsonArrays(SerializationFormat.DEFAULT);
    }

    @Test
    public void doesNotAutocloseJsonArraysForPureFormat() throws Exception
    {
        doesNotAutocloseJsonArrays(SerializationFormat.PURE);
    }

    private void doesNotAutocloseJsonArrays(SerializationFormat format) throws Exception
    {
        JsonStreamingResult result = new JsonStreamingResult(new JsonStreamingResult.JsonStreamHandler()
        {
            @Override
            public void writeTo(JsonGenerator x)
            {
                try
                {
                    x.writeStartArray();
                    // throw before closing array to mimic streaming error
                    throw new TestException();
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Stream<ObjectNode> toStream()
            {
                return null;
            }
        });

        Serializer serializer = result.getSerializer(format);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try
        {
            serializer.stream(outputStream);
        }
        catch (TestException e)
        {
            try
            {
                new ObjectMapper().readTree(outputStream.toByteArray());
                Assert.fail("Serialized bytes should not contains valid JSON.  Is Jackson autoclosing arrays and objects?");
            }
            catch (JsonEOFException eofException)
            {
                assertThat(eofException, hasMessage(CoreMatchers.startsWith("Unexpected end-of-input: expected close marker for Array")));
            }
        }
    }

    private static class TestException extends RuntimeException
    {

    }
}
