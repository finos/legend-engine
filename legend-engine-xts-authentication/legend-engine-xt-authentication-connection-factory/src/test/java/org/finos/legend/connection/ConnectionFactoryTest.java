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

package org.finos.legend.connection;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.connection.impl.InstrumentedStoreInstanceProvider;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class ConnectionFactoryTest
{
    @Test
    public void testGetConnection_WithFailures() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_Any_to_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build(),
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.Y)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_Y.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");

        // success
        env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X()));

        Exception exception;

        // error: store not found
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "unknown", new AuthenticationConfiguration_X()));
        });
        Assertions.assertEquals("Can't find store instance with identifier 'unknown'", exception.getMessage());

        // error: unsupported authentication mechanism
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", TestAuthenticationMechanismType.Z));
        });
        Assertions.assertEquals("Store 'test' does not support authentication mechanism 'Z'. Supported mechanism(s):\n" +
                "- X\n" +
                "- Y", exception.getMessage());

        // error: authentication mechanism does not come with a default config generator
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", TestAuthenticationMechanismType.X));
        });
        Assertions.assertEquals("Can't auto-generate authentication configuration for store 'test' with authentication mechanism 'X'. Please provide a configuration of one of the following type(s):\n" +
                "- AuthenticationConfiguration_X", exception.getMessage());

        // error: unsupported authentication configuration
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Z()));
        });
        Assertions.assertEquals("Store 'test' does not accept authentication configuration type 'AuthenticationConfiguration_Z'. Supported configuration type(s):\n" +
                "- AuthenticationConfiguration_X\n" +
                "- AuthenticationConfiguration_Y", exception.getMessage());

        // error: unresolvable authentication flow
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Y()));
        });
        Assertions.assertEquals("No authentication flow for store 'test' can be resolved for the specified identity (authentication configuration: AuthenticationConfiguration_Y, connection specification: TestConnectionSpecification)", exception.getMessage());
    }

    /**
     * Test Case: Any -> A -> [Connection]
     */
    @Test
    public void testGetConnection_WithSimpleFlow() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_Any_to_A__withX(),
                        new CredentialBuilder_Any_to_B__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A(),
                        new ConnectionBuilder_B()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build(),
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.Y)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_Y.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
                "Credential->Credential_A [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_A.class);
    }

    /**
     * Test Case: Any -> B -> [Connection]
     */
    @Test
    public void testGetConnection_WithSpecificBuilderOrder() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        // if Any -> B credential builder is specified first, it will take precedence over Any -> A
                        new CredentialBuilder_Any_to_B__withX(),
                        new CredentialBuilder_Any_to_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A(),
                        new ConnectionBuilder_B()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
                "Credential->Credential_B [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_B.class);
    }

    /**
     * Test Case: Any -> A -> B -> C -> [Connection]
     */
    @Test
    public void testGetConnection_WithChainFlow() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_A_to_B__withX(),
                        new CredentialBuilder_B_to_C__withX(),
                        new CredentialBuilder_Any_to_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_C()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
                "Credential->Credential_A [AuthenticationConfiguration_X]",
                "Credential_A->Credential_B [AuthenticationConfiguration_X]",
                "Credential_B->Credential_C [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_C.class);
    }

    /**
     * Test Case: B -> C -> [Connection]
     */
    @Test
    public void testGetConnection_WithShortestFlowResolved() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_A_to_B__withX(),
                        new CredentialBuilder_B_to_C__withX(),
                        new CredentialBuilder_Any_to_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_C()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_B());
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential_B.class, Lists.mutable.with(
                "Credential_B->Credential_C [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_C.class);
    }

    /**
     * Test Case: A -> B -> [Connection]
     */
    @Test
    public void testGetConnection_WithNoAuthConfigProvided() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_A_to_B__withY()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A(),
                        new ConnectionBuilder_B()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build(),
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.Y)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_Y.class)
                                .withDefaultAuthenticationConfigurationGenerator(AuthenticationConfiguration_Y::new)
                                .build(),
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.Z)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_Z.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());

        // success
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test");
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
                "Credential_A->Credential_B [AuthenticationConfiguration_Y]"
        ), ConnectionBuilder_B.class);

        // error: unresolvable authentication flow
        Exception exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getAuthenticator(new Identity("test"), "test");
        });
        Assertions.assertEquals("No authentication flow for store 'test' can be resolved for the specified identity. Try specifying an authentication mechanism or authentication configuration. Supported configuration type(s):\n" +
                "- AuthenticationConfiguration_X (X)\n" +
                "- AuthenticationConfiguration_Y (Y)\n" +
                "- AuthenticationConfiguration_Z (Z)", exception.getMessage());
    }

    /**
     * Test Case: A -> A -> [Connection]
     */
    @Test
    public void testGetConnection_WithCredentialExtractor() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        new CredentialExtractor_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
                "Credential_A->Credential_A [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_A.class);

        // if no extractor is specified, but an extractor like credential builder is, this should still work
        TestEnv env2 = TestEnv.create(
                Lists.mutable.with(
                        new CredentialBuilder_A_to_A__withX()
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        authenticator = env2.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(identity, env2.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
                "Credential_A->Credential_A [AuthenticationConfiguration_X]"
        ), ConnectionBuilder_A.class);
    }

    /**
     * Test Case: A x A -> [Connection]
     * No credential extractor is provided, short-circuit resolution should NOT happen
     */
    @Test
    public void testGetConnection_WithoutCredentialExtractor() throws Exception
    {
        TestEnv env = TestEnv.create(
                Lists.mutable.with(
                        // no extractor is specified
                ),
                Lists.mutable.with(
                        new ConnectionBuilder_A()
                ),
                Lists.mutable.with(
                        new AuthenticationMechanismConfiguration.Builder(TestAuthenticationMechanismType.X)
                                .withAuthenticationConfigurationTypes(AuthenticationConfiguration_X.class)
                                .build()
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());
        Exception exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(identity, env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X()));
        });
        Assertions.assertEquals("No authentication flow for store 'test' can be resolved for the specified identity (authentication configuration: AuthenticationConfiguration_X, connection specification: TestConnectionSpecification)", exception.getMessage());
    }

    private void assertAuthenticator(Identity identity, ConnectionFactory connectionFactory, Authenticator<?> authenticator, Class<? extends Credential> sourceCredentialType, List<String> credentialBuilders, Class<? extends ConnectionBuilder> connectionBuilderType) throws Exception
    {
        Assertions.assertEquals(sourceCredentialType, authenticator.getSourceCredentialType());
        Assertions.assertEquals(connectionBuilderType, authenticator.getConnectionBuilder().getClass());
        Assertions.assertArrayEquals(credentialBuilders.toArray(), authenticator.getCredentialBuilders().stream().map(builder -> String.format("%s->%s [%s]", builder.getInputCredentialType().getSimpleName(), builder.getOutputCredentialType().getSimpleName(), builder.getAuthenticationConfigurationType().getSimpleName())).toArray());
        connectionFactory.getConnection(identity, authenticator);
    }

    private static class TestEnv
    {
        final LegendEnvironment environment;
        final InstrumentedStoreInstanceProvider storeInstanceProvider;
        final ConnectionFactory connectionFactory;

        private TestEnv(List<CredentialBuilder> credentialBuilders, List<ConnectionBuilder> connectionBuilders, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
        {
            this.environment = new LegendEnvironment.Builder()
                    .withStoreSupport(new StoreSupport.Builder()
                            .withIdentifier("test")
                            .withAuthenticationMechanismConfigurations(authenticationMechanismConfigurations)
                            .build())
                    .build();
            this.storeInstanceProvider = new InstrumentedStoreInstanceProvider();
            this.connectionFactory = new ConnectionFactory.Builder(this.environment, this.storeInstanceProvider)
                    .withCredentialBuilders(credentialBuilders)
                    .withConnectionBuilders(connectionBuilders)
                    .build();
        }

        TestEnv newStore(String identifier, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
        {
            this.storeInstanceProvider.injectStoreInstance(new StoreInstance.Builder(this.environment)
                    .withIdentifier(identifier)
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanismConfigurations(authenticationMechanismConfigurations)
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build());
            return this;
        }

        static TestEnv create()
        {
            return new TestEnv(Lists.mutable.empty(), Lists.mutable.empty(), Lists.mutable.empty());
        }

        static TestEnv create(List<CredentialBuilder> credentialBuilders, List<ConnectionBuilder> connectionBuilders, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
        {
            return new TestEnv(credentialBuilders, connectionBuilders, authenticationMechanismConfigurations);
        }
    }

    // -------------------------- Credential -------------------------------

    private static class Credential_A implements Credential
    {
    }

    private static class Credential_B implements Credential
    {
    }

    private static class Credential_C implements Credential
    {
    }

    // -------------------------- Authentication -------------------------------

    private static class AuthenticationConfiguration_X extends AuthenticationConfiguration
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }

    private static class AuthenticationConfiguration_Y extends AuthenticationConfiguration
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }

    private static class AuthenticationConfiguration_Z extends AuthenticationConfiguration
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }

    private enum TestAuthenticationMechanismType implements AuthenticationMechanism
    {
        X,
        Y,
        Z;

        @Override
        public String getLabel()
        {
            return this.toString();
        }
    }

    private static class CredentialBuilder_A_to_A__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_A, Credential_A>
    {
        @Override
        public Credential_A makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_A credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_A();
        }
    }

    private static class CredentialBuilder_A_to_B__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_A, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_A credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialBuilder_B_to_C__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_B, Credential_C>
    {
        @Override
        public Credential_C makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_B credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_C();
        }
    }

    private static class CredentialBuilder_Any_to_A__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential, Credential_A>
    {
        @Override
        public Credential_A makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_A();
        }
    }

    private static class CredentialBuilder_A_to_B__withY extends CredentialBuilder<AuthenticationConfiguration_Y, Credential_A, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_Y authenticationConfiguration, Credential_A credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialBuilder_Any_to_B__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfigurationX, Credential credential, LegendEnvironment environment) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialExtractor_A__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_A, Credential_A>
    {
        @Override
        public Credential_A makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_A credential, LegendEnvironment environment) throws Exception
        {

            Optional<Credential_A> credentialOptional = identity.getCredential(Credential_A.class);
            if (!credentialOptional.isPresent())
            {
                throw new RuntimeException("");
            }
            return credentialOptional.get();
        }
    }

    // -------------------------- Connection -------------------------------

    private static class TestConnectionManager implements ConnectionManager
    {
        @Override
        public void initialize(LegendEnvironment environment)
        {
        }
    }

    private static class TestConnectionSpecification extends ConnectionSpecification
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }

    private static class ConnectionBuilder_A extends ConnectionBuilder<Object, Credential_A, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(TestConnectionSpecification connectionSpecification, Authenticator<Credential_A> authenticator, Identity identity) throws Exception
        {
            return null;
        }

        @Override
        public ConnectionManager getConnectionManager()
        {
            return new TestConnectionManager();
        }
    }

    private static class ConnectionBuilder_B extends ConnectionBuilder<Object, Credential_B, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(TestConnectionSpecification connectionSpecification, Authenticator<Credential_B> authenticator, Identity identity) throws Exception
        {
            return null;
        }

        @Override
        public ConnectionManager getConnectionManager()
        {
            return new TestConnectionManager();
        }
    }

    private static class ConnectionBuilder_C extends ConnectionBuilder<Object, Credential_C, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(TestConnectionSpecification connectionSpecification, Authenticator<Credential_C> authenticator, Identity identity) throws Exception
        {
            return null;
        }

        @Override
        public ConnectionManager getConnectionManager()
        {
            return new TestConnectionManager();
        }
    }
}
