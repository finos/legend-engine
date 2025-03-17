//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.api.analytics.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.api.analytics.DataProductAnalytics;
import org.finos.legend.engine.api.analytics.model.DataProductAnalysisInput;
import org.finos.legend.engine.generation.analytics.DataProductAnalyticsHelper;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class TestDataProductAnalyticsApi
{
    private final DataProductAnalytics api = new DataProductAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = DataProductAnalyticsHelper.getNewObjectMapper();
    private static final String minimumPureClientVersion = "v1_20_0";
    private static final ImmutableList<String> testVersions = PureClientVersions.versionsSince(minimumPureClientVersion);

    @Test
    public void testDataProductAnalysisWithNotFoundDataProduct() throws IOException
    {
        testVersions.forEach(pureClient ->
        {
            Exception exception = Assert.assertThrows(EngineException.class, () ->
            {
                api.analyzeDataProduct(new DataProductAnalysisInput(pureClient, "model::UnknownDataProduct", PureModelContextData.newPureModelContextData(null, null, Lists.immutable.empty())), null);
            });
            Assert.assertEquals(exception.getMessage(), "Can't find data space 'model::UnknownDataProduct'");
        });
    }

    @Test
    public void testDataProductAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("dataProductAnalyticsTestData.json")), PureModelContextData.class);
        String expected = "{\"defaultExecutionContext\":\"dummyContext\",\"diagrams\":[],\"elementDocs\":[],\"elements\":[],\"executables\":[],\"executionContexts\":[{\"compatibleRuntimes\":[\"model::dummyRuntime\"],\"datasets\":[],\"defaultRuntime\":\"model::dummyRuntime\",\"mapping\":\"model::dummyMapping\",\"name\":\"dummyContext\"}],\"mappingToMappingCoverageResult\":{\"model::dummyMapping\":{\"mappedEntities\":[]}},\"model\":{\"_type\":\"data\",\"elements\":[]},\"name\":\"AnimalDS\",\"package\":\"model::animal\",\"path\":\"model::animal::AnimalDS\",\"stereotypes\":[],\"taggedValues\":[]}";
        testAnalyticsWithVersions(expected, modelContextData, "model::animal::AnimalDS");
    }

    @Test
    public void testDataProductCoverageAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("dataProductAnalyticsTestData.json")), PureModelContextData.class);
        String expected = "{\"defaultExecutionContext\":\"dummyContext\",\"diagrams\":[],\"elementDocs\":[],\"elements\":[],\"executables\":[],\"executionContexts\":[{\"compatibleRuntimes\":[\"model::dummyRuntime\"],\"defaultRuntime\":\"model::dummyRuntime\",\"mapping\":\"model::dummyMapping\",\"name\":\"dummyContext\"}],\"mappingToMappingCoverageResult\":{\"model::dummyMapping\":{\"mappedEntities\":[],\"model\":{\"_type\":\"data\",\"elements\":[]}}},\"name\":\"AnimalDS\",\"package\":\"model::animal\",\"path\":\"model::animal::AnimalDS\",\"stereotypes\":[],\"taggedValues\":[]}";
        testCoverageAnalyticsWithVersions(expected, modelContextData, "model::animal::AnimalDS");
    }

    private void testAnalyticsWithVersions(String expected, PureModelContextData modelContextData, String dataProduct)
    {
        testVersions.forEach(pureClient ->
        {
            Response response = api.analyzeDataProduct(new DataProductAnalysisInput(pureClient, dataProduct, modelContextData), null);
            Assert.assertEquals(expected, response.getEntity().toString());
        });
    }

    private void testCoverageAnalyticsWithVersions(String expected, PureModelContextData modelContextData, String dataProduct)
    {
        testVersions.forEach(pureClient ->
        {
            Response response = api.analyzeDataProductCoverage(new DataProductAnalysisInput(pureClient, dataProduct, modelContextData), null);
            Assert.assertEquals(expected, response.getEntity().toString());
        });
    }
}

