// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.execution.test.data.generation.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.execution.test.data.generation.TestDataGeneration;
import org.finos.legend.engine.execution.test.data.generation.api.model.TestDataGenerationWithDefaultSeedInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;

import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Map;
import javax.ws.rs.core.Response;


public class TestTestDataGeneration
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorBuilder().withStoreExecutors(Relational.build(0)).build();


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

    public Response doTest(String path) throws Exception
    {
        String testDataGenerationWithDefaultSeedInput = getResourceAsString(path);
        TestDataGenerationWithDefaultSeedInput input = objectMapper.readValue(testDataGenerationWithDefaultSeedInput, TestDataGenerationWithDefaultSeedInput.class);
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        String clientVersion = input.clientVersion == null ? PureClientVersions.production : input.clientVersion;
        Map<String, Object> parameterNameValueMap = Maps.mutable.empty();
        if (input.parameterValues != null)
        {
            for (ParameterValue parameterValue : input.parameterValues)
            {
                parameterNameValueMap.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
            }
        }
        return TestDataGeneration.executeTestDataGenerateWithDefaultSeedUtil(
                pureModel -> HelperValueSpecificationBuilder.buildLambda(input.function.body, input.function.parameters, pureModel.getContext()),
                () -> modelManager.loadModel(input.model, clientVersion, Identity.makeUnknownIdentity(), null),
                input.mapping,
                input.runtime,
                input.context,
                input.hashStrings,
                parameterNameValueMap,
                clientVersion,
                Identity.makeUnknownIdentity(),
                "system",
                this.planExecutor
        );
    }


    @Test
    public void TestDataGenerationWithDefaultSeed() throws Exception
    {
        Response response = doTest("models/relationalModelTestDataGenerationInputWithParameters.json");
        /**
         *  Since test data is randomly generated, we can't compare the whole content.
         *  one of the examples of generated test data is:
         *        default
         *        PersonTable
         *        id,lastName
         *        1,67
         *        2,dc
         *        3,2f
         *        4,43
         *        5,4f
         *        6,26
         *        7,f9
         *        31,22
         *        34,22
         *        35,22
         *        -----
         */

        Assert.assertTrue(response.getEntity().toString().contains("default\n" +
                "PersonTable\n" +
                "id,lastName"));
    }

    @Test
    public void TestDataGenerationWithMilestoningParamWithDefaultSeed() throws Exception
    {
        Response response = doTest("models/relationalModelTestDataGenerationInputWithMilestoningParams.json");
        /**
         *  Since test data is randomly generated, we can't compare the whole content.
         *  one of the examples of generated test data is:
         *        default
         *        PersonTable
         *        id,lastName
         *        1,67
         *        2,dc
         *        3,2f
         *        4,43
         *        5,4f
         *        6,26
         *        7,f9
         *        31,22
         *        34,22
         *        35,22
         *        -----
         */
        Assert.assertTrue(response.getEntity().toString().contains("default\n" +
                "PersonTable\n" +
                "id,lastName"));

    }
}