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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationProperty
{
    public String name;

    @JsonDeserialize(using = ValueDeserializer.class)
    public Object value;

    public SourceInformation sourceInformation;

    private static class ValueDeserializer extends JsonDeserializer<Object>
    {
        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonToken a = jsonParser.getCurrentToken();
            if (jsonParser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT)
            {
                return jsonParser.getLongValue();
            }
            else if (jsonParser.getCurrentToken() == JsonToken.VALUE_TRUE)
            {
                return Boolean.TRUE;
            }
            else if (jsonParser.getCurrentToken() == JsonToken.VALUE_FALSE)
            {
                return Boolean.FALSE;
            }
            else if (jsonParser.getCurrentToken() == JsonToken.START_ARRAY)
            {
                List<String> value = new ArrayList<>();
                while (jsonParser.nextToken() != JsonToken.END_ARRAY)
                {
                    if (jsonParser.getCurrentToken() != JsonToken.VALUE_STRING)
                    {
                        throw new IllegalArgumentException("Configuration property value array only supports string values");
                    }
                    value.add((jsonParser.getValueAsString()));
                }
                return value;
            }
            else if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT)
            {
                TypeFactory typeFactory = deserializationContext.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
                Map<String, Object> obj = deserializationContext.readValue(jsonParser, mapType);
                obj.values().forEach(value ->
                {
                    if (!(value instanceof String))
                    {
                        throw new IllegalArgumentException("Configuration property value map only supports string values");
                    }
                });
                return obj;
            }
            else
            {
                return jsonParser.getText();
            }
        }
    }
}
