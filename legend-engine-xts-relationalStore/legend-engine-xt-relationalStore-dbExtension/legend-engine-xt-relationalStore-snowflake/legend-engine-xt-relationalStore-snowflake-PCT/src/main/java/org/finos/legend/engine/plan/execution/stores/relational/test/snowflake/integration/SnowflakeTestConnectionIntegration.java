// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.snowflake.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import software.amazon.awssdk.regions.Region;

import java.util.Properties;


public class SnowflakeTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Snowflake");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Snowflake;
    }

    @Override
    public void setup()
    {
        Vault.INSTANCE.registerImplementation(
                new AWSVaultImplementation(
                        System.getProperty("AWS_ACCESS_KEY_ID"),
                        System.getProperty("AWS_SECRET_ACCESS_KEY"),
                        Region.US_EAST_1,
                        "snowflake.INTEGRATION_USER1"
                )
        );
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "ki79827";
        snowflakeDatasourceSpecification.region = "us-east-2";
        snowflakeDatasourceSpecification.warehouseName = "INTEGRATION_WH1";
        snowflakeDatasourceSpecification.databaseName = "INTEGRATION_DB1";
        snowflakeDatasourceSpecification.cloudType = "aws";
        snowflakeDatasourceSpecification.role = "INTEGRATION_ROLE1";

        SnowflakePublicAuthenticationStrategy authSpec = new SnowflakePublicAuthenticationStrategy();
        authSpec.privateKeyVaultReference = "encrypted_private_key";
        authSpec.passPhraseVaultReference = "private_key_encryption_password";
        authSpec.publicUserName = "INTEGRATION_USER1";

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(snowflakeDatasourceSpecification, authSpec, DatabaseType.Snowflake);
        conn.type = DatabaseType.Snowflake;           // for compatibility with legacy DatabaseConnection
        conn.element = null;

        return conn;
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
