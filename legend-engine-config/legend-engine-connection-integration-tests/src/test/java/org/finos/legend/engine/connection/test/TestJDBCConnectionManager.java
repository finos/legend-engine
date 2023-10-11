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

import net.bytebuddy.asm.Advice;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.IdentitySpecification;
import org.finos.legend.connection.JDBCConnectionBuilder;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.PostgresTestContainerWrapper;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.impl.InstrumentedStoreInstanceProvider;
import org.finos.legend.connection.impl.JDBCConnectionManager;
import org.finos.legend.connection.impl.StaticJDBCConnectionBuilder;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;
import org.finos.legend.connection.impl.UserPasswordCredentialBuilder;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.connection.protocol.StaticJDBCConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLTransientConnectionException;
import java.util.Properties;

public class TestJDBCConnectionManager
{
    PostgresTestContainerWrapper postgresContainer;
    private static final String TEST_STORE_INSTANCE_NAME = "test-store";

    private LegendEnvironment environment;
    private IdentityFactory identityFactory;
    private InstrumentedStoreInstanceProvider storeInstanceProvider;
    private ConnectionFactory connectionFactory;
    private StoreInstance storeInstance;

    @BeforeEach
    public void setup()
    {
        postgresContainer = PostgresTestContainerWrapper.build();
        postgresContainer.start();

        Properties properties = new Properties();
        properties.put("passwordRef", this.postgresContainer.getPassword());

        LegendEnvironment.Builder environmentBuilder = new LegendEnvironment.Builder()
                .withVaults(new PropertiesFileCredentialVault(properties))
                .withStoreSupports(
                        new RelationalDatabaseStoreSupport.Builder(DatabaseType.POSTGRES)
                                .withIdentifier("Postgres")
                                .withAuthenticationMechanismConfigurations(
                                        new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).withAuthenticationConfigurationTypes(
                                                UserPasswordAuthenticationConfiguration.class
                                        ).build()
                                )
                                .build()
                );

