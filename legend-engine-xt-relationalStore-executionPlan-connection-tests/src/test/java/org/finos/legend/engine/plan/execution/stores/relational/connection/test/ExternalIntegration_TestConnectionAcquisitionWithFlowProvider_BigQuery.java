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

import java.sql.Connection;
import javax.security.auth.Subject;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_BigQuery extends ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_GoogleCloud
{

    public static final String TEST_QUERY = "select * from `legend-integration-testing.integration_dataset1.table1`";

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.BigQuery;
    }

    @Test
    public void testBigQueryGCPADCConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithGCPADCSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, TEST_QUERY);
    }

    @Test
    public void testBigQueryGCPWIFConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithGCPWIFSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, TEST_QUERY);
    }

    @Test
    public void testBigQueryGCPADCConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithGCPADCSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>) null, systemUnderTest);
        testConnection(connection, TEST_QUERY);
    }

    @Test
    public void testBigQueryGCPWIFConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.bigQueryWithGCPWIFSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>) null, systemUnderTest);
        testConnection(connection, TEST_QUERY);
    }

    private RelationalDatabaseConnection bigQueryWithGCPADCSpec()
    {
        BigQueryDatasourceSpecification bigQueryDatasourceSpecification = new BigQueryDatasourceSpecification();
        bigQueryDatasourceSpecification.projectId = "legend-integration-testing";
        bigQueryDatasourceSpecification.defaultDataset = "integration_dataset1";
        GCPApplicationDefaultCredentialsAuthenticationStrategy authSpec = new GCPApplicationDefaultCredentialsAuthenticationStrategy();
        return new RelationalDatabaseConnection(bigQueryDatasourceSpecification, authSpec, DatabaseType.BigQuery);
    }

    private RelationalDatabaseConnection bigQueryWithGCPWIFSpec()
    {
        BigQueryDatasourceSpecification bigQueryDatasourceSpecification = new BigQueryDatasourceSpecification();
        bigQueryDatasourceSpecification.projectId = "legend-integration-testing";
        bigQueryDatasourceSpecification.defaultDataset = "integration_dataset1";
        GCPWorkloadIdentityFederationAuthenticationStrategy authSpec = new GCPWorkloadIdentityFederationAuthenticationStrategy();
        authSpec.serviceAccountEmail = "integration-bq-sa1@legend-integration-testing.iam.gserviceaccount.com";
        return new RelationalDatabaseConnection(bigQueryDatasourceSpecification, authSpec, DatabaseType.BigQuery);
    }
}
