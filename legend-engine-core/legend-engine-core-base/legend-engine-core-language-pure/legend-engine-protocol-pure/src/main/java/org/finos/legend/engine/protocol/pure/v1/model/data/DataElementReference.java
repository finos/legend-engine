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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.Objects;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;

@JsonDeserialize(using = DataElementReference.DataElementReferenceDeserializer.class)
public class DataElementReference extends EmbeddedData
{
    public PackageableElementPointer dataElement;

    public static class DataElementReferenceDeserializer extends JsonDeserializer<DataElementReference>
    {
        @Override
        public DataElementReference deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);
            JsonNode dataElementNode = node.get("dataElement");
            DataElementReference dataElementReference = new DataElementReference();
            if (dataElementNode != null)
            {
                if (dataElementNode.isTextual())
                {
                    dataElementReference.dataElement = new PackageableElementPointer(
                            PackageableElementType.DATA,
                            dataElementNode.textValue(),
                            Objects.isNull(node.get("sourceInformation")) ? null : codec.treeToValue(node.get("sourceInformation"), SourceInformation.class)
                    );
                }
                else if (dataElementNode.isObject())
                {
                    dataElementReference.dataElement = codec.treeToValue(dataElementNode, PackageableElementPointer.class);
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