        this.environment = environmentBuilder.build();
        this.identityFactory = new IdentityFactory.Builder(this.environment)
                .build();
        this.storeInstanceProvider = new InstrumentedStoreInstanceProvider();
        ConnectionSpecification connectionSpecification = new StaticJDBCConnectionSpecification(
                this.postgresContainer.getHost(),
                this.postgresContainer.getPort(),
                this.postgresContainer.getDatabaseName()
        );
        this.storeInstance = new StoreInstance.Builder(this.environment)
                .withIdentifier(TEST_STORE_INSTANCE_NAME)
                .withStoreSupportIdentifier("Postgres")
                .withConnectionSpecification(connectionSpecification)
                .build();
    }

    @AfterEach
    public void cleanUp()
    {
        postgresContainer.stop();

        JDBCConnectionManager.getInstance().flushPool();
    }

    @Test
    public void testBasicConnectionPooling() throws Exception
    {
        JDBCConnectionBuilder customizedJDBCConnectionBuilder = new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword();
        customizedJDBCConnectionBuilder.setConnectionPoolConfig(
                new JDBCConnectionManager.ConnectionPoolConfig.Builder()
                        .withMaxPoolSize(2)
                        .withConnectionTimeout(1000L)
                        .build()
        );
        this.connectionFactory = new ConnectionFactory.Builder(this.environment, this.storeInstanceProvider)
                .withCredentialBuilders(
                        new UserPasswordCredentialBuilder()
                )
                .withConnectionBuilders(
                        customizedJDBCConnectionBuilder
                )
                .build();
        this.storeInstanceProvider.injectStoreInstance(this.storeInstance);
        Identity identity = identityFactory.createIdentity(
                new IdentitySpecification.Builder()
                        .withName("test-user")
                        .build()
        );
        ConnectionSpecification connectionSpecification = this.storeInstance.getConnectionSpecification();
        AuthenticationConfiguration authenticationConfiguration = new UserPasswordAuthenticationConfiguration(
                postgresContainer.getUser(),
                new PropertiesFileSecret("passwordRef")
        );
        Authenticator authenticator = this.connectionFactory.getAuthenticator(identity, TEST_STORE_INSTANCE_NAME, authenticationConfiguration);

        JDBCConnectionManager connectionManager = JDBCConnectionManager.getInstance();
        Assertions.assertEquals(0, connectionManager.getPoolSize());

        // 1. Get a connection, this should initialize the pool as well as create a new connection in the empty pool
        // this connection should be active
        Connection connection0 = this.connectionFactory.getConnection(identity, authenticator);

        String poolName = JDBCConnectionManager.getPoolName(identity, connectionSpecification, authenticationConfiguration);
        JDBCConnectionManager.ConnectionPool connectionPool = connectionManager.getPool(poolName);

        // 2. Close the connection, verify that the pool keeps this connection around in idle state
        Connection underlyingConnection0 = connection0.unwrap(Connection.class);
        connection0.close();

        Assertions.assertEquals(1, connectionPool.getTotalConnections());
        Assertions.assertEquals(0, connectionPool.getActiveConnections());
        Assertions.assertEquals(1, connectionPool.getIdleConnections());

        // 3. Get a new connection, the pool should return the idle connection and create no new connection
        Connection connection1 = this.connectionFactory.getConnection(identity, authenticator);

        Assertions.assertEquals(underlyingConnection0, connection1.unwrap(Connection.class));
        Assertions.assertEquals(1, connectionPool.getTotalConnections());
        Assertions.assertEquals(1, connectionPool.getActiveConnections());
        Assertions.assertEquals(0, connectionPool.getIdleConnections());

        // 4. Get another connection while the first one is still alive and used, a new connection
        // will be created in the pool
        this.connectionFactory.getConnection(identity, authenticator);

        Assertions.assertEquals(2, connectionPool.getTotalConnections());
        Assertions.assertEquals(2, connectionPool.getActiveConnections());
        Assertions.assertEquals(0, connectionPool.getIdleConnections());

        // 5. Get yet another connection while the first and second one are still alive and used, this will
        // exceed the pool size, throwing an error
        Assertions.assertThrows(SQLTransientConnectionException.class, () ->
        {
            this.connectionFactory.getConnection(identity, authenticator);
        });
    }

    @Test
    public void testConnectionPoolingForDifferentIdentities() throws Exception
    {
        this.connectionFactory = new ConnectionFactory.Builder(this.environment, this.storeInstanceProvider)
                .withCredentialBuilders(
                        new UserPasswordCredentialBuilder()
                )
                .withConnectionBuilders(
                        new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword()
                )
                .build();
        this.storeInstanceProvider.injectStoreInstance(this.storeInstance);
        Identity identity1 = identityFactory.createIdentity(
                new IdentitySpecification.Builder()
                        .withName("testUser1")
                        .build()
        );
        Identity identity2 = identityFactory.createIdentity(
                new IdentitySpecification.Builder()
                        .withName("testUser2")
                        .build()
        );
        ConnectionSpecification connectionSpecification = this.storeInstance.getConnectionSpecification();
        AuthenticationConfiguration authenticationConfiguration = new UserPasswordAuthenticationConfiguration(
                postgresContainer.getUser(),
                new PropertiesFileSecret("passwordRef")
        );

        JDBCConnectionManager connectionManager = JDBCConnectionManager.getInstance();
        Assertions.assertEquals(0, connectionManager.getPoolSize());

        // 1. Get a new connection for identity1, which should initialize a pool
        this.connectionFactory.getConnection(identity1, this.connectionFactory.getAuthenticator(identity1, TEST_STORE_INSTANCE_NAME, authenticationConfiguration));

        String poolName1 = JDBCConnectionManager.getPoolName(identity1, connectionSpecification, authenticationConfiguration);
        JDBCConnectionManager.ConnectionPool connectionPool1 = connectionManager.getPool(poolName1);

        Assertions.assertEquals(1, connectionManager.getPoolSize());
        Assertions.assertEquals(1, connectionPool1.getTotalConnections());
        Assertions.assertEquals(1, connectionPool1.getActiveConnections());
        Assertions.assertEquals(0, connectionPool1.getIdleConnections());

        // 2. Get a new connection for identity2, which should initialize another pool
        this.connectionFactory.getConnection(identity2, this.connectionFactory.getAuthenticator(identity2, TEST_STORE_INSTANCE_NAME, authenticationConfiguration));

        String poolName2 = JDBCConnectionManager.getPoolName(identity2, connectionSpecification, authenticationConfiguration);
        JDBCConnectionManager.ConnectionPool connectionPool2 = connectionManager.getPool(poolName2);

        Assertions.assertEquals(2, connectionManager.getPoolSize());
        Assertions.assertEquals(1, connectionPool2.getTotalConnections());
        Assertions.assertEquals(1, connectionPool2.getActiveConnections());
        Assertions.assertEquals(0, connectionPool2.getIdleConnections());
    }

    @Test
    public void testRetryOnBrokenConnection()
    {
        //
    }

    public static class CustomAdvice
    {
        @Advice.OnMethodExit
        public static void intercept(@Advice.Return(readOnly = false) String value)
        {
            System.out.println("intercepted: " + value);
            value = "hi: " + value;
        }
    }

//    public static class MyWay
//    {
//    }
//
//    private static class InstrumentedStaticJDBCConnectionBuilder
//    {
//        static class WithPlaintextUsernamePassword extends StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword
//        {
//            WithPlaintextUsernamePassword(Function<HikariConfig, Void> hikariConfigHandler)
//            {
//                this.connectionManager = new InstrumentedJDBCConnectionManager(hikariConfigHandler);
//            }
//
//            @Override
//            public JDBCConnectionManager getConnectionManager()
//            {
//                return this.connectionManager;
//            }
//
//            @Override
//            protected Type[] actualTypeArguments()
//            {
//                Type genericSuperClass = this.getClass().getSuperclass().getGenericSuperclass();
//                ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
//                return parameterizedType.getActualTypeArguments();
//            }
//        }
//    }
//
//    private static class InstrumentedJDBCConnectionManager extends JDBCConnectionManager
//    {
//        private final Function<HikariConfig, Void> hikariConfigHandler;
//
//        InstrumentedJDBCConnectionManager(Function<HikariConfig, Void> hikariConfigHandler)
//        {
//            this.hikariConfigHandler = hikariConfigHandler;
//        }
//
////        @Override
////        protected void handleHikariConfig(HikariConfig config)
////        {
////            config.setRegisterMbeans(true);
////            this.hikariConfigHandler.apply(config);
////        }
//    }
}
