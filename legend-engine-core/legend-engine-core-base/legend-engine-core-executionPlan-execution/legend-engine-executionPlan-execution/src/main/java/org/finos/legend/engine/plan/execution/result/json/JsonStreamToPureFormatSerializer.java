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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

public class JsonStreamToPureFormatSerializer extends JsonSerializer
{
    private final JsonStreamingResult result;

    public JsonStreamToPureFormatSerializer(JsonStreamingResult result)
    {
        this.result = result;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        try (JsonGenerator generator = this.createGenerator(targetStream))
        {
            generator.setCodec(new ObjectMapper());
            result.getJsonStream().accept(generator);
        }
        finally
        {
            result.close();
        }
    }
}
