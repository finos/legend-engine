// Copyright 2026 Goldman Sachs
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.finos.legend.engine.language.pure.dsl.generation.project.ProjectGenerationResult;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;


public class TestGenerateProject
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    private static final String MODEL = "{\"_type\":\"data\",\"elements\":["
            + "{\"_type\":\"Enumeration\",\"name\":\"MyEnum\",\"package\":\"model\",\"values\":[{\"value\":\"A\"},{\"value\":\"B\"}]},"
            + "{\"_type\":\"Enumeration\",\"name\":\"DependencyEnum\",\"package\":\"dependency\",\"values\":[{\"value\":\"A\"}]}"
            + "]}";

    private final ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
    private final ArtifactGenerationExtensionApi api = new ArtifactGenerationExtensionApi(this.modelManager);

    @Test
    public void aRequestWithOnlyAModelRunsWhatTheBuildRuns() throws Exception
    {
        ProjectGenerationResult result = new GenerateProjectRunner(this.modelManager).run(input("{\"model\":" + MODEL + "}"), Identity.getAnonymousIdentity());

        Assert.assertEquals(ProjectGenerationResult.Status.SUCCEEDED, result.status);
        Assert.assertEquals(2, result.artifactGeneration.results.size());
        ProjectGenerationResult.ArtifactResult artifact = result.artifactGeneration.results.get(1);
        Assert.assertEquals("model::MyEnum", artifact.element);
        Assert.assertEquals("test-enumeration-generation", artifact.extension);
        Assert.assertEquals("model/MyEnum/test-enumeration-generation", artifact.folder);
        Assert.assertEquals("SomeTestOutput.txt", artifact.files.get(0).fileName);
        Assert.assertEquals("Some output for enumeration 'MyEnum'", artifact.files.get(0).content);
    }

    @Test
    public void onlyTheProjectsElementsGetArtifacts() throws Exception
    {
        ProjectGenerationResult result = new GenerateProjectRunner(this.modelManager).run(input("{\"model\":" + MODEL + ",\"projectElementPaths\":[\"model::MyEnum\"]}"), Identity.getAnonymousIdentity());

        Assert.assertEquals(1, result.artifactGeneration.results.size());
        Assert.assertEquals("model::MyEnum", result.artifactGeneration.results.get(0).element);
    }

    @Test
    public void theResponseHasEveryStepWithItsStatus() throws Exception
    {
        Response response = this.api.generateProject(input("{\"model\":" + MODEL + ",\"fileGenerationConfig\":{\"disabled\":true}}"), null);

        Assert.assertEquals(200, response.getStatus());
        JsonNode body = objectMapper.readTree((String) response.getEntity());
        Assert.assertEquals("SUCCEEDED", body.get("status").asText());
        Assert.assertEquals("SUCCEEDED", body.get("compilation").get("status").asText());
        Assert.assertEquals("SUCCEEDED", body.get("modelGeneration").get("status").asText());
        Assert.assertEquals("data", body.get("modelGeneration").get("generatedModel").get("_type").asText());
        Assert.assertEquals("DISABLED", body.get("fileGeneration").get("status").asText());
        Assert.assertFalse("a step that did not run has no duration", body.get("fileGeneration").has("durationMs"));
        Assert.assertEquals("SUCCEEDED", body.get("artifactGeneration").get("status").asText());
        Assert.assertEquals("model/MyEnum/test-enumeration-generation", body.get("artifactGeneration").get("results").get(1).get("folder").asText());
    }

    @Test
    public void aFailingStepIsAResultNotAnError() throws Exception
    {
        String brokenModel = "{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"name\":\"A\",\"package\":\"model\",\"properties\":[{\"name\":\"b\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"genericType\":{\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"model::Missing\"}}}]}]}";

        Response response = this.api.generateProject(input("{\"model\":" + brokenModel + "}"), null);

        Assert.assertEquals(200, response.getStatus());
        JsonNode body = objectMapper.readTree((String) response.getEntity());
        Assert.assertEquals("FAILED", body.get("status").asText());
        Assert.assertEquals("FAILED", body.get("compilation").get("status").asText());
        Assert.assertTrue(body.get("compilation").get("failure").get("message").asText().contains("model::Missing"));
        Assert.assertEquals("SKIPPED", body.get("artifactGeneration").get("status").asText());
    }

    @Test
    public void aRequestThatCannotRunIsABadRequest() throws Exception
    {
        Response unknownExtension = this.api.generateProject(input("{\"model\":" + MODEL + ",\"artifactGenerationConfig\":{\"includedExtensionKeys\":[\"noSuchExtension\"]}}"), null);
        Assert.assertEquals(400, unknownExtension.getStatus());
        String error = objectMapper.writeValueAsString(unknownExtension.getEntity());
        Assert.assertTrue(error, error.contains("Unknown artifact extension keys: noSuchExtension"));

        Response noModel = this.api.generateProject(input("{}"), null);
        Assert.assertEquals(400, noModel.getStatus());
    }

    private static ProjectGenerationInput input(String json) throws Exception
    {
        return objectMapper.readValue(json, ProjectGenerationInput.class);
    }
}
