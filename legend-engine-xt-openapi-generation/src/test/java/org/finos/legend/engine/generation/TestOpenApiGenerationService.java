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

package org.finos.legend.engine.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.generation.model.OpenApiGenerationInput;
import org.finos.legend.engine.generation.service.OpenApiGenerationService;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestOpenApiGenerationService
{
    private ObjectMapper objectMapper = new ObjectMapperFactory().getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void shouldGenerateOpenApiSpecFromPureModelContext() throws IOException
    {
        String payload = getResource("pure_model_context_payload.json");
        String expected = getResource("expected_openapi.json");
        assertNotNull(payload);
        OpenApiGenerationInput input = objectMapper.readValue(payload, OpenApiGenerationInput.class);
        ModelManager manager = new ModelManager(DeploymentMode.TEST);
        OpenApiGenerationService service = new OpenApiGenerationService(manager);
        Response response = service.generateOpenApi(input, null);
        assertNotNull(response);
        assertEquals("OpenAPI specification assertion",expected, response.getEntity().toString());
    }

    private String getResource(String s) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(s)), StandardCharsets.UTF_8);
    }
}
