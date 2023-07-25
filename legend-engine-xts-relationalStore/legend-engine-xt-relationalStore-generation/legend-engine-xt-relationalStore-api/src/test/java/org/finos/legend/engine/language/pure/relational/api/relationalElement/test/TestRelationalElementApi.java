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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.RelationalElementAPI;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.input.DatabaseToModelGenerationInput;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.ServiceLoader;

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
    public void shouldGenerateModelFromDatabase() throws IOException
    {
        String expectedJson = loadFromFile("expectedJson.json");
        String inputGrammar = loadFromFile("inputGrammar.pure");
        Assert.assertNotNull(expectedJson);
        Assert.assertNotNull(inputGrammar);
        PureModelContextData inputPmcd = compilePmcd(inputGrammar);
        String databasePath = "meta::relational::transform::autogen::tests::testDB";
        DatabaseToModelGenerationInput inputJson = new DatabaseToModelGenerationInput(databasePath, inputPmcd);
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> generatorExtensions.flatCollect(e -> e.getExtraExtensions(pureModel));
        RelationalElementAPI relationalElementAPI = new RelationalElementAPI(routerExtensions, DeploymentMode.PROD);
        Response response = relationalElementAPI.generateModelFromDatabase(inputJson, null);
        Assert.assertNotNull(response);
        String actualJson = response.getEntity().toString();
        Assert.assertEquals(expectedJson, actualJson);
    }
}
