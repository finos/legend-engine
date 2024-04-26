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

package org.finos.legend.engine.language.pure.relational.api.relationalElement.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.RelationalElementAPI;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.input.DatabaseToModelGenerationInput;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;

import net.javacrumbs.jsonunit.JsonAssert;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestRelationalElementApi
{
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final ModelManager testManager = new ModelManager(DeploymentMode.TEST);

    private static PureModelContextData buildPMCDFromString(String model)
    {
        return PureModelContextData.newBuilder()
                .withPureModelContextData(PureGrammarParser.newInstance().parseModel(model))
                .withSerializer(new Protocol("pure", "vX_X_X"))
                .withOrigin(new PureModelContextPointer())
                .build();
    }

    private static String loadFromFile(String filename) throws IOException
    {
        return IOUtils.toString(Objects.requireNonNull(TestRelationalElementApi.class
                        .getClassLoader()
                        .getResource(filename)),
                StandardCharsets.UTF_8
        );
    }


    private void test(String inputGrammarPath, String expectedJsonPath, String dbPath) throws IOException
    {
        String expectedJson = loadFromFile(expectedJsonPath);
        String inputGrammar = loadFromFile(inputGrammarPath);
        Assert.assertNotNull(expectedJson);
        Assert.assertNotNull(inputGrammar);
        PureModelContextData inputPmcd = buildPMCDFromString(inputGrammar);
        DatabaseToModelGenerationInput inputJson = new DatabaseToModelGenerationInput(dbPath, inputPmcd, null);
        RelationalElementAPI relationalElementAPI = new RelationalElementAPI(DeploymentMode.PROD, null);
        Response response = relationalElementAPI.generateModelsFromDatabaseSpecification(inputJson, null);
        Assert.assertNotNull(response);
        String actualJson = response.getEntity().toString();
        JsonAssert.assertJsonEquals(expectedJson, actualJson);
        PureModelContextData generatedModel = objectMapper.readValue(actualJson, PureModelContextData.class);
        PureModelContextData fullModel = generatedModel.combine(inputPmcd);
        // compile generated model and input database
        testManager.loadModelAndData(fullModel, fullModel.serializer.version, IdentityFactoryProvider.getInstance().getAnonymousIdentity(), null);
    }

    @Test
    public void shouldGenerateModelsFromDatabaseSpecification() throws IOException
    {
        test("inputGrammar.pure", "expectedJson.json", "meta::relational::transform::autogen::tests::testDB");
    }

    @Test
    public void generateModelsWithDbWithIncludes() throws IOException
    {
        test("inputGrammarWithInclude.pure", "expectedJsonWithInclude.json", "model::MyDB");

    }
}
