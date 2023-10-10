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

package org.finos.legend.engine.connection.test;

import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.PostgresTestContainerWrapper;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;
import org.finos.legend.connection.protocol.StaticJDBCConnectionSpecification;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestPostgresConnection
{
    public static class WithUserPassword extends AbstractConnectionFactoryTest<Connection>
    {
        private PostgresTestContainerWrapper postgresContainer;

        @Override
        public void setup()
        {
            try
            {
                this.postgresContainer = PostgresTestContainerWrapper.build();
                this.postgresContainer.start();
            }
            catch (Exception e)
            {
                assumeTrue(false, "Can't start PostgreSQLContainer");
            }
        }

        @Override
        public void cleanup()
        {
            if (this.postgresContainer != null)
            {
                this.postgresContainer.stop();
            }
        }

        @Override
        public CredentialVault getCredentialVault()
        {
            Properties properties = new Properties();
            properties.put("passwordRef", this.postgresContainer.getPassword());
            return new PropertiesFileCredentialVault(properties);
        }

        @Override
        public StoreInstance getStoreInstance()
        {
            ConnectionSpecification connectionSpecification = new StaticJDBCConnectionSpecification(
                    this.postgresContainer.getHost(),
                    this.postgresContainer.getPort(),
                    this.postgresContainer.getDatabaseName()
            );
            return new StoreInstance.Builder(this.environment)
                    .withIdentifier(TEST_STORE_INSTANCE_NAME)
                    .withStoreSupportIdentifier("Postgres")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).build()
                    )
                    .withConnectionSpecification(connectionSpecification)
                    .build();
        }

        @Override
        public Identity getIdentity()
        {
            return getAnonymousIdentity(this.identityFactory);
        }

        @Override
        public AuthenticationConfiguration getAuthenticationConfiguration()
        {
            return new UserPasswordAuthenticationConfiguration(
                    postgresContainer.getUser(),
                    new PropertiesFileSecret("passwordRef")
            );
        }

        @Override
        public void runTestWithConnection(Connection connection) throws Exception
        {
            Statement statement = connection.createStatement();
            statement.setMaxRows(10);
            statement.executeQuery("select * from pg_catalog.pg_database;");
        }
    }
}
