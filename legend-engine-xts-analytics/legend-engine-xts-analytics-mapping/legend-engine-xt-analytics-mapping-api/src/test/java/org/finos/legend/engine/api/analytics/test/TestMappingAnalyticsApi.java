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
import org.finos.legend.engine.api.analytics.MappingAnalytics;
import org.finos.legend.engine.api.analytics.model.MappingModelCoverageAnalysisInput;
import org.finos.legend.engine.api.analytics.model.MappingRuntimeCompatibilityAnalysisInput;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

public class TestMappingAnalyticsApi
{
    private final MappingAnalytics api = new MappingAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testMappingModelCoverageAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("mappingModelCoverageAnalysisTestData.json")), PureModelContextData.class);
        Assert.assertEquals(
                "{\"mappedEntities\":[{\"path\":\"model::Bank\",\"properties\":[{\"_type\":\"entity\",\"entityPath\":\"model::Trader\",\"name\":\"employees\"},{\"_type\":\"enum\",\"enumPath\":\"model::OrgType\",\"name\":\"type\"}]},{\"path\":\"model::Trader\",\"properties\":[{\"_type\":\"MappedProperty\",\"name\":\"fullName\"}]}]}",
                api.analyzeModelCoverage(new MappingModelCoverageAnalysisInput("vX_X_X", "model::mapping", modelContextData), false, false, null).getEntity().toString());
    }

    @Test
    public void testMappingModelCoverageAnalysisWithTypeInfoReturned() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("mappingModelCoverageAnalysisTestData.json")), PureModelContextData.class);
        Assert.assertEquals(
                "{\"mappedEntities\":[{\"info\":{\"isRootEntity\":true,\"subClasses\":[]},\"path\":\"model::Bank\",\"properties\":[{\"_type\":\"entity\",\"entityPath\":\"model::Trader\",\"mappedPropertyInfo\":{\"multiplicity\":{\"lowerBound\":0},\"propertyType\":\"Entity\"},\"name\":\"employees\"},{\"_type\":\"enum\",\"enumPath\":\"model::OrgType\",\"mappedPropertyInfo\":{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"propertyType\":\"Enumeration\"},\"name\":\"type\"}]},{\"info\":{\"isRootEntity\":true,\"subClasses\":[]},\"path\":\"model::Trader\",\"properties\":[{\"_type\":\"MappedProperty\",\"mappedPropertyInfo\":{\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"propertyType\":\"String\"},\"name\":\"fullName\"}]}]}",
                api.analyzeModelCoverage(new MappingModelCoverageAnalysisInput("vX_X_X", "model::mapping", modelContextData), true, true, null).getEntity().toString());
    }

    @Test
    public void testMappingRuntimeCompatibilityAnalysis() throws IOException
    {
        PureModelContextData modelContextData = objectMapper.readValue(Objects.requireNonNull(getClass().getClassLoader().getResource("mappingRuntimeCompatibilityAnalyticsTestData.json")), PureModelContextData.class);
        Assert.assertEquals(
                "{\"runtimes\":[\"runtime::R0\",\"runtime::R1\",\"runtime::R3\"]}",
                api.analyzeMappingRuntimeCompatibility(new MappingRuntimeCompatibilityAnalysisInput("vX_X_X", "mapping::M0", modelContextData), null).getEntity().toString());
        Assert.assertEquals(
                "{\"runtimes\":[\"runtime::R1\",\"runtime::R3\"]}",
                api.analyzeMappingRuntimeCompatibility(new MappingRuntimeCompatibilityAnalysisInput("vX_X_X", "mapping::M1", modelContextData), null).getEntity().toString());
        Assert.assertEquals(
                "{\"runtimes\":[\"runtime::R2\",\"runtime::R3\"]}",
                api.analyzeMappingRuntimeCompatibility(new MappingRuntimeCompatibilityAnalysisInput("vX_X_X", "mapping::M2", modelContextData), null).getEntity().toString());
    }
}

