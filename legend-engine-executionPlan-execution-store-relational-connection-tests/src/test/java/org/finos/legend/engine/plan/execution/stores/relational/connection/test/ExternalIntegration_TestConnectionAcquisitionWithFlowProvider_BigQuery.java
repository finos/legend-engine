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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_BigQuery extends DbSpecificTests{

    private ConnectionManagerSelector connectionManagerSelector;
    private final InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup()
    {
        Vault.INSTANCE.registerImplementation(inMemoryVault);
        LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
        LegendDefaultDatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration = new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration();
        flowProvider.configure(flowProviderConfiguration);
        assertBigQueryWIFFlowIsAvailable(flowProvider);
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
    }

    public void assertBigQueryWIFFlowIsAvailable(LegendDefaultDatabaseAuthenticationFlowProvider flowProvider)
    {
        BigQueryDatasourceSpecification datasourceSpecification = new BigQueryDatasourceSpecification();
        GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy authenticationStrategy = new GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, DatabaseType.BigQuery);
        relationalDatabaseConnection.type = DatabaseType.BigQuery;

        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("BigQuery Workload Identity Federation Flow does not exist ", flow.isPresent());
    }

    @Test
    public void testBigQueryWIFConnection_subject() throws Exception {

        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithWIFSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, "select * from `legend-integration-testing.legend_testing_dataset.basic_test_table`");
    }

    @Test
    public void testBigQueryWIFConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithWIFSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>)null, systemUnderTest);
        testConnection(connection, "select * from `legend-integration-testing.legend_testing_dataset.basic_test_table`");
    }

    private RelationalDatabaseConnection bigQueryWithWIFSpec() {
        inMemoryVault.setValue("key1", System.getenv("AWS_ACCESS_KEY_ID"));
        inMemoryVault.setValue("secret1", System.getenv("AWS_SECRET_ACCESS_KEY"));
        BigQueryDatasourceSpecification bigQueryDatasourceSpecification = new BigQueryDatasourceSpecification();
        bigQueryDatasourceSpecification.projectId = "legend-integration-testing";
        bigQueryDatasourceSpecification.defaultDataset = "legend_testing_dataset";
        GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy authSpec = new GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy();
        authSpec.workloadProjectNumber = "412074507462";
        authSpec.workloadPoolId = "aws-wif-pool2";
        authSpec.workloadProviderId = "aws-wif-provider2";
        authSpec.serviceAccountEmail = "legend-integration-wif1@legend-integration-testing.iam.gserviceaccount.com";
        authSpec.awsAccountId = "564704738649";
        authSpec.awsRegion = "us-east-1";
        authSpec.awsRole = "gcp-wif";
        authSpec.awsAccessKeyIdVaultReference = "key1";
        authSpec.awsSecretAccessKeyVaultReference = "secret1";
        return new RelationalDatabaseConnection(bigQueryDatasourceSpecification, authSpec, DatabaseType.BigQuery);
    }
}
