// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.shared.core.test;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class TestObjectMapperFactoryStreamLimits
{
    private static final int BEYOND_DEFAULT_MAX_DEPTH = 3 * StreamReadConstraints.DEFAULT_MAX_DEPTH;

    @Test
    public void standardMapperReadsAndWritesJsonNestedBeyondJacksonDefaultDepth() throws Exception
    {
        ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapper();
        String json = nestedArrays(BEYOND_DEFAULT_MAX_DEPTH);

        JsonNode node = mapper.readTree(json);

        Assertions.assertEquals(json, mapper.writeValueAsString(node));
    }

    @Test
    public void standardMapperReadsStringLongerThanJacksonDefaultMaxStringLength() throws Exception
    {
        String value = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 1, "a"));

        String parsed = ObjectMapperFactory.getNewStandardObjectMapper().readValue("\"" + value + "\"", String.class);

        Assertions.assertEquals(value.length(), parsed.length());
    }

    @Test
    public void standardMapperReadsNumberLongerThanJacksonDefaultMaxNumberLength() throws Exception
    {
        String digits = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_NUM_LEN + 1, "7"));

        JsonNode node = ObjectMapperFactory.getNewStandardObjectMapper().readTree("{\"n\":" + digits + "}");

        Assertions.assertEquals(digits, node.get("n").bigIntegerValue().toString());
    }

    @Test
    public void standardMapperReadsPropertyNameLongerThanJacksonDefaultMaxNameLength() throws Exception
    {
        String name = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_NAME_LEN + 1, "p"));

        JsonNode node = ObjectMapperFactory.getNewStandardObjectMapper().readTree("{\"" + name + "\":1}");

        Assertions.assertEquals(1, node.get(name).intValue());
    }

    @Test
    public void protocolMapperReadsLambdaWithMoreNestedCallsThanJacksonDefaultDepthAllows() throws Throwable
    {
        String body = "{\"_type\":\"boolean\",\"value\":true}";
        for (int i = 0; i < StreamReadConstraints.DEFAULT_MAX_DEPTH; i++)
        {
            body = "{\"_type\":\"func\",\"function\":\"or\",\"parameters\":[" + body + ",{\"_type\":\"boolean\",\"value\":false}]}";
        }
        String json = "{\"_type\":\"lambda\",\"body\":[" + body + "],\"parameters\":[]}";
        ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        // Databind recurses per nesting level, so with no depth cap the default thread stack is the limit
        LambdaFunction lambda = onThreadWithLargeStack(() -> mapper.readValue(json, LambdaFunction.class));

        Assertions.assertEquals(1, lambda.body.size());
    }

    @Test
    public void unlimitedDefaultsApplyToMappersCreatedOutsideTheFactory() throws Exception
    {
        try
        {
            ObjectMapperFactory.useUnlimitedStreamConstraintsByDefault();
            ObjectMapper mapper = new ObjectMapper();
            String json = nestedArrays(BEYOND_DEFAULT_MAX_DEPTH);

            Assertions.assertEquals(json, mapper.writeValueAsString(mapper.readTree(json)));
        }
        finally
        {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
            StreamWriteConstraints.overrideDefaultStreamWriteConstraints(null);
        }
    }

    private static <T> T onThreadWithLargeStack(Callable<T> callable) throws Throwable
    {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(null, () ->
        {
            try
            {
                result.set(callable.call());
            }
            catch (Throwable t)
            {
                failure.set(t);
            }
        }, "large-stack", 256L * 1024 * 1024);
        thread.start();
        thread.join();
        if (failure.get() != null)
        {
            throw failure.get();
        }
        return result.get();
    }

    private static String nestedArrays(int depth)
    {
        return String.join("", Collections.nCopies(depth, "[")) + String.join("", Collections.nCopies(depth, "]"));
    }
}
