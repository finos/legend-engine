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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

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
        assertPoolExists(false,"user1",ds1.getConnectionKey());
        assertPoolExists(false,"user2",ds2.getConnectionKey());
        assertPoolExists(false,"user3",ds3.getConnectionKey());

        requestConnection("user1",ds1);
        requestConnection("user2",ds2);
        requestConnection("user3",ds3);
        requestConnection("user1",ds1);

        Assert.assertEquals(3, connectionStateManager.size());
        assertPoolExists(true,"user1",ds1.getConnectionKey());
        assertPoolExists(true,"user2",ds2.getConnectionKey());
        assertPoolExists(true,"user3",ds3.getConnectionKey());

    }

    @Test
    public void testDataSourceRegistrationSequentialCalls()
    {
        Identity user1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("user1");
        Identity user2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("user2");

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
        assertPoolStateExists(pool1, pool2);;
    }

    @Test
    public void testDataSourceEviction()
    {
        Identity user1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("user1");
        Identity user2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("user2");
        Identity user3 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("user3");

        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1"));
        DataSourceSpecification ds2 = buildLocalDataSourceSpecification(Collections.emptyList());

        String pool1 = connectionStateManager.poolNameFor(user1, ds1.getConnectionKey());
        String pool2 = connectionStateManager.poolNameFor(user2, ds2.getConnectionKey());

        Assert.assertEquals(0, connectionStateManager.size());
        assertPoolExists(false, user1.getName(), ds1.getConnectionKey());
        assertPoolExists(false, user1.getName(), ds2.getConnectionKey());

        ConnectionStateManager.ConnectionStateHousekeepingTask houseKeeper = new ConnectionStateManager.ConnectionStateHousekeepingTask(Duration.ofMinutes(5).getSeconds());

        requestConnection(user1, ds1);
        requestConnection(user2, ds2);


        // advance clock by 4 minutes and run housekeeper
        clock.advance(Duration.ofMinutes(4));
        houseKeeper.run();

        Assert.assertEquals(2, connectionStateManager.size());
        assertPoolExists(true, "user1", ds1.getConnectionKey());
        assertPoolExists(true, "user2", ds2.getConnectionKey());
        assertPoolExists(false, "user3", ds2.getConnectionKey());
        assertPoolStateExists(pool1, pool2);
        Assert.assertEquals(2, connectionStateManager.size());
        clock.advance(Duration.ofMinutes(5));

        requestConnection(user3, ds1);//new user makes a connection
        String pool3 = connectionStateManager.poolNameFor(user3, ds1.getConnectionKey());
        Assert.assertNotNull(pool3);

        assertPoolExists(true, "user1", ds1.getConnectionKey());
        assertPoolExists(true, "user2", ds2.getConnectionKey());
        assertPoolExists(true, "user3", ds1.getConnectionKey());
        Assert.assertEquals(3, connectionStateManager.size());

        //default time is 10 mins , no new connection for user1 and 2 , should be removed
        clock.advance(Duration.ofMinutes(2));
        houseKeeper.run();

        assertPoolExists(false, "user1", ds1.getConnectionKey());
        assertPoolExists(false, "user2", ds2.getConnectionKey());
        assertPoolExists(true, "user3", ds1.getConnectionKey());
        Assert.assertEquals(1, connectionStateManager.size());
        Assert.assertEquals(1, connectionStateManager.size());
        assertPoolStateExists(pool3);
    }
}