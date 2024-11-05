//  Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.api.analytics.ClassAnalytics;
import org.finos.legend.engine.api.analytics.model.ClassModelCoverageAnalysisInput;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

public class TestClassAnalyticsApi
{
    private final ClassAnalytics api = new ClassAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testClassModelCoverageAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("classModelCoverageAnalysisTestData.json")), PureModelContextData.class);
        Assert.assertEquals(
                "{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"constraints\":[],\"name\":\"Organization\",\"originalMilestonedProperties\":[],\"package\":\"model\",\"properties\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariables\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"name\",\"stereotypes\":[],\"taggedValues\":[]},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"model::Person\"},\"typeArguments\":[],\"typeVariables\":[]},\"multiplicity\":{\"lowerBound\":0},\"name\":\"employees\",\"stereotypes\":[],\"taggedValues\":[]},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariables\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"type\",\"stereotypes\":[],\"taggedValues\":[]}],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"class\",\"constraints\":[],\"name\":\"Person\",\"originalMilestonedProperties\":[],\"package\":\"model\",\"properties\":[],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]}]}",
                api.analyzeClassModelCoverage(new ClassModelCoverageAnalysisInput("vX_X_X", "model::Organization", modelContextData), true, null).getEntity().toString());
    }
}

