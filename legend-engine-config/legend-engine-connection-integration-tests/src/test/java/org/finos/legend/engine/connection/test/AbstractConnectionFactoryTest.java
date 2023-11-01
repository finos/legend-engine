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
import org.finos.legend.authentication.vault.impl.EnvironmentCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.connection.AuthenticationMechanism;
import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.Connection;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseSupport;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.IdentitySpecification;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.impl.CoreAuthenticationMechanismType;
import org.finos.legend.connection.impl.KerberosCredentialExtractor;
import org.finos.legend.connection.impl.KeyPairCredentialBuilder;
import org.finos.legend.connection.impl.RelationalDatabaseType;
import org.finos.legend.connection.impl.SnowflakeConnectionBuilder;
import org.finos.legend.connection.impl.StaticJDBCConnectionBuilder;
import org.finos.legend.connection.impl.UserPasswordCredentialBuilder;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.UserPasswordAuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractConnectionFactoryTest<T>
{
    protected LegendEnvironment environment;
    protected IdentityFactory identityFactory;
    protected ConnectionFactory connectionFactory;

    @BeforeEach
    public void initialize()
    {
        this.setup();

        LegendEnvironment.Builder environmentBuilder = LegendEnvironment.builder()
                .vaults(
                        new SystemPropertiesCredentialVault(),
                        new EnvironmentCredentialVault()
                )
                .databaseSupports(
                        DatabaseSupport.builder()
                                .type(RelationalDatabaseType.POSTGRES)
                                .authenticationMechanisms(
                                        AuthenticationMechanism.builder()
                                                .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                                .authenticationConfigurationTypes(
                                                        UserPasswordAuthenticationConfiguration.class
                                                ).build()
                                )
                                .build(),
                        DatabaseSupport.builder()
                                .type(RelationalDatabaseType.SNOWFLAKE)
                                .authenticationMechanisms(
                                        AuthenticationMechanism.builder()
                                                .type(CoreAuthenticationMechanismType.KEY_PAIR)
                                                .authenticationConfigurationTypes(
                                                        EncryptedPrivateKeyPairAuthenticationConfiguration.class
                                                ).build()
                                )
                                .build()
                );

        CredentialVault credentialVault = this.getCredentialVault();
        if (credentialVault != null)
        {
            environmentBuilder.vault(credentialVault);
        }

        this.environment = environmentBuilder.build();

        this.identityFactory = IdentityFactory.builder()
                .environment(this.environment)
                .build();

        this.connectionFactory = ConnectionFactory.builder()
                .environment(this.environment)
                .credentialBuilders(
                        new KerberosCredentialExtractor(),
                        new UserPasswordCredentialBuilder(),
                        new KeyPairCredentialBuilder()
                )
                .connectionBuilders(
                        new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword(),
                        new SnowflakeConnectionBuilder.WithKeyPair()
                )
                .build();
    }

    @AfterEach
    public void shutdown()
    {
        this.cleanup();
    }

    public abstract void setup();

    public abstract void cleanup();

    public CredentialVault getCredentialVault()
    {
        return null;
    }

    public abstract Identity getIdentity();

    public abstract DatabaseType getDatabaseType();

    public abstract ConnectionSpecification getConnectionSpecification();

    public abstract AuthenticationConfiguration getAuthenticationConfiguration();

    public abstract void runTestWithConnection(T connection) throws Exception;

    @Test
    public void runTest() throws Exception
    {
        Identity identity = this.getIdentity();
        DatabaseType databaseType = this.getDatabaseType();
        ConnectionSpecification connectionSpecification = this.getConnectionSpecification();
        AuthenticationConfiguration authenticationConfiguration = this.getAuthenticationConfiguration();

        Connection databaseConnection = Connection.builder()
                .databaseSupport(this.environment.getDatabaseSupport(databaseType))
                .identifier("test::connection")
                .connectionSpecification(connectionSpecification)
                .authenticationConfiguration(authenticationConfiguration)
                .build();

        Authenticator authenticator = this.connectionFactory.getAuthenticator(identity, databaseConnection);
        T connection = this.connectionFactory.getConnection(identity, authenticator);

        this.runTestWithConnection(connection);
        System.out.println("Successfully established and checked connection!");
    }

    // ------------------------------ Utilities ---------------------------------

    protected static Identity getAnonymousIdentity(IdentityFactory identityFactory)
    {
        return identityFactory.createIdentity(
                IdentitySpecification.builder()
                        .name("test-user")
                        .build()
        );
    }
}
