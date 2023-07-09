// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.engine.store.core.LegendStoreConnectionProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class TestLegendPostgresUserCommands
{
    private static PostgresTestContainerWrapper postgresTestContainerWrapper;

    @BeforeClass
    public static void setup()
    {
        postgresTestContainerWrapper = PostgresTestContainerWrapper.build();
        postgresTestContainerWrapper.start();
    }

    @AfterClass
    public static void shutdown()
    {
        if (postgresTestContainerWrapper == null)
        {
            return;
        }
        postgresTestContainerWrapper.stop();
    }

    @Test
    public void testCommand() throws Exception
    {
        LegendPostgresSupport postgresSupport =  new LegendPostgresSupportForTest();
        LegendStoreConnectionProvider<Connection> connectionProvider = postgresSupport.getConnectionProvider();
        connectionProvider.initialize();

        LegendPostgresCurrentUserCommand command = new LegendPostgresCurrentUserCommand();
        command.initialize(connectionProvider);

        String currentUser = command.run();
        assertEquals("test", currentUser);

        connectionProvider.shutdown();
    }

    public static class LegendPostgresSupportForTest extends LegendPostgresSupport
    {
        @Override
        public LegendStoreConnectionProvider<Connection> getConnectionProvider()
        {
            return new LegendStoreConnectionProvider<Connection>()
            {
                @Override
                public Connection getConnection() throws Exception
                {
                    Class.forName("org.postgresql.Driver");
                    Connection conn = DriverManager.getConnection(
                            postgresTestContainerWrapper.getJdbcUrl(),
                            postgresTestContainerWrapper.getUser(),
                            postgresTestContainerWrapper.getPassword()
                    );
                    return conn;
                }

                @Override
                public void configureConnection(Connection connection) throws Exception
                {
                    throw new UnsupportedOperationException("unsupported");
                }

                @Override
                public Optional<IntermediationRuleProvider> getIntermediationRuleProvider() throws Exception
                {
                    return Optional.empty();
                }
            };
        }
    }
}
