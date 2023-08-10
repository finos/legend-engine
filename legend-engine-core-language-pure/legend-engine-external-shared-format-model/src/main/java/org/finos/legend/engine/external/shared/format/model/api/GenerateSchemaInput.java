//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.shared.format.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

public class GenerateSchemaInput
{
    @JsonProperty(required = true)
    public String clientVersion;

    @JsonProperty(required = true)
    public PureModelContext model;

    @JsonProperty(required = true)
    @JsonDeserialize(using = ConfigDeserializer.class)
    public ModelToSchemaConfiguration config;

    @JsonProperty(required = true)
    public ModelUnit sourceModelUnit;

    public boolean generateBinding = false;
    public String targetBindingPath;

    private static class ConfigDeserializer extends JsonDeserializer
    {
        private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
        {
            JsonNode tree = jsonParser.readValueAsTree();
            if (!tree.has("format"))
            {
                throw new IOException("Schema configuration is missing format");
            }
            String format = tree.get("format").textValue();

            if (!ExternalFormats.extensions.containsKey(format))
            {
                throw new IOException("Unknown external format: " + format);
            }
            Class<?> configType = Arrays.stream(ExternalFormats.extensions.get(format).getClass().getGenericInterfaces())
                    .filter(ParameterizedType.class::isInstance)
                    .map(ParameterizedType.class::cast)
                    .filter(pt -> pt.getRawType().equals(ExternalFormatSchemaGenerationExtension.class))
                    .findFirst()
                    .map(pt -> pt.getActualTypeArguments()[1])
                    .map(Class.class::cast)
                    .orElseThrow(() -> new IOException("Cannot obtain model generation configuration type"));

            return objectMapper.treeToValue(tree, configType);
        }
    }
}
