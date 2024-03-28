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

package org.finos.legend.engine.testDataGeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.testData.generation.model.TestDataGenerationInput;
import org.finos.legend.engine.testData.generation.service.TestDataGenerationService;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class TestDataGenerationTest
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final ModelManager modelManager = new ModelManager(DeploymentMode.TEST);

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource(path);
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : null;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testGenerateEmbeddedData(String testGenerationInputPath, String expectedResult) throws Exception
    {
        String testGenerationInput = getResourceAsString(testGenerationInputPath);
        TestDataGenerationInput input = objectMapper.readValue(testGenerationInput, TestDataGenerationInput.class);
        PureModel pureModel = modelManager.loadModel(input.model, input.clientVersion == null ? PureClientVersions.production : input.clientVersion, IdentityFactoryProvider.getInstance().getAnonymousIdentity(), null);
        List<EmbeddedData> testData = TestDataGenerationService.generateEmbeddedData(input.query, pureModel.getMapping(input.mapping), pureModel);
        Assert.assertEquals(objectMapper.writeValueAsString(testData), expectedResult);
    }

    @Test
    public void testRelationalCSVTableGeneration() throws Exception
    {
        testGenerateEmbeddedData(
                "models/relationalModelTestDataGenerationInput.json",
                "[{\"tables\":[{\"schema\":\"default\",\"table\":\"FirmTable\",\"values\":\"id\"},{\"schema\":\"default\",\"table\":\"PersonTable\",\"values\":\"id,firm_id,firstName\"}]}]");
    }
}
