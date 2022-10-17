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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

import java.io.IOException;

@Deprecated
@JsonDeserialize(using = PrimitiveType.PrimitiveTypeDeserializer.class)
public class PrimitiveType extends PackageableElementPtr
{
    private static ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();

    private PrimitiveType()
    {

    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class PrimitiveTypeDeserializer extends JsonDeserializer<ValueSpecification>
    {
        @Override
        public ValueSpecification deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode name = node.get("name");
            ValueSpecification result;
            if (name != null)
            {
                result = new PackageableElementPtr(name.asText());
            }
            else
            {
                result = new PackageableElementPtr(node.get("fullPath").asText());
            }
            JsonNode sourceInformation = node.get("sourceInformation");
            if (sourceInformation != null)
            {
                result.sourceInformation = om.treeToValue(sourceInformation, SourceInformation.class);
            }
            return result;
        }
    }

}

