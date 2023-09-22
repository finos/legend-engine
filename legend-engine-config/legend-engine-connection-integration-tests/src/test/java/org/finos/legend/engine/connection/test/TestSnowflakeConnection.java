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
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.impl.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.connection.protocol.SnowflakeConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assume.assumeTrue;

public class TestSnowflakeConnection
{
    public static class WithKeyPair extends AbstractConnectionFactoryTest<Connection>
    {
        private static final String TEST_SNOWFLAKE_PK = "TEST_SNOWFLAKE_PK";
        private static final String TEST_SNOWFLAKE_PK_PASSPHRASE = "TEST_SNOWFLAKE_PK_PASSPHRASE";
        private String snowflakePrivateKey;
        private String snowflakePassPhrase;

        @Override
        public void setup()
        {
            try
            {
                this.snowflakePrivateKey = this.environmentConfiguration.lookupVaultSecret(new EnvironmentCredentialVaultSecret(TEST_SNOWFLAKE_PK), null);
                this.snowflakePassPhrase = this.environmentConfiguration.lookupVaultSecret(new EnvironmentCredentialVaultSecret(TEST_SNOWFLAKE_PK_PASSPHRASE), null);
            }
            catch (Exception e)
            {
                assumeTrue("Can't retrieve Snowflake test instance key-pair info (TEST_SNOWFLAKE_PK, TEST_SNOWFLAKE_PK_PASSPHRASE)", false);
            }
        }

        @Override
        public void cleanup()
        {
            // do nothing
        }

        @Override
        public CredentialVault getCredentialVault()
        {
            Properties properties = new Properties();
            properties.put("snowflakePkRef", this.snowflakePrivateKey);
            properties.put("snowflakePkPassphraseRef", this.snowflakePassPhrase);
            return new PropertiesFileCredentialVault(properties);
        }

        @Override
        public StoreInstance getStoreInstance()
        {
            SnowflakeConnectionSpecification connectionSpecification = new SnowflakeConnectionSpecification();
            connectionSpecification.databaseName = "SUMMIT_DEV";
            connectionSpecification.accountName = "ki79827";
            connectionSpecification.warehouseName = "SUMMIT_DEV";
            connectionSpecification.region = "us-east-2";
            connectionSpecification.cloudType = "aws";
            connectionSpecification.role = "SUMMIT_DEV";
            return new StoreInstance.Builder(this.environmentConfiguration)
                    .withIdentifier(TEST_STORE_INSTANCE_NAME)
                    .withStoreSupportIdentifier("Snowflake")
                    .withAuthenticationMechanisms(
                            AuthenticationMechanismType.KEY_PAIR
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
            return new EncryptedPrivateKeyPairAuthenticationConfiguration(
                    "SUMMIT_DEV1",
                    new PropertiesFileSecret("snowflakePkRef"),
                    new PropertiesFileSecret("snowflakePkPassphraseRef")
            );
        }

        @Override
        public void runTestWithConnection(Connection connection) throws Exception
        {
            Statement statement = connection.createStatement();
            statement.setMaxRows(10);
            statement.executeQuery("select * from INFORMATION_SCHEMA.DATABASES;");
        }
    }
}
