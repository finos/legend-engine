// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.execution.test.data.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.io.IOException;

public class TestDataGenerationObjectMapperFactory
{
    public static ObjectMapper getNewObjectMapper()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule simpleModule = new SimpleModule();
        // NOTE: for test data generation we prepend the : otherwise we could have used ExecutionResultSerializer
        // Also note that here PureDate is the metamodel form
        simpleModule.addSerializer(PureDate.class, new JsonSerializer<PureDate>()
        {
            @Override
            public void serialize(PureDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException
            {
                gen.writeRaw(":\"" + value.toString() + "\"");
            }
        });
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }
}