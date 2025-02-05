// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.Map;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.One;
import org.finos.legend.engine.protocol.pure.v1.ProcessHelper;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;

@JsonDeserialize(using = ClassInstance.InstanceValueDeserializer.class)
public class ClassInstance extends One
{
    private static final Map<String, java.lang.Class<?>> classMap = PureProtocolObjectMapperFactory.getClassInstanceTypeMappings();

    public String type;
    @JsonSerialize(using = ClassInstance.ValueSerializer.class)
    public Object value = Lists.mutable.empty();

    public ClassInstance()
    {

    }

    public ClassInstance(String type, Object value, SourceInformation sourceInformation)
    {
        this.type = type;
        this.value = value;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class InstanceValueDeserializer extends JsonDeserializer<ClassInstance>
    {
        @Override
        public ClassInstance deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            ClassInstance result = new ClassInstance();
            result.type = node.get("type").textValue();
            Class<?> _class = classMap.get(result.type);
            if (_class == null)
            {
                throw new RuntimeException("Can't parse the ClassInstance value for type '" + result.type + "'");
            }
            if (ValueSpecification.class.isAssignableFrom(_class))
            {
                ((ObjectNode) node.get("value")).set("_type", new TextNode(result.type));   // For backward compatibility
            }
            result.value = oc.treeToValue(node.get("value"), _class);
            result.sourceInformation = ProcessHelper.processOne(node, "sourceInformation", SourceInformation.class, oc);
            return result;
        }
    }

    // Jackson prefer static typing, and value is of type Object, preventing Jackson from figuring out the proper serializer
    // Hence, his forces to serialize for the actual type in a dynamic manner and ensure "_type" property is uncluded
    public static class ValueSerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException
        {
            if (value != null)
            {
                if (value instanceof Iterable)
                {
                    gen.writeStartArray();
                    for (Object v : (Iterable<?>) value)
                    {
                        gen.writeObject(v);
                    }
                    gen.writeEndArray();
                }
                else
                {
                    gen.writeObject(value);
                }
            }
            else
            {
                gen.writeNull();
            }
        }
    }
}

