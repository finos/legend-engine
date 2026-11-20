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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;

import javax.security.auth.Subject;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class TestLocalH2TestDataPool
{
    private final Identity identity = Identity.makeIdentity((Subject) null);

    @Test
    public void testPoolHousekeeperDoesNotRetainCallerContextClassLoader() throws Exception
    {
        LocalH2DataSourceSpecification specification = newTestDataSpecification();

        Thread currentThread = Thread.currentThread();
        ClassLoader original = currentThread.getContextClassLoader();
        try (URLClassLoader executionClassLoader = new URLClassLoader(new URL[0], original))
        {
            Connection connection;
            currentThread.setContextClassLoader(executionClassLoader);
            try
            {
                connection = getConnection(specification);
            }
            finally
            {
                currentThread.setContextClassLoader(original);
            }
            connection.close();

            String housekeeperName = poolNameFor(specification) + ":housekeeper";
            List<Thread> housekeepers = Thread.getAllStackTraces().keySet().stream()
                    .filter(t -> t.getName().equals(housekeeperName))
                    .collect(Collectors.toList());
            assertEquals(1, housekeepers.size());
            assertNotSame(executionClassLoader, housekeepers.get(0).getContextClassLoader());
        }
    }

    @Test
    public void testTestDataPoolReleasesIdleConnectionsAtHikariMinimum() throws Exception
    {
        LocalH2DataSourceSpecification specification = newTestDataSpecification();
        getConnection(specification).close();

        HikariDataSource pool = (HikariDataSource) ConnectionStateManager.getInstance().get(poolNameFor(specification)).getDataSource();
        assertEquals(SECONDS.toMillis(10), pool.getIdleTimeout());
    }

    private static LocalH2DataSourceSpecification newTestDataSpecification()
    {
        return new LocalH2DataSourceSpecification(
                Lists.mutable.with("create table T_" + UUID.randomUUID().toString().replace('-', '_') + "(id INT)"),
                new H2Manager(),
                new TestDatabaseAuthenticationStrategy());
    }

    private Connection getConnection(LocalH2DataSourceSpecification specification)
    {
        return specification.getConnectionUsingIdentity(this.identity, TestLocalH2ConcurrentConnectionAcquisition.plainTextCredentialSupplier());
    }

    private String poolNameFor(LocalH2DataSourceSpecification specification)
    {
        return ConnectionStateManager.getInstance().poolNameFor(this.identity, specification.getConnectionKey());
    }
}
