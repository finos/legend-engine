// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.entitlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.entitlement.services.EntitlementModelObjectMapperFactory;
import org.finos.legend.engine.entitlement.services.RelationalDatabaseEntitlementServiceExtension;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class TestRelationalStoreEntitlementAnalytics
{
    private static final ObjectMapper entitlementObjectMapper = EntitlementModelObjectMapperFactory.getNewObjectMapper();

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

    private void testStoreEntitlementAnalyticsRelationalStoreExtension(String modelFilePath, String runtimePath, String mappingPath, String expectedAnalysisResult) throws Exception
    {
        String pureModelString = getResourceAsString(modelFilePath);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        Mapping mapping = pureModel.getMapping(mappingPath);
        Root_meta_pure_runtime_Runtime runtime = pureModel.getRuntime(runtimePath);
        RelationalDatabaseEntitlementServiceExtension extension = new RelationalDatabaseEntitlementServiceExtension();
        List<DatasetSpecification> datasets = extension.generateDatasetSpecifications(null, runtimePath, runtime, mappingPath, mapping, pureModelContextData, pureModel);
        Assert.assertEquals(entitlementObjectMapper.writeValueAsString(datasets), expectedAnalysisResult);
    }

    @Test
    public void testStoreEntitlementAnalyticsForH2() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure", "runtime::H2Runtime", "mapping::CovidDataMapping", "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]");
    }

    @Test
    public void testStoreEntitlementAnalyticsForSnowflake() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure", "runtime::SnowflakeRuntime", "mapping::CovidDataMapping", "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]");
    }

    @Test
    public void testStoreEntitlementAnalyticsForComplexRuntime() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure", "runtime::CompoundRuntime", "mapping::CovidDataMapping", "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"},{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]");
    }
}
