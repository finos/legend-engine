//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.generation.model.OpenApiGenerationInput;
import org.finos.legend.engine.generation.service.OpenApiGenerationService;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;

public class TestOpenApiGenerationService
{
    private ObjectMapper objectMapper = new PureProtocolObjectMapperFactory().getNewObjectMapper();

    @Test
    public void shouldGenerateOpenApiSpecFromPureModelContext() throws IOException
    {
        String payload = IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("pure_model_context_payload.json")), StandardCharsets.UTF_8);
        assertNotNull(payload);
        OpenApiGenerationInput input = objectMapper.readValue(payload, OpenApiGenerationInput.class);
        ModelManager manager = new ModelManager(DeploymentMode.TEST);
        OpenApiGenerationService service = new OpenApiGenerationService(manager);
        Response response = service.generateOpenApi(input, null);
        Assert.assertEquals("{\"openapi\":\"3.0.0\",\"info\":{\"title\":\"Legend API\",\"version\":\"1.0.0\"},\"servers\":[{}],\"paths\":{\"/testAPI/demo\":{\"get\":{\"tags\":[\"definition\"],\"responses\":{\"200\":{\"description\":\"success\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/TabularDataSet\"}}}}}},\"post\":{\"tags\":[\"definition\"],\"responses\":{\"200\":{\"description\":\"success\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/TabularDataSet\"}}}}}}}},\"components\":{\"schemas\":{\"TabularDataSet\":{\"type\":\"object\",\"properties\":{\"columns\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/TDSColumn\"}},\"rows\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/TDSRow\"}}}},\"Any\":{\"type\":\"object\",\"properties\":{}},\"DataType\":{\"type\":\"object\",\"properties\":{}},\"TDSColumn\":{\"type\":\"object\",\"properties\":{\"enumMappingId\":{\"type\":\"string\"},\"sourceDataType\":{\"$ref\":\"#/components/schemas/Any\"},\"name\":{\"type\":\"string\"},\"documentation\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\"},\"type\":{\"$ref\":\"#/components/schemas/DataType\"}}},\"TDSRow\":{\"type\":\"object\",\"properties\":{\"values\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Any\"}},\"parent\":{\"$ref\":\"#/components/schemas/TabularDataSet\"}}}}}}", response.getEntity());
    }
}
