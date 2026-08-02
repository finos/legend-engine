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

package org.finos.legend.engine.protocol.pure.m3.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;

import java.io.IOException;

public class TaggedValue
{
    public TagPtr tag;
    /**
     * Historically a plain JSON string; now a {@link CString} so the protocol records whether the literal was
     * authored as a multi-line ('''...''') block. On the wire it stays a plain string unless {@code multiLine}
     * is set, and both shapes deserialize.
     */
    @JsonSerialize(using = ValueSerializer.class)
    @JsonDeserialize(using = ValueDeserializer.class)
    public CString value;
    public SourceInformation sourceInformation;

    public static class ValueSerializer extends JsonSerializer<CString>
    {
        @Override
        public void serialize(CString value, JsonGenerator generator, SerializerProvider serializers) throws IOException
        {
            if (value.multiLine)
            {
                generator.writeStartObject();
                generator.writeStringField("_type", "string");
                generator.writeBooleanField("multiLine", true);
                generator.writeStringField("value", value.value);
                generator.writeEndObject();
            }
            else
            {
                generator.writeString(value.value);
            }
        }

        @Override
        public void serializeWithType(CString value, JsonGenerator generator, SerializerProvider serializers, TypeSerializer typeSerializer) throws IOException
        {
            // the wire shape is hand-encoded above - a bare string, or an object carrying its own _type
            serialize(value, generator, serializers);
        }
    }

    public static class ValueDeserializer extends JsonDeserializer<CString>
    {
        @Override
        public CString deserialize(JsonParser parser, DeserializationContext context) throws IOException
        {
            JsonNode node = parser.getCodec().readTree(parser);
            if (!node.isObject())
            {
                return new CString(node.asText());
            }
            JsonNode value = node.get("value");
            CString result = new CString(value == null ? "" : value.asText());
            JsonNode multiLine = node.get("multiLine");
            result.multiLine = multiLine != null && multiLine.asBoolean();
            return result;
        }

        @Override
        public Object deserializeWithType(JsonParser parser, DeserializationContext context, TypeDeserializer typeDeserializer) throws IOException
        {
            // both wire shapes are handled above - a bare string has no type id, and the object carries its own
            return deserialize(parser, context);
        }
    }
}
