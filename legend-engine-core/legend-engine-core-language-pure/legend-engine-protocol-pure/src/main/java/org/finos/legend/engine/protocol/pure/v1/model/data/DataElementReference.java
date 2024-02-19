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

package org.finos.legend.engine.protocol.pure.v1.model.data;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(using = DataElementReference.DataElementReferenceDeserializer.class)
public class DataElementReference extends EmbeddedData
{
    public PackageableElementPointer dataElement;

    public static class DataElementReferenceDeserializer extends JsonDeserializer<DataElementReference>
    {

        private static ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

        @Override
        public DataElementReference deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode dataElementNode = node.get("dataElement");
            DataElementReference dataElementReference = new DataElementReference();
            if (dataElementNode != null)
            {
                if (dataElementNode.isTextual())
                {
                    dataElementReference.dataElement = new PackageableElementPointer(
                            PackageableElementType.DATA,
                            dataElementNode.textValue(),
                            Objects.isNull(node.get("sourceInformation")) ? null : objectMapper.treeToValue(node.get("sourceInformation"), SourceInformation.class)
                    );
                }
                else if (dataElementNode.isObject())
                {
                    dataElementReference.dataElement = objectMapper.treeToValue(dataElementNode, PackageableElementPointer.class);
                }
                else
                {
                    throw new IOException("DataElementReference expects property 'dataElement' to be a PackageableElementPointer");
                }
            }
            // for backward compatability
            else
            {
                throw new IOException("DataElementReference requires attribute dataElement.");
            }
            return dataElementReference;
        }
    }

}
