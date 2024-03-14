// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.generation.artifact.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.finos.legend.engine.generation.artifact.GenerationOutput;
import org.finos.legend.engine.generation.artifact.api.ArtifactGenerationExtensionOutput.SerializedArtifactExtensionResult;
import org.finos.legend.engine.generation.artifact.api.ArtifactGenerationExtensionOutput.SerializedArtifactsByExtensionElement;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;

public class TestArtifactGenerationExtensionApi
{

    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    private final ArtifactGenerationExtensionRunner api = new ArtifactGenerationExtensionRunner(new ModelManager(DeploymentMode.TEST));

    @Test
    public void testArtifactGenerationApi() throws IOException
    {
        ArtifactGenerationExtensionInput context = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("org/finos/legend/engine/generation/artifact/api/SimpleModel.json")), ArtifactGenerationExtensionInput.class);
        ArtifactGenerationExtensionOutput runnerResult = api.run(context, IdentityFactoryProvider.getInstance().getAnonymousIdentity());
        List<SerializedArtifactExtensionResult> values = runnerResult.values;
        Assert.assertEquals(1, values.size());
        SerializedArtifactExtensionResult value = values.get(0);
        Assert.assertEquals(value.extension, "test-enumeration-generation");
        List<SerializedArtifactsByExtensionElement> artifactsByElement = value.artifactsByExtensionElement;
        Assert.assertEquals(1, artifactsByElement.size());
        SerializedArtifactsByExtensionElement artifactResult = artifactsByElement.get(0);
        Assert.assertEquals("model::MyEnum", artifactResult.element);
        Assert.assertEquals("test-enumeration-generation", artifactResult.extension);
        Assert.assertEquals(1, artifactResult.files.size());
        GenerationOutput file = artifactResult.files.get(0);
        Assert.assertEquals("txt", file.getFormat());
        Assert.assertEquals("model/MyEnum/test-enumeration-generation/SomeTestOutput.txt", file.getFileName());
        Assert.assertEquals("Some output for enumeration 'MyEnum'", file.getContent());
    }


}
