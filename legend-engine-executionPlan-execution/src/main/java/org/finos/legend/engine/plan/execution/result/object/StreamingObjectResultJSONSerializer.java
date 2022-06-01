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

package org.finos.legend.engine.plan.execution.result.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.collections.impl.Counter;
import org.finos.legend.engine.plan.execution.result.serialization.ExecutionResultObjectMapperFactory;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

public class StreamingObjectResultJSONSerializer extends Serializer
{
    private final ObjectMapper objectMapper = ExecutionResultObjectMapperFactory.getNewObjectMapper();
    private final StreamingObjectResult streamingObjectResult;
    private final byte[] b_builder = "{\"builder\": ".getBytes();
    private final byte[] b_activities = ", \"activities\": [".getBytes();
    private final byte[] b_objects = "], \"objects\" : [".getBytes();
    private final byte[] b_comma = ",".getBytes();
    private final byte[] b_end = "]".getBytes();
    private final byte[] b_endResult = "}".getBytes();

    public StreamingObjectResultJSONSerializer(StreamingObjectResult streamingObjectResult)
    {
        this.streamingObjectResult = streamingObjectResult;
    }

    @Override
    public void stream(OutputStream stream)
    {
        try
        {
            stream.write(b_builder);
            objectMapper.writeValue(stream, this.streamingObjectResult.getResultBuilder());
            stream.write(b_activities);
            streamCollection(stream, this.streamingObjectResult.activities);
            stream.write(b_objects);
            streamObjects(stream);
            stream.write(b_end);
            stream.write(b_endResult);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            this.streamingObjectResult.close();
        }
    }

    private void streamObjects(OutputStream outputStream)
    {
        this.serializeStream(this.streamingObjectResult.getObjectStream(), outputStream);
    }

    private void streamCollection(OutputStream outputStream, List collection) throws IOException
    {
        this.serializeStream(collection.stream(), outputStream);
        outputStream.flush();
    }

    private void serializeStream(Stream<Object> objects, OutputStream outputStream)
    {
        final Counter counter = new Counter(1);
        objects.forEach(val ->
        {
            try
            {
                if (counter.getCount() > 1)
                {
                    outputStream.write(b_comma);

                }
                objectMapper.writeValue(outputStream, val);
                counter.increment();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}

