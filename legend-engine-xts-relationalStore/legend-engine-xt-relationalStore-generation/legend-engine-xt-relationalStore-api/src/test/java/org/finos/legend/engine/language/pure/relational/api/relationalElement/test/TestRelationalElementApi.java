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

import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.RelationalElementAPI;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.input.DatabaseToModelGenerationInput;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import net.javacrumbs.jsonunit.JsonAssert;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestRelationalElementApi
{
    private static PureModelContextData compilePmcd(String model)
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

    @Test
    public void shouldGenerateModelsFromDatabaseSpecification() throws IOException
    {
        String expectedJson = loadFromFile("expectedJson.json");
        String inputGrammar = loadFromFile("inputGrammar.pure");
        Assert.assertNotNull(expectedJson);
        Assert.assertNotNull(inputGrammar);
        PureModelContextData inputPmcd = compilePmcd(inputGrammar);
        String databasePath = "meta::relational::transform::autogen::tests::testDB";
        DatabaseToModelGenerationInput inputJson = new DatabaseToModelGenerationInput(databasePath, inputPmcd, null);
        RelationalElementAPI relationalElementAPI = new RelationalElementAPI(DeploymentMode.PROD, null);
        Response response = relationalElementAPI.generateModelsFromDatabaseSpecification(inputJson, null);
        Assert.assertNotNull(response);
        String actualJson = response.getEntity().toString();
        JsonAssert.assertJsonEquals(expectedJson, actualJson);
    }
}
