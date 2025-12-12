// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.client.Entity;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import javax.ws.rs.core.Response;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDataQualityApi
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/test-data.pure", TestDataQualityApi.class);

    @ClassRule
    public static final ResourceTestRule resources = getResourceTestRule();

    public static ResourceTestRule getResourceTestRule()

    {
        DeploymentMode deploymentMode = DeploymentMode.TEST;

        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();

        ModelManager modelManager = mock(ModelManager.class);
        PureModel pureModel = new ModelManager(deploymentMode).loadModel(pureModelContextData, PureClientVersions.production, Identity.getAnonymousIdentity(), "");
        when(modelManager.loadModel(any(), any(), any(), any())).thenReturn(pureModel);

        MetaDataServerConfiguration metaDataServerConfiguration = new MetaDataServerConfiguration();

        DataQualityExecute api = new DataQualityExecute(
                modelManager,
                executor,
                (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())),
                LegendPlanTransformers.transformers,
                metaDataServerConfiguration,
                null
        );

        ResourceTestRule resources = ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(api)
                .addResource(new MockPac4jFeature())
                .bootstrapLogging(false)
                .build();

        return resources;
    }

    @Test
    public void testDataQualityProfiling() throws IOException
    {
        DataQualityProfileInput input = new DataQualityProfileInput();
        input.clientVersion = "vX_X_X";
        input.packagePath = "demo::simplePersonValidation";
        input.model = new PureModelContextPointer();

        Response response = resources.target("pure/v1/dataquality/profile")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }


    @Test
    public void testDataQualityGetPropertyPathTree() throws IOException
    {
        PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/modelWithDataQualityValidation.pure", TestDataQualityApi.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertTrue(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
        RootGraphFetchTree rootGraphFetchTree = DataQualityPropertyPathTreeGenerator.getPropertyPathTree(((Root_meta_external_dataquality_DataQuality)packageableElement)._validationTree(), "vX_X_X", model, null);
        assertEquals("{\"_type\":\"rootGraphFetchTree\",\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::dataquality::Person\",\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"name\",\"subTrees\":[],\"subTypeTrees\":[]},{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"age\",\"subTrees\":[],\"subTypeTrees\":[]}],\"subTypeTrees\":[]}", objectMapper.writeValueAsString(rootGraphFetchTree));

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement2 = model.getPackageableElement("meta::dataquality::Person");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement2));
    }

    @Ignore
    @Test
    public void testDataQualityGetPropertyPathTree_WithNullDataQualityValidation() throws IOException
    {
        PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/modelWithNullDataQualityValidation.pure", TestDataQualityApi.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
    }
}
