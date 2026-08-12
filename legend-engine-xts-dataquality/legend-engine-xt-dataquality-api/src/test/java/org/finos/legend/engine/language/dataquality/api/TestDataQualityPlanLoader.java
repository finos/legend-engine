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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.dataquality.model.DataQualityExecuteInput;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class TestDataQualityPlanLoader
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final TypeReference<List<Artifact>> ARTIFACT_TYPE = new TypeReference<List<Artifact>>()
    {
    };

    @ClassRule
    public static WireMockClassRule wireMockServer = new WireMockClassRule();

    @Rule
    public WireMockClassRule rule = wireMockServer;

    @Test
    public void testFetchPlanFromSDLC() throws IOException
    {
        configureWireMockForNoRetries();
        DataQualityPlanLoader dataQualityPlanLoader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        SingleExecutionPlan singleExecutionPlan = dataQualityPlanLoader.fetchPlanFromSDLC(Identity.getAnonymousIdentity(), createDataQualityParameterValue());
        assertNotNull(singleExecutionPlan);
    }

    @Test
    public void testFetchPlanFromSDLC_EmptyProjectCoordinates_Exception() throws IOException
    {
        configureWireMockForNoRetries();
        DataQualityPlanLoader dataQualityPlanLoader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        DataQualityExecuteInput dataQualityParameterValue = new DataQualityExecuteInput();
        dataQualityParameterValue.sdlc = null;
        dataQualityParameterValue.elementPath = "meta::dataquality::PersonDataQualityValidation";
        Assert.assertEquals("DataQualityParameter info must contain Element Path and sdlc to access metadata services", Assert.assertThrows(EngineException.class, () -> dataQualityPlanLoader.fetchPlanFromSDLC(Identity.getAnonymousIdentity(), dataQualityParameterValue)).getMessage());
    }

    @Test
    public void testFetchPlanFromSDLC_EmptyAlloyProjectCoordinates_Exception() throws IOException
    {
        AlloySDLC alloySDLC = new AlloySDLC();
        alloySDLC.groupId = "com.dq.test";
        alloySDLC.artifactId = "test-sandbox";
        alloySDLC.version = null;
        configureWireMockForNoRetries();
        DataQualityPlanLoader dataQualityPlanLoader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        DataQualityExecuteInput dataQualityParameterValue = new DataQualityExecuteInput();
        dataQualityParameterValue.sdlc = alloySDLC;
        dataQualityParameterValue.elementPath = "meta::dataquality::PersonDataQualityValidation";
        Assert.assertEquals("AlloySDLC info must contain and group and artifact IDs to access metadata services", Assert.assertThrows(EngineException.class, () -> dataQualityPlanLoader.fetchPlanFromSDLC(Identity.getAnonymousIdentity(), dataQualityParameterValue)).getMessage());
    }

    @Test
    public void testFetchReconPlanFromSDLC() throws IOException
    {
        configureWireMockForReconPlan("master-SNAPSHOT");
        DataQualityPlanLoader loader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        SingleExecutionPlan plan = loader.fetchReconPlanFromSDLC(Identity.getAnonymousIdentity(), "meta::dataquality::TestRelationComparison", createAlloySDLC("master-SNAPSHOT"));
        assertNotNull(plan);
    }

    @Test
    public void testFetchReconPlanFromSDLC_NotPresentInArtifacts_Exception() throws IOException
    {
        configureWireMockForNoRetries();
        DataQualityPlanLoader loader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        EngineException ex = Assert.assertThrows(EngineException.class,
                () -> loader.fetchReconPlanFromSDLC(Identity.getAnonymousIdentity(), "meta::dataquality::PersonDataQualityValidation", createAlloySDLC("master-SNAPSHOT")));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().contains(DataQualityPlanLoader.RECON_EXECUTION_PLAN_FILE_NAME));
    }

    @Test
    public void testFetchReconPlanFromSDLC_CachedForReleaseVersion() throws IOException
    {
        String version = "1.2.3";
        configureWireMockForReconPlan(version);
        DataQualityPlanLoader loader = new DataQualityPlanLoader(createServerConnectionConfiguration(), null);
        SingleExecutionPlan first = loader.fetchReconPlanFromSDLC(Identity.getAnonymousIdentity(), "meta::dataquality::TestRelationComparison", createAlloySDLC(version));
        assertNotNull(first);
        // Stop the server; a second call should still succeed if the plan is cached.
        WireMock.reset();
        SingleExecutionPlan second = loader.fetchReconPlanFromSDLC(Identity.getAnonymousIdentity(), "meta::dataquality::TestRelationComparison", createAlloySDLC(version));
        assertSame(first, second);
    }

    @Test
    public void testReconPlanCacheKey_SnapshotAndInvalidCoordinatesAreNotCacheable()
    {
        Assert.assertNotNull(DataQualityPlanLoader.cacheKey("meta::dataquality::TestRelationComparison", createAlloySDLC("1.2.3")));
        Assert.assertNull(DataQualityPlanLoader.cacheKey("meta::dataquality::TestRelationComparison", createAlloySDLC("master-SNAPSHOT")));
        Assert.assertNull(DataQualityPlanLoader.cacheKey(null, createAlloySDLC("1.2.3")));
        Assert.assertNull(DataQualityPlanLoader.cacheKey("meta::dataquality::TestRelationComparison", null));
        AlloySDLC missingVersion = createAlloySDLC(null);
        Assert.assertNull(DataQualityPlanLoader.cacheKey("meta::dataquality::TestRelationComparison", missingVersion));
    }

    private AlloySDLC createAlloySDLC(String version)
    {
        AlloySDLC sdlc = new AlloySDLC();
        sdlc.groupId = "com.dq.test";
        sdlc.artifactId = "test-sandbox";
        sdlc.version = version;
        return sdlc;
    }

    private void configureWireMockForReconPlan(String version) throws IOException
    {
        URL url = Objects.requireNonNull(TestDataQualityPlanLoader.class.getClassLoader().getResource("inputs/dataQualityReconArtifacts.json"));
        List<Artifact> response = objectMapper.readValue(url, ARTIFACT_TYPE);
        WireMock.stubFor(WireMock.get("/api/generations/com.dq.test/test-sandbox/versions/" + version + "/meta::dataquality::TestRelationComparison")
                .willReturn(WireMock.aResponse().withStatus(200).withBody(objectMapper.writeValueAsString(response))));
    }

    private DataQualityExecuteInput createDataQualityParameterValue()
    {
        AlloySDLC alloySDLC = new AlloySDLC();
        alloySDLC.groupId = "com.dq.test";
        alloySDLC.artifactId = "test-sandbox";
        alloySDLC.version = "master-SNAPSHOT";
        DataQualityExecuteInput dataQualityParameterValue = new DataQualityExecuteInput();
        dataQualityParameterValue.sdlc = alloySDLC;
        dataQualityParameterValue.elementPath = "meta::dataquality::PersonDataQualityValidation";
        return dataQualityParameterValue;
    }

    private ServerConnectionConfiguration createServerConnectionConfiguration()
    {
        ServerConnectionConfiguration serverConfiguration = new ServerConnectionConfiguration();

        serverConfiguration.host = "localhost";
        serverConfiguration.port = rule.port();
        serverConfiguration.prefix = "/api";

        return serverConfiguration;
    }

    private static void configureWireMockForNoRetries() throws IOException
    {
        URL url = Objects.requireNonNull(TestDataQualityPlanLoader.class.getClassLoader().getResource("inputs/dataQualityArtifacts.json"));
        List<Artifact> response = objectMapper.readValue(url, ARTIFACT_TYPE);
        WireMock.stubFor(WireMock.get("/api/generations/com.dq.test/test-sandbox/versions/master-SNAPSHOT/meta::dataquality::PersonDataQualityValidation")
                .willReturn(WireMock.aResponse().withStatus(200).withBody(objectMapper.writeValueAsString(response))));
    }
}
