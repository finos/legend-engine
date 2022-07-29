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
        Assert.assertEquals(response.getEntity().toString(), "{\"_type\":\"data\",\"elements\":[{\"_type\":\"diagram\",\"classViews\":[],\"generalizationViews\":[],\"name\":\"BlankDiagram\",\"package\":\"model\",\"propertyViews\":[]}]}");
    }

    @Test
    public void testDiagramAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("diagramAnalyticsTestData.json")), PureModelContextData.class);
        Response response = api.analyzeDiagramModelCoverage(new DiagramModelCoverageAnalysisInput("vX_X_X", "model::animal::AnimalDiagram", modelContextData), false, null);
        Assert.assertEquals(response.getEntity().toString(), "{\"_type\":\"data\",\"elements\":[{\"_type\":\"profile\",\"name\":\"doc\",\"package\":\"meta::pure::profiles\",\"stereotypes\":[\"deprecated\"],\"tags\":[\"doc\",\"todo\"]},{\"_type\":\"profile\",\"name\":\"typemodifiers\",\"package\":\"meta::pure::profiles\",\"stereotypes\":[\"abstract\"],\"tags\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Animal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"family\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::Family\"},{\"multiplicity\":{\"lowerBound\":0},\"name\":\"children\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::Animal\"}],\"qualifiedProperties\":[],\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"Enumeration\",\"name\":\"Family\",\"package\":\"model::animal\",\"stereotypes\":[],\"taggedValues\":[],\"values\":[{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"MAMMAL\"},{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"REPTILE\"}]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Mammal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::mammal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"noOfLegs\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"String\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[\"model::animal::Animal\"],\"taggedValues\":[{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":\"a warm-blooded vertebrate animal of a class that is distinguished by the possession of hair or fur\"}]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Reptile\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::reptile\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"hasFin\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"Boolean\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[\"model::animal::Animal\"],\"taggedValues\":[]}]}");
    }

    @Test
    public void testDiagramAnalysisWithDerivedProperties() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("diagramAnalyticsTestDataWithDerivedProperties.json")), PureModelContextData.class);
        Response response = api.analyzeDiagramModelCoverage(new DiagramModelCoverageAnalysisInput("vX_X_X", "model::animal::AnimalDiagram", modelContextData), false, null);
        Assert.assertEquals(response.getEntity().toString(), "{\"_type\":\"data\",\"elements\":[{\"_type\":\"profile\",\"name\":\"doc\",\"package\":\"meta::pure::profiles\",\"stereotypes\":[\"deprecated\"],\"tags\":[\"doc\",\"todo\"]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Animal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"family\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::Family\"},{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"noOfLegs\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"Number\"},{\"multiplicity\":{\"lowerBound\":0},\"name\":\"children\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"model::animal::GenericAnimal2\"}],\"qualifiedProperties\":[{\"body\":[{\"_type\":\"func\",\"fControl\":\"greaterThan_Number_1__Number_1__Boolean_1_\",\"function\":\"greaterThan\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"this\"}],\"property\":\"noOfLegs\"},{\"_type\":\"integer\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"values\":[4]}]}],\"name\":\"something\",\"parameters\":[],\"returnMultiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"returnType\":\"Boolean\",\"stereotypes\":[],\"taggedValues\":[]},{\"body\":[{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":0,\"upperBound\":0},\"values\":[]}],\"name\":\"something2\",\"parameters\":[],\"returnMultiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"returnType\":\"model::animal::GenericAnimal\",\"stereotypes\":[],\"taggedValues\":[]}],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"Enumeration\",\"name\":\"Family\",\"package\":\"model::animal\",\"stereotypes\":[],\"taggedValues\":[],\"values\":[{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"UO\"},{\"stereotypes\":[],\"taggedValues\":[],\"value\":\"OP\"}]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"GenericAnimal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal\",\"properties\":[],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"GenericAnimal2\",\"originalMilestonedProperties\":[],\"package\":\"model::animal\",\"properties\":[],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Mammal\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::mammal\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"noOfLegs\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"String\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Reptile\",\"originalMilestonedProperties\":[],\"package\":\"model::animal::reptile\",\"properties\":[{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"hasFin\",\"stereotypes\":[],\"taggedValues\":[],\"type\":\"Boolean\"}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[\"model::animal::Animal\"],\"taggedValues\":[{\"tag\":{\"profile\":\"meta::pure::profiles::doc\",\"value\":\"doc\"},\"value\":\"\"}]}]}");
    }
}

