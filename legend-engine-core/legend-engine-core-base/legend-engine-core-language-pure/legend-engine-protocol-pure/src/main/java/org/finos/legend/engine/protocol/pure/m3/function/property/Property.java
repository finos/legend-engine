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

package org.finos.legend.engine.protocol.pure.m3.function.property;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processMany;
import static org.finos.legend.engine.protocol.pure.v1.ProcessHelper.processOne;

@JsonDeserialize(using = Property.PropertyDeserializer.class)
public class Property
{
    public String name;
    public GenericType genericType;
    public Multiplicity multiplicity;
    public DefaultValue defaultValue;
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();
    public AggregationKind aggregation;
    public SourceInformation sourceInformation;

    public static class PropertyDeserializer extends JsonDeserializer<Property>
    {
        @Override
        public Property deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            Property property = new Property();

            property.name = node.get("name").asText();

            property.genericType = processOne(node, "genericType", GenericType.class, codec);

            // Backward compatibility --------------
            if (node.get("type") != null)
            {
                String fullPath = node.get("type").asText();
                PackageableType type = new PackageableType(fullPath);
                if (node.get("propertyTypeSourceInformation") != null)
                {
                    type.sourceInformation = codec.treeToValue(node.get("propertyTypeSourceInformation"), SourceInformation.class);
                }
                property.genericType = new GenericType(type);
            }
            // Backward compatibility --------------

            property.multiplicity = processOne(node, "multiplicity", Multiplicity.class, codec);
            property.defaultValue = processOne(node, "defaultValue", DefaultValue.class, codec);
            property.stereotypes = processMany(node, "stereotypes", StereotypePtr.class, codec);
            property.taggedValues = processMany(node, "taggedValues", TaggedValue.class, codec);
            property.aggregation = processOne(node, "aggregation", AggregationKind.class, codec);
            property.sourceInformation = processOne(node, "sourceInformation", SourceInformation.class, codec);

            return property;
        }
    }


}
