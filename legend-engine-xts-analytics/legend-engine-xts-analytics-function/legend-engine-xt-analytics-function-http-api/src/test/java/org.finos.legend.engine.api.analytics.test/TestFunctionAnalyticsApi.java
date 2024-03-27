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
import org.finos.legend.engine.api.analytics.FunctionAnalytics;
import org.finos.legend.engine.api.analytics.model.FunctionModelCoverageAnalysisInput;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

public class TestFunctionAnalyticsApi
{
    private final FunctionAnalytics api = new FunctionAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testFunctionModelCoverageAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("functionModelCoverageAnalysisTestData.json")), PureModelContextData.class);
        Assert.assertEquals(
                "{\"_type\":\"data\",\"elements\":[{\"_type\":\"class\",\"constraints\":[],\"name\":\"Person\",\"originalMilestonedProperties\":[],\"package\":\"model\",\"properties\":[],\"qualifiedProperties\":[],\"stereotypes\":[],\"superTypes\":[],\"taggedValues\":[]},{\"_type\":\"function\",\"body\":[{\"_type\":\"func\",\"function\":\"plus\",\"parameters\":[{\"_type\":\"collection\",\"multiplicity\":{\"lowerBound\":3,\"upperBound\":3},\"values\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"person\"}],\"property\":\"firstName\"},{\"_type\":\"string\",\"value\":\" \"},{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"person\"}],\"property\":\"lastName\"}]}]}],\"name\":\"personFullName_Person_1__String_1_\",\"package\":\"model\",\"parameters\":[{\"_type\":\"var\",\"class\":\"model::Person\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"person\"}],\"postConstraints\":[],\"preConstraints\":[],\"returnMultiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"returnType\":\"String\",\"stereotypes\":[],\"taggedValues\":[]}]}",
                api.analyzeFunctionModelCoverage(new FunctionModelCoverageAnalysisInput("vX_X_X", "model::personFullName_Person_1__String_1_", modelContextData), true, null).getEntity().toString());
    }
}

