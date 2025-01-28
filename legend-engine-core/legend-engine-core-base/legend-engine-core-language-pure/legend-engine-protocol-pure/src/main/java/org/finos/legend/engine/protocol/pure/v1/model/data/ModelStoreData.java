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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@JsonDeserialize(using = ModelStoreData.ModelStoreDeserializer.class)
public class ModelStoreData extends EmbeddedData
{

    public List<ModelTestData> modelData;

    public static class ModelStoreDeserializer extends JsonDeserializer<ModelStoreData>
    {
        @Override
        public ModelStoreData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);
            JsonNode instances = node.get("instances");
            JsonNode modelDataJsonNode = node.get("modelData");
            ModelStoreData result = new ModelStoreData();
            result.modelData = Lists.mutable.empty();
            if (modelDataJsonNode != null)
            {
                if (modelDataJsonNode.isArray())
                {
                    ArrayNode modelDataNode = (ArrayNode) modelDataJsonNode;
                    Iterator<JsonNode> elements = modelDataNode.elements();
                    while (elements.hasNext())
                    {
                        result.modelData.add(codec.treeToValue(elements.next(), ModelTestData.class));
                    }
                }
                else
                {
                    throw new IOException("Model Store Data expects property 'modelData' to be an array");
                }
            }
            // for backward compatability
            else if (instances != null)
            {
                if (instances.isObject())
                {
                    ObjectNode instanceObjectNode = (ObjectNode) instances;
                    Iterator<Map.Entry<String, JsonNode>> itr = instanceObjectNode.fields();
                    while (itr.hasNext())
                    {
                        Map.Entry<String, JsonNode> instance = itr.next();
                        String model = instance.getKey();
                        JsonNode instanceVal = instance.getValue();
                        ValueSpecification val = codec.treeToValue(instanceVal, ValueSpecification.class);
                        ModelInstanceTestData modelInstanceTestData = new ModelInstanceTestData();
                        modelInstanceTestData.model = model;
                        modelInstanceTestData.instances = val;
                        result.modelData.add(modelInstanceTestData);
                    }
                }
            }
            return result;
        }
    }
}
