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
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ConnectionFactoryTest
{
    @Test
    public void testStoreInstanceManagement()
    {
        TestEnv env = TestEnv.create();
        StoreInstance storeInstance = new StoreInstance.Builder(env.environmentConfiguration)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();
        env.connectionFactory.registerStoreInstance(storeInstance);

        // failure
        Exception exception;

        // error: store already registered
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.registerStoreInstance(storeInstance);
        });
        Assert.assertEquals("Can't register store instance: found multiple store instances with identifier 'test-store'", exception.getMessage());

        // error: store not found
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getAuthenticator(new Identity("test"), "unknown");
        });
        Assert.assertEquals("Can't find store instance with identifier 'unknown'", exception.getMessage());
    }

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
                        TestAuthenticationMechanismType.X,
                        TestAuthenticationMechanismType.Y
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");

        // success
        env.connectionFactory.getConnection(env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X()));

        Exception exception;

        // error: store not found
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(env.connectionFactory.getAuthenticator(identity, "unknown", new AuthenticationConfiguration_X()));
        });
        Assert.assertEquals("Can't find store instance with identifier 'unknown'", exception.getMessage());

        // error: unsupported authentication mechanism
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Z()));
        });
        Assert.assertEquals("Can't get authenticator: authentication mechanism 'Z' is not supported by store 'test'. Supported mechanism(s):\n- X (config: AuthenticationConfiguration_X)\n- Y (config: AuthenticationConfiguration_Y)", exception.getMessage());

        // error: unresolvable authentication flow
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Y()));
        });
        Assert.assertEquals("Can't get authenticator: no authentication flow for store 'test' can be resolved for the specified identity using authentication mechanism 'Y' (authentication configuration: AuthenticationConfiguration_Y, connection specification: TestConnectionSpecification)", exception.getMessage());

        // alternate error message when authentication mechanisms are not properly registered
        TestEnv env2 = TestEnv.create(
                Lists.mutable.with(),
                Lists.mutable.with(),
                Lists.mutable.empty(),
                Lists.mutable.with(
                        TestAuthenticationMechanismType.X,
                        TestAuthenticationMechanismType.Y
                )
        ).newStore("test", Lists.mutable.empty());

        // error: unsupported authentication mechanism
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env2.connectionFactory.getConnection(env2.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Z()));
        });
        Assert.assertEquals("Can't get authenticator: authentication mechanism with configuration 'AuthenticationConfiguration_Z' is not supported by store 'test'. Supported mechanism(s):\n- X (config: AuthenticationConfiguration_X)\n- Y (config: AuthenticationConfiguration_Y)", exception.getMessage());

        // error: unresolvable authentication flow
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env2.connectionFactory.getConnection(env2.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_Y()));
        });
        Assert.assertEquals("Can't get authenticator: no authentication flow for store 'test' can be resolved for the specified identity using authentication mechanism with configuration 'AuthenticationConfiguration_Y' (authentication configuration: AuthenticationConfiguration_Y, connection specification: TestConnectionSpecification)", exception.getMessage());
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
                        TestAuthenticationMechanismType.X,
                        TestAuthenticationMechanismType.Y
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test");
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env.connectionFactory, authenticator, Credential.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_B());
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env.connectionFactory, authenticator, Credential_B.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X,
                        TestAuthenticationMechanismType.Y,
                        TestAuthenticationMechanismType.Z
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());

        // success
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test");
        assertAuthenticator(env.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
                "Credential_A->Credential_B [AuthenticationConfiguration_Y]"
        ), ConnectionBuilder_B.class);

        // error: unresolvable authentication flow
        Exception exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getAuthenticator(new Identity("test"), "test");
        });
        Assert.assertEquals("Can't get authenticator: no authentication flow for store 'test' can be resolved for the specified identity using auto-generated authentication configuration. Try specifying an authentication mechanism by providing a configuration of one of the following types:\n- AuthenticationConfiguration_X (mechanism: X)\n- AuthenticationConfiguration_Z (mechanism: Z)", exception.getMessage());
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());
        Authenticator authenticator = env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        authenticator = env2.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X());
        assertAuthenticator(env2.connectionFactory, authenticator, Credential_A.class, Lists.mutable.with(
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
                        TestAuthenticationMechanismType.X
                )
        ).newStore("test", Lists.mutable.empty());

        Identity identity = new Identity("test", new Credential_A());
        Exception exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            env.connectionFactory.getConnection(env.connectionFactory.getAuthenticator(identity, "test", new AuthenticationConfiguration_X()));
        });
        Assert.assertEquals("Can't get authenticator: no authentication flow for store 'test' can be resolved for the specified identity using authentication mechanism 'X' (authentication configuration: AuthenticationConfiguration_X, connection specification: TestConnectionSpecification)", exception.getMessage());
    }

    private void assertAuthenticator(ConnectionFactory connectionFactory, Authenticator authenticator, Class<? extends Credential> sourceCredentialType, List<String> credentialBuilders, Class<? extends ConnectionBuilder> connectionBuilderType) throws Exception
    {
        Assert.assertEquals(sourceCredentialType, authenticator.getSourceCredentialType());
        Assert.assertEquals(connectionBuilderType, authenticator.getConnectionBuilder().getClass());
        Assert.assertArrayEquals(credentialBuilders.toArray(), authenticator.getCredentialBuilders().stream().map(builder -> String.format("%s->%s [%s]", builder.getInputCredentialType().getSimpleName(), builder.getOutputCredentialType().getSimpleName(), builder.getAuthenticationConfigurationType().getSimpleName())).toArray());
        connectionFactory.getConnection(authenticator);
    }

    private static class TestEnv
    {
        final EnvironmentConfiguration environmentConfiguration;
        final ConnectionFactory connectionFactory;

        private TestEnv(List<CredentialBuilder<?, ?, ?>> credentialBuilders, List<ConnectionBuilder<?, ?, ?>> connectionBuilders, List<AuthenticationMechanism> authenticationMechanisms, List<AuthenticationMechanism> supportedAuthenticationMechanisms)
        {
            this.environmentConfiguration = new EnvironmentConfiguration.Builder()
                    .withStoreSupport(new StoreSupport.Builder()
                            .withIdentifier("test")
                            .withAuthenticationMechanisms(supportedAuthenticationMechanisms)
                            .build())
                    .withAuthenticationMechanisms(authenticationMechanisms)
                    .build();

            this.connectionFactory = new ConnectionFactory.Builder(environmentConfiguration)
                    .withCredentialBuilders(credentialBuilders)
                    .withConnectionBuilders(connectionBuilders)
                    .build();
        }

        TestEnv newStore(String identifier, List<AuthenticationMechanism> authenticationMechanisms)
        {
            this.connectionFactory.registerStoreInstance(new StoreInstance.Builder(environmentConfiguration)
                    .withIdentifier(identifier)
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanisms(authenticationMechanisms)
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build());
            return this;
        }

        static TestEnv create()
        {
            return new TestEnv(Lists.mutable.empty(), Lists.mutable.empty(), Lists.mutable.empty(), Lists.mutable.empty());
        }

        static TestEnv create(List<CredentialBuilder<?, ?, ?>> credentialBuilders, List<ConnectionBuilder<?, ?, ?>> connectionBuilders, List<AuthenticationMechanism> authenticationMechanisms, List<AuthenticationMechanism> supportedAuthenticationMechanisms)
        {
            return new TestEnv(credentialBuilders, connectionBuilders, authenticationMechanisms, supportedAuthenticationMechanisms);
        }

        static TestEnv create(List<CredentialBuilder<?, ?, ?>> credentialBuilders, List<ConnectionBuilder<?, ?, ?>> connectionBuilders, List<AuthenticationMechanism> supportedAuthenticationMechanisms)
        {
            return new TestEnv(credentialBuilders, connectionBuilders, Lists.mutable.with(
                    TestAuthenticationMechanismType.X,
                    TestAuthenticationMechanismType.Y,
                    TestAuthenticationMechanismType.Z
            ), supportedAuthenticationMechanisms);
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
    }

    private static class AuthenticationConfiguration_Y extends AuthenticationConfiguration
    {
    }

    private static class AuthenticationConfiguration_Z extends AuthenticationConfiguration
    {
    }

    private enum TestAuthenticationMechanismType implements AuthenticationMechanism
    {
        X
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_X.class;
                    }
                },
        Y
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_Y.class;
                    }

                    @Override
                    public AuthenticationConfiguration generateConfiguration()
                    {
                        return new AuthenticationConfiguration_Y();
                    }
                },
        Z
                {
                    @Override
                    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType()
                    {
                        return AuthenticationConfiguration_Z.class;
                    }
                };

        @Override
        public String getLabel()
        {
            return this.toString();
        }
    }

    private static class CredentialBuilder_A_to_A__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_A, Credential_A>
    {
        @Override
        public Credential_A makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_A credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_A();
        }
    }

    private static class CredentialBuilder_A_to_B__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_A, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_A credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialBuilder_B_to_C__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential_B, Credential_C>
    {
        @Override
        public Credential_C makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential_B credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_C();
        }
    }

    private static class CredentialBuilder_Any_to_A__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential, Credential_A>
    {
        @Override
        public Credential_A makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfiguration, Credential credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_A();
        }
    }

    private static class CredentialBuilder_A_to_B__withY extends CredentialBuilder<AuthenticationConfiguration_Y, Credential_A, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_Y authenticationConfiguration, Credential_A credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialBuilder_Any_to_B__withX extends CredentialBuilder<AuthenticationConfiguration_X, Credential, Credential_B>
    {
        @Override
        public Credential_B makeCredential(Identity identity, AuthenticationConfiguration_X authenticationConfigurationX, Credential credential, EnvironmentConfiguration configuration) throws Exception
        {
            return new Credential_B();
        }
    }

    private static class CredentialExtractor_A__withX extends CredentialExtractor<AuthenticationConfiguration_X, Credential_A>
    {
    }

    // -------------------------- Connection -------------------------------

    private static class TestConnectionSpecification extends ConnectionSpecification
    {
    }

    private static class ConnectionBuilder_A extends ConnectionBuilder<Object, Credential_A, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(Credential_A credential, TestConnectionSpecification connectionSpecification, StoreInstance storeInstance) throws Exception
        {
            return null;
        }
    }

    private static class ConnectionBuilder_B extends ConnectionBuilder<Object, Credential_B, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(Credential_B credential, TestConnectionSpecification connectionSpecification, StoreInstance storeInstance) throws Exception
        {
            return null;
        }
    }

    private static class ConnectionBuilder_C extends ConnectionBuilder<Object, Credential_C, TestConnectionSpecification>
    {
        @Override
        public Object getConnection(Credential_C credential, TestConnectionSpecification connectionSpecification, StoreInstance storeInstance) throws Exception
        {
            return null;
        }
    }
}
