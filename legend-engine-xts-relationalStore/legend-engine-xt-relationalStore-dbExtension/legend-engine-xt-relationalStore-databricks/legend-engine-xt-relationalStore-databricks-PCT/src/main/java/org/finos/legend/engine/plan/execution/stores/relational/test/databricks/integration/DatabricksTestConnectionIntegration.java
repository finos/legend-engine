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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.integration;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.aws.AWSVaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DatabricksTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private final RelationalDatabaseConnection conn = new RelationalDatabaseConnection();

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Databricks");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Databricks;
    }

    @Override
    public void setup()
    {
        DatabricksDatasourceSpecification dsSpecs = new DatabricksDatasourceSpecification();
        ApiTokenAuthenticationStrategy authSpec = new ApiTokenAuthenticationStrategy();

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

                dsSpecs.hostname = properties.getProperty("databricks.spec.hostname");
                dsSpecs.port = properties.getProperty("databricks.spec.port");
                dsSpecs.protocol = properties.getProperty("databricks.spec.protocol");
                dsSpecs.httpPath = properties.getProperty("databricks.spec.httpPath");

                authSpec.apiToken = "databricks.auth.apiToken";
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
                            "databricks"
                    )
            );

            dsSpecs.hostname = "dbc-f0687849-717f.cloud.databricks.com";
            dsSpecs.port = "443";
            dsSpecs.protocol = "https";
            dsSpecs.httpPath = "/sql/1.0/warehouses/c56852187940e5a3";

            authSpec.apiToken = "integration_test.apitoken";
        }
        else
        {
            throw new IllegalStateException("Cannot initialize Databricks integration connection");
        }

        conn.type = DatabaseType.Databricks;
        conn.databaseType = DatabaseType.Databricks;
        conn.element = null;
        conn.datasourceSpecification = dsSpecs;
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
    public void shutDown()
    {
        this.cleanup();
    }

    @Override
    public void start()
    {
        this.setup();
    }
}