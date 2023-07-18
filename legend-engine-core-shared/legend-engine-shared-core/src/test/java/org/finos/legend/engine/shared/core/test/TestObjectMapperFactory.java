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

package org.finos.legend.engine.shared.core.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

public class TestObjectMapperFactory
{
    @Test
    public void doesNotAutocloseJsonArraysAndObjects() throws IOException
    {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = ObjectMapperFactory.getNewStandardObjectMapper().getFactory().createGenerator(writer);
        generator.writeStartArray();
        generator.writeStartObject();
        generator.close();
        Assert.assertEquals("Generator should not autoclose JSON array and object", "[{", writer.toString());
    }

    @Test
    public void doesNotAutocloseOutputStream() throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        {
            @Override
            public void close()
            {
                Assert.fail("Close should not be call");
            }
        };

        ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapper();
        mapper.writeValue(os, "hello");
    }

    @Test
    public void sortMapKeys() throws IOException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();
        HashMap<String, Object> map = new HashMap<>();
        map.put("b", 1);
        map.put("a", true);
        map.put("f", 4);
        map.put("d", false);
        String json = objectMapper.writeValueAsString(map);
        Assert.assertEquals("{\"a\":true,\"b\":1,\"d\":false,\"f\":4}", json);
    }

    @Test
    public void sortPojoProperties() throws IOException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();
        String json = objectMapper.writeValueAsString(new PojoForTest(1, true, 4, false));
        Assert.assertEquals("{\"a\":true,\"b\":1,\"d\":false,\"f\":4}", json);
    }

    private static class PojoForTest
    {
        @JsonProperty
        private int b;
        @JsonProperty
        private boolean a;
        @JsonProperty
        private int f;
        @JsonProperty
        private boolean d;

        public PojoForTest(int b, boolean a, int f, boolean d)
        {
            this.b = b;
            this.a = a;
            this.f = f;
            this.d = d;
        }
    }
}
