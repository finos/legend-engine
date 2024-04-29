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

package org.finos.legend.engine.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class TestAnyDeserializer
{
    @Test
    public void testString() throws Exception
    {
        test(new Foo("legend"));
    }

    @Test
    public void testInteger() throws Exception
    {
        test(new Foo(1));
    }

    @Test
    public void testLong() throws Exception
    {
        test(new Foo(Integer.MAX_VALUE + 1));
    }

    @Test
    public void testFloat() throws Exception
    {
        test(new Foo(1.1));
    }

    @Test
    public void testDouble() throws Exception
    {
        test(new Foo(1.1d));
    }


    @Test
    public void testBoolean() throws Exception
    {
        test(new Foo(true));
    }

    @Test
    public void testNull() throws Exception
    {
        test(new Foo(null));
    }

    @Test
    public void testCollection() throws Exception
    {
        test(new Foo(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testFail()
    {
        Assert.assertThrows(JsonMappingException.class, () -> test(new Foo(new Foo(1))));
    }


    private void test(Foo expected) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(expected);
        Foo rehydrated = objectMapper.readValue(json, Foo.class);
        Assert.assertEquals(expected.getValue(), rehydrated.getValue());
    }

    private static class Foo
    {
        @JsonDeserialize(using = AnyDeserializerWrapper.class)
        private final Object value;

        public Foo(@JsonProperty("value") Object value)
        {
            this.value = value;
        }

        public Object getValue()
        {
            return value;
        }
    }

    private static class AnyDeserializerWrapper extends JsonDeserializer<Object>
    {
        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            return new AnyDeserializer(Arrays.asList(
                    String.class, Integer.class, Long.class, Float.class,
                    Double.class, Boolean.class
            )).deserialize(jsonParser, deserializationContext);
        }
    }
}