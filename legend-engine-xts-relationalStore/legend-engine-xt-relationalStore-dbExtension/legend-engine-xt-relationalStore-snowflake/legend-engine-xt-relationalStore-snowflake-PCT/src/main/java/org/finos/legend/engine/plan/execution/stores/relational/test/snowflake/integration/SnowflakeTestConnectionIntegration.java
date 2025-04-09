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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import software.amazon.awssdk.regions.Region;


public class SnowflakeTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private final RelationalDatabaseConnection conn = new RelationalDatabaseConnection();

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
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        SnowflakePublicAuthenticationStrategy authSpec = new SnowflakePublicAuthenticationStrategy();

        String pctProperties = System.getProperty("pct.external.resources.properties", System.getenv("PCT_EXTERNAL_RESOURCES_PROPERTIES"));
        Path localPctProperties = Paths.get(pctProperties != null ? pctProperties : "");

        String awsAccessKeyId = System.getProperty("AWS_ACCESS_KEY_ID", System.getenv("AWS_ACCESS_KEY_ID"));
        String awsSecretAccessKey = System.getProperty("AWS_SECRET_ACCESS_KEY", System.getenv("AWS_SECRET_ACCESS_KEY"));

        if (!Files.isDirectory(localPctProperties) && Files.isReadable(localPctProperties))
        {
            try (InputStream is = Files.newInputStream(localPctProperties))
            {
                Properties properties = new Properties();
                properties.load(is);

                Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));

                snowflakeDatasourceSpecification.accountName = properties.getProperty("snowflake.spec.accountName");
                snowflakeDatasourceSpecification.region = properties.getProperty("snowflake.spec.region");
                snowflakeDatasourceSpecification.warehouseName = properties.getProperty("snowflake.spec.warehouseName");
                snowflakeDatasourceSpecification.databaseName = properties.getProperty("snowflake.spec.databaseName");
                snowflakeDatasourceSpecification.cloudType = properties.getProperty("snowflake.spec.cloudType");
                snowflakeDatasourceSpecification.role = properties.getProperty("snowflake.spec.role");

                authSpec.publicUserName = properties.getProperty("snowflake.auth.publicUserName");
                authSpec.privateKeyVaultReference = "snowflake.auth.privateKey";
                authSpec.passPhraseVaultReference = "snowflake.auth.passPhrase";
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        else if (!StringUtils.isEmpty(awsAccessKeyId) && !StringUtils.isEmpty(awsSecretAccessKey))
        {
            Vault.INSTANCE.registerImplementation(
                    new AWSVaultImplementation(
                            awsAccessKeyId,
                            awsSecretAccessKey,
                            Region.US_EAST_1,
                            "snowflake.INTEGRATION_USER1"
                    )
            );

            snowflakeDatasourceSpecification.accountName = "ki79827";
            snowflakeDatasourceSpecification.region = "us-east-2";
            snowflakeDatasourceSpecification.warehouseName = "INTEGRATION_WH1";
            snowflakeDatasourceSpecification.databaseName = "INTEGRATION_DB1";
            snowflakeDatasourceSpecification.cloudType = "aws";
            snowflakeDatasourceSpecification.role = "INTEGRATION_ROLE1";

            authSpec.privateKeyVaultReference = "encrypted_private_key";
            authSpec.passPhraseVaultReference = "private_key_encryption_password";
            authSpec.publicUserName = "INTEGRATION_USER1";
        }
        else
        {
            throw new IllegalStateException("Cannot initialize Snowflake integration connection");
        }


        conn.type = DatabaseType.Snowflake;
        conn.databaseType = DatabaseType.Snowflake;
        conn.element = null;
        conn.datasourceSpecification = snowflakeDatasourceSpecification;
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
