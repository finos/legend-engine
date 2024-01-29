package org.finos.legend.engine.server.test.extensions;

import org.finos.legend.engine.external.shared.format.model.api.ExternalFormats;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class TestExtensions
{
    @Test
    public void testAvailableFormats()
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        Response response = new ExternalFormats(modelManager).codeGenerationDescriptions();
        Assert.assertEquals(200, response.getStatus());
    }
}
