// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;

import java.io.IOException;
import java.util.Objects;

/** Use to mock data in function using a runtime for execution
 * store represents the store you want to mock data for
 * This assume the function uses 1 (or none) runtime
 * We will resolve the connection used for the store
 * In the future, this could be extended to add runtime pointer if more than one runtime
 * and/or one connection but for now the expectation is one store can be mocked
 */

@JsonDeserialize(using = StoreTestData.StoreTestDataDeserializer.class)
public class StoreTestData
{
    public String doc;
    public StoreProviderPointer store;
    public EmbeddedData data;
    public SourceInformation sourceInformation;

    public static class StoreTestDataDeserializer extends JsonDeserializer<StoreTestData>
    {

        private static ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

        @Override
        public StoreTestData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode docNode = node.get("doc");
            JsonNode storeNode = node.get("store");
            JsonNode dataNode = node.get("data");
            JsonNode sourceInformationNode = node.get("sourceInformation");
            StoreTestData storeTestData = new StoreTestData();
            storeTestData.doc = Objects.isNull(docNode) ? null : docNode.textValue();
            storeTestData.sourceInformation = Objects.isNull(sourceInformationNode) ? null : objectMapper.treeToValue(sourceInformationNode, SourceInformation.class);
            storeTestData.data = Objects.isNull(dataNode) ? null : objectMapper.treeToValue(dataNode, EmbeddedData.class);
            if (storeNode != null)
            {
                if (storeNode.isTextual())
                {
                    storeTestData.store = new StoreProviderPointer(
                            PackageableElementType.STORE,
                            storeNode.textValue(),
                            Objects.isNull(node.get("sourceInformation")) ? null : objectMapper.treeToValue(node.get("sourceInformation"), SourceInformation.class)
                    );
                }
                else if (storeNode.isObject())
                {
                    storeTestData.store = objectMapper.treeToValue(storeNode, StoreProviderPointer.class);
                }
                else
                {
                    throw new IOException("StoreTestData expects property 'store' to be a StoreProviderPointer");
                }
            }
            // for backward compatability
            else
            {
                throw new IOException("StoreTestData requires attribute store.");
            }
            return storeTestData;
        }
    }
}
