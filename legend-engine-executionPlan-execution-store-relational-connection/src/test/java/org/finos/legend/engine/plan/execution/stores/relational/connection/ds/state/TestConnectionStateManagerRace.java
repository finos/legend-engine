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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestConnectionStateManagerRace extends TestConnectionManagement
{
    private CountDownLatch countDownLatch;

    @Before
    public void setup() throws Exception
    {
        super.setup();
        this.countDownLatch = new CountDownLatch(1);

        this.connectionStateManager = new InstrumentedConnectionStateManager(clock, countDownLatch, 2);
        ConnectionStateManager.setInstanceForTesting(connectionStateManager);
    }


    @Test
    public void testEvictionRace() throws InterruptedException
    {
        // cache has 2 state objects
        Identity user1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("pool1");
        Identity user2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("pool2");
        DataSourceSpecification ds1 = buildLocalDataSourceSpecification(Arrays.asList("DROP TABLE IF EXISTS T1;"));
        String pool1 = connectionStateManager.poolNameFor(user1,ds1.getConnectionKey());
        String pool2 = connectionStateManager.poolNameFor(user2,ds1.getConnectionKey());

        requestConnection(user1, ds1);
        Assert.assertEquals(1, connectionStateManager.get(pool1).getStatistics().getRequestedConnections());
        requestConnection(user1, ds1);
        requestConnection(user2, ds1);
        DataSourceStatistics pool1Version1 = DataSourceStatistics.clone(connectionStateManager.get(pool1).getStatistics());
        assertEquals(2, connectionStateManager.size());
        assertPoolStateExists(pool1, pool2);
        Assert.assertEquals(2, connectionStateManager.get(pool1).getStatistics().getRequestedConnections());
        Assert.assertEquals(1, connectionStateManager.get(pool2).getStatistics().getRequestedConnections());

        // advance clock by 4 minutes and invoke eviction (to simulate eviction task)
        clock.advance(Duration.ofMinutes(4));
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<?> evictionTask = executorService.submit(() -> connectionStateManager.evictStateOlderThan(Duration.ofMinutes(3)));

        // the instrumented state manager is given a countdown latch to artificially introduce a delay after it has found entries to evict
        Thread.sleep(Duration.ofSeconds(2).toMillis());

        // update the state for pool1
        requestConnection(user1, ds1);
        Assert.assertEquals(3, connectionStateManager.get(pool1).getStatistics().getRequestedConnections());
        // now let the state manager continue with the eviction
        countDownLatch.countDown();
        Thread.sleep(Duration.ofSeconds(2).toMillis());
        assertTrue("Background eviction task has not completed", evictionTask.isDone());
        Assert.assertNotSame("mismatch in cached state", pool1Version1.getLastConnectionRequestAge(), connectionStateManager.get(pool1).getStatistics().getLastConnectionRequestAge());
        assertEquals(1, connectionStateManager.size());
        assertPoolStateExists(pool1);


    }
}