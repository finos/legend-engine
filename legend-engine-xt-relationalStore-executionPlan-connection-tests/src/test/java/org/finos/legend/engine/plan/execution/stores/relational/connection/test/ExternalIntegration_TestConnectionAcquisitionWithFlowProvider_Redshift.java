// Copyright 2021 Goldman Sachs
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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_Redshift extends DbSpecificTests
{
    public static final String REDSHIFT_CLUSTER_NAME = "redshift-load-balancer-cb907ffe8879f6c2.elb.us-east-1.amazonaws.com";

    private ConnectionManagerSelector connectionManagerSelector;
    private PropertiesVaultImplementation vaultImplementation;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup() throws Exception
    {
        LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
        assertRedshiftFlowIsAvailable(flowProvider);

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
        //usePropertiesVault();
    }

    private void usePropertiesVault() {
        Properties properties = new Properties();
        properties.put("REDSHIFT_LEGEND_INTEG_USERNAME", "XXXX");
        properties.put("REDSHIFT_LEGEND_INTEG_PASSWORD", "XXXX");
        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    public void assertRedshiftFlowIsAvailable(LegendDefaultDatabaseAuthenticationFlowProvider flowProvider)
    {
        RelationalDatabaseConnection relationalDatabaseConnection = this.redshiftWithUserPassword();
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("Redshift flow does not exist ", flow.isPresent());
    }

    @After
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
    }

    @Test
    public void testRedshiftUserPasswordConnection() throws Exception
    {
        try {
            RelationalDatabaseConnection systemUnderTest = this.redshiftWithUserPassword();
            Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
            // TODO : epsstan - connection acquisition fails with increased concurrency. Do we have a bug or is this a driver issue ?
            testConnection(connection, 1, "select * from test");
        }
        finally {
            ConnectionStateManager.getInstance().close();
        }
    }

    private RelationalDatabaseConnection redshiftWithUserPassword()
    {
        RedshiftDatasourceSpecification redshiftDatasourceSpecification = new RedshiftDatasourceSpecification();
        redshiftDatasourceSpecification.databaseName = "integration_db1";
        redshiftDatasourceSpecification.host = REDSHIFT_CLUSTER_NAME;
        redshiftDatasourceSpecification.port = 5439;
        redshiftDatasourceSpecification.region = "us-east-1";
        redshiftDatasourceSpecification.clusterID = "";
        UserNamePasswordAuthenticationStrategy authenticationStrategy = new UserNamePasswordAuthenticationStrategy();
        authenticationStrategy.baseVaultReference = "";
        authenticationStrategy.userNameVaultReference = "REDSHIFT_INTEGRATION_USER1_NAME";
        authenticationStrategy.passwordVaultReference = "REDSHIFT_INTEGRATION_USER1_PASSWORD";
        RelationalDatabaseConnection connection = new RelationalDatabaseConnection(redshiftDatasourceSpecification, authenticationStrategy, DatabaseType.Redshift);
        connection.type = DatabaseType.Redshift;
        return connection;
    }
}
