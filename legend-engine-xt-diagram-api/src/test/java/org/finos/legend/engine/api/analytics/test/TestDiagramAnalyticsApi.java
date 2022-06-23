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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.api.analytics.DiagramAnalytics;
import org.finos.legend.engine.api.analytics.model.DiagramModelCoverageAnalysisInput;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class TestDiagramAnalyticsApi
{
    private final DiagramAnalytics api = new DiagramAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testDiagramAnalysisWithNotFoundDiagram() throws IOException
    {
        Exception exception = Assert.assertThrows(EngineException.class, () ->
        {
            Response response = api.analyzeDiagramModelCoverage(new DiagramModelCoverageAnalysisInput("vX_X_X", "model::UnknownDiagram", PureModelContextData.newPureModelContextData(null, null, Lists.immutable.empty())), true, null);
        });
        Assert.assertEquals(exception.getMessage(), "Can't find diagram 'model::UnknownDiagram'");
    }

    @Test
    public void testBlankDiagramAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("diagramAnalyticsTestData.json")), PureModelContextData.class);
        Response response = api.analyzeDiagramModelCoverage(new DiagramModelCoverageAnalysisInput("vX_X_X", "model::BlankDiagram", modelContextData), true, null);
        Assert.assertEquals(response.getEntity().toString(), "{\"_type\":\"data\",\"elements\":[{\"_type\":\"Diagram\",\"classViews\":[],\"generalizationViews\":[],\"name\":\"BlankDiagram\",\"package\":\"model\",\"propertyViews\":[]}]}");
    }

    @Test
    public void testDiagramAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("diagramAnalyticsTestData.json")), PureModelContextData.class);
        Response response = api.analyzeDiagramModelCoverage(new DiagramModelCoverageAnalysisInput("vX_X_X", "model::animal::AnimalDiagram", modelContextData), false, null);
        Assert.assertEquals(response.getEntity().toString(), "{\"_type\":\"data\",\"elements\":[{\"_type\":\"Profile\",\"name\":\"doc\",\"package\":\"meta::pure::profiles\",\"stereotypes\":[\"deprecated\"],\"tags\":[\"doc\",\"todo\"]},{\"_type\":\"Profile\",\"name\":\"typemodifiers\",\"package\":\"meta::pure::profiles\",\"stereotypes\":[\"abstract\"],\"tags\":[]},{\"_type\":\"Class\",\"constraints\":[],\"name\":\"Animal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"family\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::Family\"},{\"multiplicity\":{\"lowerBound\":0},\"name\":\"children\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::Animal\"}],\"qualifiedProperties\":[],\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"Enumeration\",\"name\":\"Family\",\"package\":\"model::animal\",\"stereotypes\":[],\"taggedValues\":[],\"values\":[{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"MAMMAL\"},{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"REPTILE\"}]},{\"_type\":\"Class\",\"constraints\":[],\"name\":\"Mammal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::mammal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"noOfLegs\",\"stereotypes\":[],\"taggedValues\":[{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":\"(TBD)\"}],\"type\":\"String\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[\"model::animal::Animal\"],\"taggedValues\":[{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":\"a warm-blooded vertebrate animal of a class that is distinguished by the possession of hair or fur\"}]},{\"_type\":\"Class\",\"constraints\":[],\"name\":\"Reptile\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::reptile\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"hasFin\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"Boolean\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[\"model::animal::Animal\"],\"taggedValues\":[]}]}");
    }
}

