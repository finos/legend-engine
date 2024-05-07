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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestConnectionStateManager extends TestConnectionManagement
{

    @Before
    public void setup() throws Exception
    {
        super.setup();
        this.connectionStateManager = ConnectionStateManager.getInstanceForTesting(clock);
    }

    @Test
    public void testDefaultEvictionDuration()
    {
        System.clearProperty(ConnectionStateManager.EVICTION_DURATION_SYSTEM_PROPERTY);
        long evictionDurationInSeconds = ConnectionStateManager.resolveEvictionDuration();
        assertEquals(Duration.ofMinutes(10).getSeconds(), evictionDurationInSeconds);
    }

    @Test
    public void testNonDefaultEvictionDuration()
    {
        System.setProperty(ConnectionStateManager.EVICTION_DURATION_SYSTEM_PROPERTY, "4567");
        long evictionDurationInSeconds = ConnectionStateManager.resolveEvictionDuration();
        assertEquals(4567, evictionDurationInSeconds);
    }


    @Test
    public void testDataSourceConnectionsRequest()
    {
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        DataSourceSpecification ds2 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T2"));
        DataSourceSpecification ds3 = buildLocalDataSourceSpecification(Collections.emptyList());
        assertPoolExists(false, "user1", ds1.getConnectionKey());
        assertPoolExists(false, "user2", ds2.getConnectionKey());
        assertPoolExists(false, "user3", ds3.getConnectionKey());

        requestConnection("user1", ds1);
        requestConnection("user2", ds2);
        requestConnection("user3", ds3);
        requestConnection("user1", ds1);

        Assert.assertEquals(3, connectionStateManager.size());
        assertPoolExists(true, "user1", ds1.getConnectionKey());
        assertPoolExists(true, "user2", ds2.getConnectionKey());
        assertPoolExists(true, "user3", ds3.getConnectionKey());

    }

    @Test
    public void testMetricCreationForNewConnectionPoolUsingHouseKeeperRun()
    {

        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        Identity user1 = new Identity("user1");

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());

        //request new connection for user1 and ds1
        requestConnection(user1, ds1);

        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run();
        //housekeeper evicts no old connections, default registry appends new instance of active, total and idle connection info for new pool entry
        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool1).getTotalConnections(), 0d);
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool1).getActiveConnections(), 0d);
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(0.00, collectorRegistry.getSampleValue("idle_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);

    }

    @Test
    public void testMetricUpdateForActiveConnectionPoolUsingHouseKeeperRun()
    {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        Identity user1 = new Identity("user1");

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());
        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        //request new connection for user1 and ds1
        requestConnection(user1, ds1);
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());

        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run();

        requestConnection(user1, ds1);
        requestConnection(user1, ds1);

        //active, total and idle connection values are updated but Metrics will be updated in 10 minutes when housekeeper gets triggered.
        Assert.assertNotEquals(connectionStateManager.getDataSourceByPoolName(pool1).getActiveConnections(), collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}));
        Assert.assertNotEquals(connectionStateManager.getDataSourceByPoolName(pool1).getTotalConnections(), collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool1}));


        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run(); //housekeeper evicts no old connections, updates default registry with active, total and idle connection info for existing pool entry

        //values in both connection state manager and metrics handler will reflect the same now
        Assert.assertEquals(connectionStateManager.getDataSourceByPoolName(pool1).getActiveConnections(), collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}),  0d);
        Assert.assertEquals(connectionStateManager.getDataSourceByPoolName(pool1).getTotalConnections(), collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool1}),  0d);
        Assert.assertEquals(connectionStateManager.getDataSourceByPoolName(pool1).getIdleConnections(), collectorRegistry.getSampleValue("idle_connections", new String[] {"poolName"}, new String[]{pool1}),  0d);
        Assert.assertEquals(3.00, connectionStateManager.getDataSourceByPoolName(pool1).getTotalConnections(), 0d);
        Assert.assertEquals(3.00, connectionStateManager.getDataSourceByPoolName(pool1).getActiveConnections(), 0d);
        Assert.assertEquals(0.00, connectionStateManager.getDataSourceByPoolName(pool1).getIdleConnections(), 0d);
        Assert.assertEquals(3.00, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(3.00, collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(0.00, collectorRegistry.getSampleValue("idle_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);

    }

    @Test
    public void testMetricDeletionForEvictedConnectionPoolsFromHouseKeeperRun()
    {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        DataSourceSpecification ds2 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T2"));
        Identity user2 = new Identity("user2");
        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());

        //create a new instance of pool entry with ds2 and user2
        requestConnection(user2, ds2);
        String pool2 = connectionStateManager.poolNameFor(user2, ds2.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds2.getConnectionKey());

        //only have 1 active connection for this pool
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool2).getTotalConnections(), 0d);
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool2).getActiveConnections(), 0d);

        //trigger housekeeper run after 10 minutes to create a new entry for this pool in metrics
        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run(); //housekeeper run to update metrics
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool2}), 0d);
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool2}), 0d);
        Assert.assertEquals(0.00, collectorRegistry.getSampleValue("idle_connections", new String[] {"poolName"}, new String[]{pool2}), 0d);

        //close this to trigger cleanup in 10 mins
        DataSourceWithStatistics userPool2 = connectionStateManager.getDataSourceByPoolName(pool2);
        userPool2.close();

        //before the next housekeeper run, Connection State Manager and Metrics display different results
        Assert.assertEquals(0.00, connectionStateManager.getDataSourceByPoolName(pool2).getActiveConnections(), 0d);
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool2}), 0d);

        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run();

        //housekeeper evicts pool2 from connection state manager and removes metrics for pool2 from default registry
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());
        Assert.assertEquals(connectionStateManager.getDataSourceByPoolName(pool2), null);
        Assert.assertEquals(null, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool2}));

    }

    @Test
    public void testMetricsUpdateWithIdleConnectionInActivePool() throws SQLException
    {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        Identity user1 = new Identity("user1");

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());
        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());

        //create new pool entry with new request connections
        requestConnection(user1, ds1);
        requestConnection(user1, ds1).close();

        //connection state manager and metrics display different values since housekeeper hasnt been triggered yet
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool1).getActiveConnections(), 0d);
        Assert.assertEquals(2.00, connectionStateManager.getDataSourceByPoolName(pool1).getTotalConnections(), 0d);
        Assert.assertEquals(1.00, connectionStateManager.getDataSourceByPoolName(pool1).getIdleConnections(), 0d);
        Assert.assertEquals(null, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}));

        clock.advance(Duration.ofMinutes(11));
        houseKeeper.run();

        //housekeeper doesnt evict any pools, updates pool1 entry metrics by incrementing the total and idle connection values
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("active_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(2.00, collectorRegistry.getSampleValue("total_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
        Assert.assertEquals(1.00, collectorRegistry.getSampleValue("idle_connections", new String[] {"poolName"}, new String[]{pool1}), 0d);
    }

    @Test
    public void testDataSourceRegistrationSequentialCalls()
    {
        Identity user1 = new Identity("user1");
        Identity user2 = new Identity("user2");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Collections.emptyList());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        Assert.assertEquals(0, connectionStateManager.size());


        requestConnection(user1, ds1);
        Assert.assertEquals(1, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds1.getConnectionKey());
        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        assertPoolStateExists(pool1);
        Assert.assertEquals(1, connectionStateManager.size());

        DataSourceSpecification ds11 = buildLocalDataSourceSpecification(Collections.emptyList());
        requestConnection(user2, ds11);
        Assert.assertEquals(ds11.getConnectionKey(), ds1.getConnectionKey());
        Assert.assertEquals(2, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds1.getConnectionKey());
        String pool2 = connectionStateManager.poolNameFor(user2, ds1.getConnectionKey());
        assertPoolStateExists(pool1, pool2);
    }

    @Test
    public void testDataSourceEviction() throws SQLException
    {
        Identity user1 = new Identity("user1");
        Identity user2 = new Identity("user2");
        Identity user3 = new Identity("user3");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        DataSourceSpecification ds2 = buildLocalDataSourceSpecification(Collections.emptyList());

        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        String pool2 = connectionStateManager.poolNameFor(user2, ds2.getConnectionKey());

        Assert.assertEquals(0, connectionStateManager.size());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());

        Connection connection1 = requestConnection(user1, ds1);
        Connection connection2 = requestConnection(user2, ds2);

        connection1.close();
        connection2.close();

        // advance clock by 4 minutes and run housekeeper
        clock.advance(Duration.ofMinutes(4));
        houseKeeper.run();

        Assert.assertEquals(2, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(false, user3.getName(), ds1.getConnectionKey());
        assertPoolStateExists(pool1, pool2);
        Assert.assertEquals(2, connectionStateManager.size());
        clock.advance(Duration.ofMinutes(5));

        requestConnection(user3, ds1);//new user makes a connection
        String pool3 = connectionStateManager.poolNameFor(user3, ds1.getConnectionKey());
        Assert.assertNotNull(pool3);

        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(true, user3.getName(), ds1.getConnectionKey());
        Assert.assertEquals(3, connectionStateManager.size());

        //default time is 10 mins , no new connection for user1 and 2 , should be removed
        clock.advance(Duration.ofMinutes(2));
        houseKeeper.run();

        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(true, user3.getName(), ds1.getConnectionKey());
        Assert.assertEquals(1, connectionStateManager.size());
        assertPoolStateExists(pool3);
    }

    @Test
    public void testDataSourceEvictionWithUnclosedConnection() throws SQLException
    {
        Identity user1 = new Identity("user1");
        Identity user2 = new Identity("user2");
        Identity user3 = new Identity("user3");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        DataSourceSpecification ds2 = buildLocalDataSourceSpecification(Collections.emptyList());

        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        String pool2 = connectionStateManager.poolNameFor(user2, ds2.getConnectionKey());

        Assert.assertEquals(0, connectionStateManager.size());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());

        Connection connection1 = requestConnection(user1, ds1);
        Connection connection2 = requestConnection(user2, ds2);

        connection2.close();

        // advance clock by 4 minutes and run housekeeper
        clock.advance(Duration.ofMinutes(4));
        houseKeeper.run();

        Assert.assertEquals(2, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(false, user3.getName(), ds1.getConnectionKey());
        assertPoolStateExists(pool1, pool2);
        Assert.assertEquals(2, connectionStateManager.size());
        clock.advance(Duration.ofMinutes(5));

        requestConnection(user3, ds1);//new user makes a connection
        String pool3 = connectionStateManager.poolNameFor(user3, ds1.getConnectionKey());
        Assert.assertNotNull(pool3);

        Assert.assertEquals(3, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(true, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(true, user3.getName(), ds1.getConnectionKey());


        //default time is 10 mins , no new connection for user2 , should be removed
        clock.advance(Duration.ofMinutes(2));
        houseKeeper.run();

        Assert.assertEquals(2, connectionStateManager.size());
        assertPoolExists(true, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(false, user3.getName(), ds1.getConnectionKey());
        assertPoolStateExists(pool1);
        assertPoolStateExists(pool3);


        connection1.close();
        houseKeeper.run();

        Assert.assertEquals(1, connectionStateManager.size());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds2.getConnectionKey());
        assertPoolExists(true, user3.getName(), ds1.getConnectionKey());
        assertPoolStateExists(pool3);
    }

    @Test
    public void canGetAggregatedStats()
    {
        Identity user1 = new Identity("user1");
        Identity user2 = new Identity("user2");
        Identity user3 = new Identity("user3");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Collections.emptyList());

        requestConnection(user1, ds1);
        requestConnection(user2, ds1);
        requestConnection(user3, ds1);


        List<ConnectionStateManagerPOJO.RelationalStoreInfo> stores = new ArrayList<>(connectionStateManager.getConnectionStateManagerPOJO().getStores());
        Assert.assertFalse(stores.isEmpty());
        Assert.assertEquals(3, stores.get(0).aggregatedPoolStats.totalConnections);
        Assert.assertEquals(0, stores.get(0).aggregatedPoolStats.idleConnections);
        Assert.assertEquals(3, stores.get(0).aggregatedPoolStats.activeConnections);
        Assert.assertEquals(0, stores.get(0).aggregatedPoolStats.threadsAwaitingConnection);
    }


    @Test
    public void testShutdown() throws IOException
    {
        Identity user1 = new Identity("user1");
        Identity user2 = new Identity("user2");
        Identity user3 = new Identity("user3");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Collections.emptyList());

        requestConnection(user1, ds1);
        requestConnection(user2, ds1);
        requestConnection(user3, ds1);

        connectionStateManager.close();
        Assert.assertEquals(0, connectionStateManager.size());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user2.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user3.getName(), ds1.getConnectionKey());

    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testPoolIdentityIsValid()
    {
        Credential mockCredential = mock(Credential.class);
        Identity identityOne = new Identity("mock", mockCredential);
        when(mockCredential.isValid()).thenReturn(true);
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Collections.emptyList());
        String poolName = connectionStateManager.poolNameFor(identityOne, ds1.getConnectionKey());
        Assert.assertNotNull(poolName);

        requestConnection(identityOne, ds1);
        Assert.assertEquals(1, connectionStateManager.size());
        DataSourceWithStatistics dataSourceWithStatistics = connectionStateManager.get(poolName);
        requestConnection(identityOne, ds1);
        Assert.assertEquals(1, connectionStateManager.size());
        DataSourceWithStatistics dataSourceWithStatistics1 = connectionStateManager.get(poolName);
        Assert.assertEquals(dataSourceWithStatistics.getStatistics().getFirstConnectionRequest(), dataSourceWithStatistics1.getStatistics().getFirstConnectionRequest());

        //mock expiring of credentials
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("Invalid Identity found, cannot build connection pool for mock");
        when(mockCredential.isValid()).thenReturn(false);
        requestConnection(identityOne, ds1);
    }
}