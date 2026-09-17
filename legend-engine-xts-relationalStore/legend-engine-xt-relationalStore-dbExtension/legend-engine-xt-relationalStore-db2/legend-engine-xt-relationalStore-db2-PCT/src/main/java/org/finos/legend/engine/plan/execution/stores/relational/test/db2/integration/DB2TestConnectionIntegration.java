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

package org.finos.legend.engine.plan.execution.stores.relational.test.db2.integration;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

public class DB2TestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    public CustomDB2Container db2Container = new CustomDB2Container();
    private TestVaultImplementation vault;
    private static final String USERNAME_REFERENCE = "db2_username";
    private static final String PASSWORD_REFERENCE = "db2_password";
    private static final int DEFAULT_STARTUP_ATTEMPTS = 3;
    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "Relational", "DB2");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.DB2;
    }

    @Override
    public void setup()
    {
        this.db2Container.withStartupAttempts(DEFAULT_STARTUP_ATTEMPTS);
        this.db2Container.withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        this.db2Container.start();

        this.vault =
                new TestVaultImplementation()
                        .withValue(USERNAME_REFERENCE, db2Container.getUsername())
                        .withValue(PASSWORD_REFERENCE, db2Container.getPassword());
        Vault.INSTANCE.registerImplementation(this.vault);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!db2Container.isRunning())
        {
            this.setup();
        }
        StaticDatasourceSpecification datasourceSpec = new StaticDatasourceSpecification();
        datasourceSpec.host = db2Container.getHost();
        datasourceSpec.port = db2Container.getFirstMappedPort();
        datasourceSpec.databaseName = db2Container.getDatabaseName();

        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.userNameVaultReference = USERNAME_REFERENCE;
        authSpec.passwordVaultReference = PASSWORD_REFERENCE;

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(datasourceSpec, authSpec, DatabaseType.DB2);
        conn.type = DatabaseType.DB2;
        conn.element = null;
        return conn;
    }

    @Override
    public void cleanup()
    {
        if (this.db2Container != null && this.db2Container.isRunning())
        {
            this.db2Container.stop();
        }
        if (this.vault != null)
        {
            Vault.INSTANCE.unregisterImplementation(this.vault);
            this.vault = null;
        }
    }

    @Override
    public void start()
    {
        this.setup();
    }

    @Override
    public void shutDown()
    {
        this.cleanup();
    }
}
