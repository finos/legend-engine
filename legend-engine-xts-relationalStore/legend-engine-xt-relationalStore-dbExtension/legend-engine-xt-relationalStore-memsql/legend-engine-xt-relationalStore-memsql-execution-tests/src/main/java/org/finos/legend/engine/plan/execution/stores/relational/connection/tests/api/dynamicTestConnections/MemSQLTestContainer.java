// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.memsql.test.MemSQLContainerWrapper;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.fail;

public class MemSQLTestContainer implements TestConnectionIntegration
{
    private static final String USERNAME_REFERENCE = "username";
    private static final String PASSWORD_REFERENCE = "password";
    private static final String DATABASE_NAME = "for_testing";

    private final MemSQLContainerWrapper memSQLContainerWrapper = MemSQLContainerWrapper.build("SINGLESTORE_INTEGRATION_LICENSE_KEY");
    private TestVaultImplementation vault;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "MemSQL");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.MemSQL;
    }

    @Override
    public void setup()
    {
        this.memSQLContainerWrapper.start();
        try (Connection connection = this.memSQLContainerWrapper.getConnection();
             Statement statement = connection.createStatement())
        {
            statement.execute("create schema if not exists " + DATABASE_NAME + ";");
            statement.execute("SET GLOBAL maximum_blob_cache_size_mb = 1024");
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        this.vault =
                new TestVaultImplementation()
                        .withValue(USERNAME_REFERENCE, this.memSQLContainerWrapper.getUser())
                        .withValue(PASSWORD_REFERENCE, this.memSQLContainerWrapper.getPassword());
        Vault.INSTANCE.registerImplementation(this.vault);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        StaticDatasourceSpecification memSQLSpecification = new StaticDatasourceSpecification();
        memSQLSpecification.host = this.memSQLContainerWrapper.getHost();
        memSQLSpecification.port = this.memSQLContainerWrapper.getPort();
        memSQLSpecification.databaseName = DATABASE_NAME;

        UserNamePasswordAuthenticationStrategy authStrategy = new UserNamePasswordAuthenticationStrategy();
        authStrategy.userNameVaultReference = USERNAME_REFERENCE;
        authStrategy.passwordVaultReference = PASSWORD_REFERENCE;

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection(memSQLSpecification, authStrategy, DatabaseType.MemSQL);
        connection.type = DatabaseType.MemSQL;
        // placeholder, to be set by tests
        connection.element = "";

        return connection;
    }

    @Override
    public void cleanup()
    {
        this.memSQLContainerWrapper.stop();
        Vault.INSTANCE.unregisterImplementation(this.vault);
    }
}
