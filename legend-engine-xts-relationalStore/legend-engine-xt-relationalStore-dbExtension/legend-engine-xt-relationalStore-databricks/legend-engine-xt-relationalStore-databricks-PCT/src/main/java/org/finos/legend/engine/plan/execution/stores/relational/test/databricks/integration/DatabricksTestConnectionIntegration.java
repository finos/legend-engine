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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

import java.util.Properties;

public class DatabricksTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{

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
        Properties properties = new Properties();
        properties.put("DATABRICKS_API_TOKEN", System.getProperty("DATABRICKS_API_TOKEN"));
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        DatabricksDatasourceSpecification dsSpecs = new DatabricksDatasourceSpecification();
        dsSpecs.hostname = "dbc-f0687849-717f.cloud.databricks.com";
        dsSpecs.port = "443";
        dsSpecs.protocol = "https";
        dsSpecs.httpPath = "/sql/1.0/warehouses/c56852187940e5a3";
        ApiTokenAuthenticationStrategy authSpec = new ApiTokenAuthenticationStrategy();
        authSpec.apiToken = "DATABRICKS_API_TOKEN";
        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(dsSpecs, authSpec, DatabaseType.Databricks);
        conn.type = DatabaseType.Databricks;           // for compatibility with legacy DatabaseConnection
        conn.element = null;

        return conn;

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