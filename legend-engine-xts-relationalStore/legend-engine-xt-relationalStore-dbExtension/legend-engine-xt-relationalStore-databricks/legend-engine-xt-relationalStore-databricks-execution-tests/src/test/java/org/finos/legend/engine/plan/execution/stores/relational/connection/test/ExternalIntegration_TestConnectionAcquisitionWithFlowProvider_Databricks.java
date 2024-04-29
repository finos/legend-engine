// Copyright 2022 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Databricks extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    private ConnectionManagerSelector connectionManagerSelector;

    // To make test work, please enable environment variable DATABRICKS_API_TOKEN with a valid API token

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @BeforeClass
    public static void setupTest()
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }

    @Before
    public void setup()
    {
        LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
        assertDatabricksApiTokenFlowIsAvailable(flowProvider);
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
    }

    public void assertDatabricksApiTokenFlowIsAvailable(LegendDefaultDatabaseAuthenticationFlowProvider flowProvider)
    {
        DatabricksDatasourceSpecification datasourceSpecification = new DatabricksDatasourceSpecification();
        ApiTokenAuthenticationStrategy authenticationStrategy = new ApiTokenAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, DatabaseType.Databricks);
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("databricks token flow does not exist ", flow.isPresent());
    }

    @Test
    public void testDatabricksConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.databricksSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, "SELECT 'supported' AS databricks");
    }

    private RelationalDatabaseConnection databricksSpec() throws Exception
    {
        DatabricksDatasourceSpecification dsSpecs = new DatabricksDatasourceSpecification();
        dsSpecs.hostname = "dbc-f0687849-717f.cloud.databricks.com";
        dsSpecs.port = "443";
        dsSpecs.protocol = "https";
        dsSpecs.httpPath = "/sql/1.0/warehouses/c56852187940e5a3";
        ApiTokenAuthenticationStrategy authSpec = new ApiTokenAuthenticationStrategy();
        authSpec.apiToken = "DATABRICKS_API_TOKEN";
        return new RelationalDatabaseConnection(dsSpecs, authSpec, DatabaseType.Databricks);
    }
}