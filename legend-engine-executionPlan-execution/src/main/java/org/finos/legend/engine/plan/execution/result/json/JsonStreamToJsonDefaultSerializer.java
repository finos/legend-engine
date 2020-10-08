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

package org.finos.legend.engine.plan.execution.result.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.io.IOException;
import java.io.OutputStream;

public class JsonStreamToJsonDefaultSerializer extends Serializer
{
    private final JsonStreamingResult result;
    private final Builder builder;

    public JsonStreamToJsonDefaultSerializer(JsonStreamingResult result)
    {
        this.result = result;
        this.builder = new Builder("json");
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(targetStream).enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN))
        {
            generator.writeStartObject();
            generator.setCodec(new ObjectMapper());
            generator.writeFieldName("builder");
            generator.writeObject(this.builder);
            generator.writeFieldName("values");
            result.getJsonStream().accept(generator);
            generator.writeEndObject();
        }
        finally
        {
            result.close();
        }
    }
}
