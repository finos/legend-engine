// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.athena.integration;

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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AthenaDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import software.amazon.awssdk.regions.Region;


public class AthenaTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private final RelationalDatabaseConnection conn = new RelationalDatabaseConnection();

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Athena");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Athena;
    }

    @Override
    public void setup()
    {
        AthenaDatasourceSpecification athenaDatasourceSpecification = new AthenaDatasourceSpecification();
        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();

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

                athenaDatasourceSpecification.databaseName = properties.getProperty("athena.spec.databaseName");
                athenaDatasourceSpecification.awsRegion = properties.getProperty("athena.spec.region");
                athenaDatasourceSpecification.workgroup = properties.getProperty("athena.spec.workgroup");
                athenaDatasourceSpecification.s3OutputLocation = properties.getProperty("athena.spec.s3OutputLocation");

                authSpec.userNameVaultReference = "athena.auth.privateKey";
                authSpec.passwordVaultReference = "athena.auth.passPhrase";
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
                            "athena.INTEGRATION_USER1"
                    )
            );

            athenaDatasourceSpecification.databaseName = "my_db";
            athenaDatasourceSpecification.awsRegion = "ap-south-1";
            athenaDatasourceSpecification.workgroup = "athenaPCTworkgroup";
            athenaDatasourceSpecification.s3OutputLocation = "s3://bucket-name/output-folder/";

            authSpec.userNameVaultReference = "encrypted_private_key";
            authSpec.passwordVaultReference = "private_key_encryption_password";
        }
        else
        {
            throw new IllegalStateException("Cannot initialize Athena integration connection");
        }


        conn.type = DatabaseType.Athena;
        conn.databaseType = DatabaseType.Athena;
        conn.element = null;
        conn.datasourceSpecification = athenaDatasourceSpecification;
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
