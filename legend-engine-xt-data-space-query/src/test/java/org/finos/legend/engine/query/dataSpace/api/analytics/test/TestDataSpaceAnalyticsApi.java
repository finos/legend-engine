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

package org.finos.legend.engine.query.dataSpace.api.analytics.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.query.dataSpace.api.analytics.DataSpaceAnalytics;
import org.finos.legend.engine.query.dataSpace.api.analytics.model.DataSpaceAnalysisInput;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class TestDataSpaceAnalyticsApi
{
    private final DataSpaceAnalytics api = new DataSpaceAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testDiagramAnalysisWithNotFoundDiagram() throws IOException
    {
        Exception exception = Assert.assertThrows(EngineException.class, () ->
        {
            Response response = api.analyzeDataSpaceModelCoverage(new DataSpaceAnalysisInput("VX_X_X", "model::UnknownDataSpace", PureModelContextData.newPureModelContextData(null, null, Lists.immutable.empty())), null);
            Assert.assertEquals(response.getEntity().toString(), "{\"classes\":[],\"enumerations\":[],\"profiles\":[]}");
        });
        Assert.assertEquals(exception.getMessage(), "Can't find data space 'model::UnknownDataSpace'");
    }

    @Test
    public void testBlankDataSpaceAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("dataSpaceAnalyticsTestData.json")), PureModelContextData.class);
        Response response = api.analyzeDataSpaceModelCoverage(new DataSpaceAnalysisInput("VX_X_X", "model::animal::Animal", modelContextData), null);
        Assert.assertEquals(response.getEntity().toString(), "{\"diagrams\":{\"model::GeneralDiagram\":{\"classes\":[],\"enumerations\":[],\"profiles\":[]},\"model::animal::AnimalDiagram\":{\"classes\":[\"model::animal::mammal::Mammal\",\"model::animal::reptile::Reptile\"],\"enumerations\":[],\"profiles\":[]}},\"executionContexts\":{\"dummyContext\":{\"runtimes\":[\"model::dummyRuntime\"]},\"dummyContext2\":{\"runtimes\":[\"model::dummyRuntime\",\"model::dummyRuntime2\"]},\"dummyContext3\":{\"runtimes\":[\"model::dummyRuntime\",\"model::dummyRuntime2\"]}}}");
    }
}

