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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.io.IOException;

@JsonDeserialize(using = PropertyPointer.PropertyPointerDeserializer.class)
public class PropertyPointer
{
    public String propertyOwner;
    public String property;
    public SourceInformation sourceInformation;

    public static class PropertyPointerDeserializer extends JsonDeserializer<PropertyPointer>
    {
        @Override
        public PropertyPointer deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode _class = node.get("class");
            JsonNode property = node.get("property");
            JsonNode sourceInformation = node.get("sourceInformation");
            JsonNode propertyOwner = node.get("propertyOwner");
            PropertyPointer propertyPointer = new PropertyPointer();
            propertyPointer.propertyOwner = _class != null ? _class.asText() : propertyOwner != null ? propertyOwner.asText() : null;
            propertyPointer.property = property.asText();
            propertyPointer.sourceInformation = om.treeToValue(sourceInformation, SourceInformation.class);
            return propertyPointer;
        }
    }
}
