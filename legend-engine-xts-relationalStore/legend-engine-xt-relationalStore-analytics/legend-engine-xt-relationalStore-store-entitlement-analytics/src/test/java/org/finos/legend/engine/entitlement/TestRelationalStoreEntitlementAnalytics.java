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
import org.finos.legend.engine.entitlement.model.entitlementReport.DatasetEntitlementReport;
import org.finos.legend.engine.entitlement.model.specification.DatasetSpecification;
import org.finos.legend.engine.entitlement.model.specification.RelationalDatabaseTableSpecification;
import org.finos.legend.engine.entitlement.services.EntitlementModelObjectMapperFactory;
import org.finos.legend.engine.entitlement.services.RelationalDatabaseEntitlementServiceExtension;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.generated.core_relational_store_entitlement_utility_relationalTableAnalyzer;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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

    private void testStoreEntitlementAnalyticsRelationalStoreExtension(String modelFilePath, String runtimePath, String mappingPath, String expectedDatasetResult, String expectedReportResult) throws Exception
    {
        String pureModelString = getResourceAsString(modelFilePath);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Mapping mapping = pureModel.getMapping(mappingPath);
        Root_meta_core_runtime_Runtime runtime = pureModel.getRuntime(runtimePath);
        RelationalDatabaseEntitlementServiceExtension extension = new RelationalDatabaseEntitlementServiceExtension();
        List<DatasetSpecification> datasets = extension.generateDatasetSpecifications(null, runtimePath, runtime, mappingPath, mapping, pureModelContextData, pureModel);
        Assert.assertEquals(expectedDatasetResult, entitlementObjectMapper.writeValueAsString(datasets));
        List<DatasetEntitlementReport> reports = extension.generateDatasetEntitlementReports(datasets, null, runtimePath, runtime, mappingPath, mapping, pureModelContextData, pureModel, null);
        Assert.assertEquals(expectedReportResult, entitlementObjectMapper.writeValueAsString(reports));
    }

    @Test
    public void testStoreEntitlementAnalyticsForH2() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure",
                "runtime::H2Runtime",
                "mapping::CovidDataMapping",
                "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]",
                "[{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"}},{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.COVID_DATA\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}}]");
    }

    @Test
    public void testStoreEntitlementAnalyticsForH2WithMappers() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure",
                "runtime::H2RuntimeWithMappers",
                "mapping::CovidDataMapping",
                "[{\"name\":\"COVID.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"COVID\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"COVID.COVID_DATA_PROD\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"COVID\",\"table\":\"COVID_DATA_PROD\"}]",
                "[{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"COVID.DEMOGRAPHICS\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"COVID\",\"table\":\"DEMOGRAPHICS\"}},{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"COVID.COVID_DATA_PROD\",\"type\":\"H2\",\"database\":\"CovidDataStore\",\"schema\":\"COVID\",\"table\":\"COVID_DATA_PROD\"}}]");
    }

    @Test
    public void testStoreEntitlementAnalyticsForSnowflake() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure",
                "runtime::SnowflakeRuntime",
                "mapping::CovidDataMapping",
                "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]",
                "[{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"}},{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}}]");
    }

    @Test
    public void testStoreEntitlementAnalyticsForComplexRuntime() throws Exception
    {
        testStoreEntitlementAnalyticsRelationalStoreExtension("models/relationalModel.pure",
                "runtime::CompoundRuntime",
                "mapping::CovidDataMapping",
                "[{\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"},{\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}]",
        "[{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.DEMOGRAPHICS\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"DEMOGRAPHICS\"}},{\"dataset\":{\"_type\":\"relationalDatabaseTable\",\"name\":\"default.COVID_DATA\",\"type\":\"Snowflake\",\"database\":\"CovidDataStore\",\"schema\":\"default\",\"table\":\"COVID_DATA\"}}]");
    }

    @Test
    public void testGenerateDatasetEntitlementReports()
    {
        List<DatasetSpecification> datasets = new ArrayList<>(Arrays.asList(new DatasetSpecification("DummyTable", "DummyH2"), new RelationalDatabaseTableSpecification("default.DEMOGRAPHICS", "H2", "CovidDataStore", "default","DEMOGRAPHICS")));
        RelationalDatabaseEntitlementServiceExtension extension = new RelationalDatabaseEntitlementServiceExtension();
        List<DatasetEntitlementReport> reports = extension.generateDatasetEntitlementReports(datasets, null, "", null, "", null, null, null, null);
        Assert.assertEquals(reports.size(), 1);
    }

    @Test
    public void testDependencyDatabaseRetrievability()
    {
        String pureModelString = getResourceAsString("models/relationalModel.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Store store = pureModel.getStore("store::CovidDataStoreA");
        Assert.assertEquals(core_relational_store_entitlement_utility_relationalTableAnalyzer.Root_meta_analytics_store_entitlements_getTablesFromDatabase_Database_1__Table_MANY_((Database) store, pureModel.getExecutionSupport()).size(), 2);
    }

    @Test
    public void testCircularDependencyInDatabases()
    {
        String pureModelString = getResourceAsString("models/databaseCircularDependency.pure");
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString, false);
        String expectedErrorMessage = "COMPILATION error: Detected a circular dependency in element prerequisites graph!";
        try
        {
            Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
            Assert.fail("Expected compilation error with message: " + expectedErrorMessage + "; but no error occurred");
        }
        catch (EngineException e)
        {
            MatcherAssert.assertThat(EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(),
                    e.getErrorType()), CoreMatchers.startsWith(expectedErrorMessage));
        }
    }
}
