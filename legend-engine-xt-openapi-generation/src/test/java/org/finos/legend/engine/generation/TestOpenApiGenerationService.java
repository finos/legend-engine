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

import static org.junit.Assert.assertNotNull;

public class TestOpenApiGenerationService {
    private ObjectMapper objectMapper = new ObjectMapperFactory().getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void shouldGenerateOpenApiSpecFromPureModelContext() throws IOException {
        String payload = IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("context_payload.json")), StandardCharsets.UTF_8);
        assertNotNull(payload);
        OpenApiGenerationInput input = objectMapper.readValue(payload, OpenApiGenerationInput.class);
        ModelManager manager = new ModelManager(DeploymentMode.TEST);
        OpenApiGenerationService service = new OpenApiGenerationService(manager);
        Response response = service.generateOpenApi(input, null);
        assertNotNull(response);
    }
}
