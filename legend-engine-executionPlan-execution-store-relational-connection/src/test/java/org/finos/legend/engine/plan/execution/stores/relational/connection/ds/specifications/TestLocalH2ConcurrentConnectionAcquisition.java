// Copyright 2021 Goldman Sachs
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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import com.zaxxer.hikari.pool.HikariProxyConnection;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.authentication.demoflows.H2LocalWithDefaultUserPasswordFlow;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.LocalH2DataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.h2.jdbc.JdbcConnection;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestLocalH2ConcurrentConnectionAcquisition
{
    @Test
    public void testConcurrentConnections() throws Exception
    {
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory threadFactory = (Runnable r) -> new Thread(r, "worker-" + counter.getAndIncrement());

        // start a bunch of threads where each thread acquires a LocalH2 connection
        List<Future<H2WorkerState>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);
        for (int i = 0; i < 10; i++)
        {
            futures.add(executorService.submit(new H2Worker()));
        }

        List<H2WorkerState> workerStates = new ArrayList<>();
        for (Future<H2WorkerState> future : futures)
        {
            workerStates.add(future.get());
        }

        /*
           Assert that each thread got its own unique connection.
           This is asserted as follows
           1/ Verify that each worker's underlying H2 connection has a unique name
           2/ Create a table with the same name in each connection. If a connection is shared, the worker that gets to use the connection after it has already been used by another worker, will get a duplicate table exception
         */

        // 10 workers produce 10 results
        assertEquals(10, workerStates.size());

        // each worker's underlying connection name is unique.
        Set<String> workerNames = workerStates.stream().map(h2WorkerState -> h2WorkerState.h2Connection.getTraceObjectName()).collect(Collectors.toSet());
        assertEquals(10, workerNames.size());

        Map<String, H2WorkerState> statesByThread = workerStates.stream().collect(Collectors.toMap(ws -> ws.name, Function.identity()));
        for (int i = 0 ; i < 10; i++)
        {
            String threadName = String.format("worker-%d", i);
            String expected = String.format("worker-%d_data0,worker-%d_data1", i, i);
            String actual = statesByThread.get(threadName).data.stream().collect(Collectors.joining(","));
            assertEquals("Mismatch in data for worker "+ threadName, expected, actual);
        }
    }

    static class H2WorkerState
    {
        private final String name;
        private final JdbcConnection h2Connection;
        private List<String> data;

        public H2WorkerState(String name, JdbcConnection h2Connection, List<String> data)
        {
            this.name = name;
            this.h2Connection = h2Connection;
            this.data = data;
        }
    }

    static class H2Worker implements Callable<H2WorkerState>
    {
        public CountDownLatch latch = new CountDownLatch(1);

        @Override
        public H2WorkerState call() throws Exception
        {
            String threadName = Thread.currentThread().getName();
            Connection connection = initConnection();
            JdbcConnection h2Connection = unwrapConnection(connection);
            List<String> data = doWork(connection, threadName);
            return new H2WorkerState(Thread.currentThread().getName(), h2Connection, data);
        }

        private Connection initConnection()
        {
            LocalH2DataSourceSpecification specification = new LocalH2DataSourceSpecification(
                    new LocalH2DataSourceSpecificationKey(Lists.mutable.empty()),
                    new H2Manager(),
                    new TestDatabaseAuthenticationStrategy(),
                    new RelationalExecutorInfo());
            Identity identity = IdentityFactoryProvider.getInstance().makeIdentity((Subject) null);
            return specification.getConnectionUsingIdentity(identity, plainTextCredentialSupplier());
        }

        private List<String> doWork(Connection connection, String threadName) throws Exception
        {
            Statement statement = connection.createStatement();
            statement.execute("create table TEST(data VARCHAR(100))");
            for (int i = 0; i < 2; i++)
            {
                statement.execute(String.format("insert into TEST(data) values('%s')", threadName + "_data" + i));
            }

            List<String> data = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery("select * from TEST");
            while (resultSet.next())
            {
                data.add(resultSet.getString(1));
            }
            resultSet.close();
            return data;
        }

        private JdbcConnection unwrapConnection(Connection connection)
        {
            try
            {
                LocalH2DataSourceSpecification.WrappedH2Connection wrapped = (LocalH2DataSourceSpecification.WrappedH2Connection) connection;
                HikariProxyConnection hikariProxyConnection = (HikariProxyConnection) getFieldUsingReflection(LocalH2DataSourceSpecification.WrappedH2Connection.class, wrapped, "conn");
                JdbcConnection h2Connection = (JdbcConnection) getFieldUsingReflection(HikariProxyConnection.class, hikariProxyConnection, "delegate");
                return h2Connection;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static Object getFieldUsingReflection(Class clazz, Object object, String fieldName) throws Exception
    {
        Field field = null;
        try
        {
            field = clazz.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e)
        {
            field = clazz.getSuperclass().getDeclaredField(fieldName);
        }
        return getValueFromObject(object, field);
    }

    private static Object getValueFromObject(Object object, Field field) throws IllegalAccessException
    {
        field.setAccessible(true);
        Object value = field.get(object);
        return value;
    }

    private static Optional<CredentialSupplier> plainTextCredentialSupplier()
    {
        CredentialSupplier credentialSupplier = new CredentialSupplier(new H2LocalWithDefaultUserPasswordFlow(), null, null);
        return Optional.of(credentialSupplier);
    }
}