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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.IdentitySpecification;
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

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
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
    }

    @Test
    public void testBasicConnectionPooling() throws Exception
    {
        this.connectionFactory = new ConnectionFactory.Builder(this.environment, this.storeInstanceProvider)
                .withCredentialBuilders(
                        new UserPasswordCredentialBuilder()
                )
                .withConnectionBuilders(
                        new InstrumentedStaticJDBCConnectionBuilder.WithPlaintextUsernamePassword((HikariConfig config) -> {
                            config.setKeepaliveTime(10000);
                            config.setMaximumPoolSize(10);
                            return null;
                        })
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

        // 1. Get a connection, this should initialize the pool as well as create a new connection in the empty pool
        // this connection should be active
        Connection connection0 = this.connectionFactory.getConnection(identity, authenticator);

        HikariPoolMXBean poolProxy = getPoolProxy(identity, connectionSpecification, authenticationConfiguration);
        Assertions.assertEquals(1, poolProxy.getTotalConnections());
        Assertions.assertEquals(1, poolProxy.getActiveConnections());
        Assertions.assertEquals(0, poolProxy.getIdleConnections());

        // 2. Close the connection, verify that the pool keeps this connection around in idle state
        connection0.close();

        Assertions.assertEquals(1, poolProxy.getTotalConnections());
        Assertions.assertEquals(0, poolProxy.getActiveConnections());
        Assertions.assertEquals(1, poolProxy.getIdleConnections());

        // 3. Get a new connection, the pool should return the idle connection and create no new connection
        Connection connection1 = this.connectionFactory.getConnection(identity, authenticator);
//        Connection connection2 = this.connectionFactory.getConnection(identity, authenticator);

//        connection1.isValid()
        Assertions.assertEquals(1, poolProxy.getTotalConnections());
        Assertions.assertEquals(1, poolProxy.getActiveConnections());
        Assertions.assertEquals(0, poolProxy.getIdleConnections());

        // 4. Get another connection while the first one is still alive and used, a new connection
        // will be created in the pool
        this.connectionFactory.getConnection(identity, authenticator);

        Assertions.assertEquals(2, poolProxy.getTotalConnections());
        Assertions.assertEquals(2, poolProxy.getActiveConnections());
        Assertions.assertEquals(0, poolProxy.getIdleConnections());
    }

    private static HikariPoolMXBean getPoolProxy(Identity identity, ConnectionSpecification connectionSpecification, AuthenticationConfiguration authenticationConfiguration) throws MalformedObjectNameException
    {
        String poolName = JDBCConnectionManager.getPoolName(identity, connectionSpecification, authenticationConfiguration);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        return JMX.newMXBeanProxy(mBeanServer, new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")"), HikariPoolMXBean.class);
    }

    private static class InstrumentedStaticJDBCConnectionBuilder
    {
        static class WithPlaintextUsernamePassword extends StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword
        {
            private final InstrumentedJDBCConnectionManager connectionManager;

            WithPlaintextUsernamePassword(Function<HikariConfig, Void> hikariConfigHandler)
            {
                this.connectionManager = new InstrumentedJDBCConnectionManager(hikariConfigHandler);
            }

            @Override
            public JDBCConnectionManager getConnectionManager()
            {
                return this.connectionManager;
            }

            @Override
            protected Type[] actualTypeArguments()
            {
                Type genericSuperClass = this.getClass().getSuperclass().getGenericSuperclass();
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
                return parameterizedType.getActualTypeArguments();
            }
        }
    }

    private static class InstrumentedJDBCConnectionManager extends JDBCConnectionManager
    {
        private final Function<HikariConfig, Void> hikariConfigHandler;

        InstrumentedJDBCConnectionManager(Function<HikariConfig, Void> hikariConfigHandler)
        {
            this.hikariConfigHandler = hikariConfigHandler;
        }

        @Override
        protected void handleHikariConfig(HikariConfig config)
        {
            config.setRegisterMbeans(true);
            this.hikariConfigHandler.apply(config);
        }
    }
}
