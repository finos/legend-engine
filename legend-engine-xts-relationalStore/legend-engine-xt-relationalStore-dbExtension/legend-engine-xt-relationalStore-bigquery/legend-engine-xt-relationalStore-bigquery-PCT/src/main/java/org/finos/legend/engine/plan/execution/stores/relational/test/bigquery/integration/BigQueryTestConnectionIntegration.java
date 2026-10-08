// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.bigquery.integration;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPServiceAccountKeyAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import software.amazon.awssdk.regions.Region;

public class BigQueryTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final String SERVICE_ACCOUNT_SECRET = "bigquery.integration-bq-sa1";
    private static final String SERVICE_ACCOUNT_KEY_VAULT_REFERENCE = "service_account_json";

    private static final String PROJECT_ID = "legend-integration-testing";
    private static final String DEFAULT_DATASET = "integration_dataset1";

    private final RelationalDatabaseConnection conn = new RelationalDatabaseConnection();

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "BigQuery");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.BigQuery;
    }

    @Override
    public void setup()
    {
        String awsAccessKeyId = System.getProperty("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
        String awsSecretAccessKey = System.getProperty("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));

        if (StringUtils.isEmpty(awsAccessKeyId) || StringUtils.isEmpty(awsSecretAccessKey))
        {
            throw new IllegalStateException("Cannot initialize BigQuery integration connection: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are required to read the service account key from the vault");
        }

        Vault.INSTANCE.registerImplementation(
                new AWSVaultImplementation(
                        awsAccessKeyId,
                        awsSecretAccessKey,
                        Region.US_EAST_1,
                        SERVICE_ACCOUNT_SECRET
                )
        );

        BigQueryDatasourceSpecification bigQueryDatasourceSpecification = new BigQueryDatasourceSpecification();
        bigQueryDatasourceSpecification.projectId = PROJECT_ID;
        bigQueryDatasourceSpecification.defaultDataset = DEFAULT_DATASET;

        GCPServiceAccountKeyAuthenticationStrategy authSpec = new GCPServiceAccountKeyAuthenticationStrategy();
        authSpec.serviceAccountKeyVaultReference = SERVICE_ACCOUNT_KEY_VAULT_REFERENCE;

        conn.type = DatabaseType.BigQuery;
        conn.databaseType = DatabaseType.BigQuery;
        conn.element = null;
        conn.datasourceSpecification = bigQueryDatasourceSpecification;
        conn.authenticationStrategy = authSpec;
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (conn.datasourceSpecification == null)
        {
            this.setup();
        }
        return this.conn;
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }
}
