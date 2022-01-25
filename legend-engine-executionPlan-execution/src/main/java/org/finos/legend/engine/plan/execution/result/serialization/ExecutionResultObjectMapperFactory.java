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

package org.finos.legend.engine.plan.execution.result.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.io.IOException;

public class ExecutionResultObjectMapperFactory
{
    public static ObjectMapper getNewObjectMapper()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
        objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(PureDate.class, new ExecutionResultObjectMapperFactory.PureDateSerializer());
        objectMapper.registerModule(simpleModule);
        return objectMapper;
    }

    public static class PureDateSerializer extends JsonSerializer<PureDate>
    {
        @Override
        public void serialize(PureDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException
        {
            gen.writeRawValue("\"" + value.toString() + "\"");
        }
    }

    public static Function<Object, String> getPurePrimitiveToJsonConverter()
    {
        return ExecutionResultObjectMapperFactory::convertPurePrimitiveToJson;
    }

    private static String convertPurePrimitiveToJson(Object value)
    {
        if (value instanceof String)
        {
            return toJson((String) value);
        }
        if (value instanceof PureDate)
        {
            return "\"" + value.toString() + "\"";
        }
        if (value == null
                || value instanceof Double && (((Double) value).isInfinite() || ((Double) value).isNaN())
                || value instanceof Float && (((Float) value).isInfinite() || ((Float) value).isNaN()))
        {
            return "null";
        }
        return value.toString();
    }

    private static String toJson(String s)
    {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        sb.append("\"");
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (ch <= '\u001F' || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF'))
                    {
                        sb.append("\\u");
                        String hex = Integer.toHexString(ch).toUpperCase();
                        for (int p = 0; p < 4 - hex.length(); p++)
                        {
                            sb.append('0');
                        }
                        sb.append(hex);
                    }
                    else
                    {
                        sb.append(ch);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
