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

package org.finos.legend.engine.plan.execution.result;

import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class StreamingResult extends Result
{
    public abstract Builder getResultBuilder();

    public abstract Serializer getSerializer(SerializationFormat format);

    public StreamingResult(List<ExecutionActivity> activities)
    {
        super("success", activities);
    }

    public void stream(OutputStream outputStream, SerializationFormat format) throws IOException
    {
        this.stream(outputStream, this.getSerializer(format));
    }

    public void stream(OutputStream outputStream, Serializer serializer) throws IOException
    {
        serializer.stream(outputStream);
    }

    public String flush(Serializer serializer)
    {
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            stream(bos, serializer);
            return bos.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
