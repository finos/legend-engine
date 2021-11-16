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

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestConnectionStateManagerRace
{
    private FakeClock clock;
    private long startTime;
    private InstrumentedConnectionStateManager connectionStateManager;
    private CountDownLatch countDownLatch;

    @Before
    public void setup() throws Exception
    {
        resetSingleton();

        this.startTime = System.currentTimeMillis();
        this.clock = new FakeClock(startTime);
        this.countDownLatch = new CountDownLatch(1);
        this.connectionStateManager = new InstrumentedConnectionStateManager(clock, countDownLatch);
    }

    private void resetSingleton() throws Exception
    {
        Field field = ConnectionStateManager.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void testEvictionRace() throws InterruptedException
    {
        // cache has 2 state objects
        connectionStateManager.registerState("pool1", null, Optional.empty());
        ConnectionState pool1Version1 = connectionStateManager.getState("pool1");

        connectionStateManager.registerState("pool2", null, Optional.empty());
        ConnectionState pool2Version1 = connectionStateManager.getState("pool2");

        assertEquals(2, connectionStateManager.size());
        assertStateExists("pool1", "pool2");

        // advance clock by 4 minutes and invoke eviction (to simulate eviction task)
        clock.advance(Duration.ofMinutes(4));
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<?> evictionTask = executorService.submit(() -> connectionStateManager.evictStateOlderThan(Duration.ofMinutes(3)));

        // the instrumented state manager is given a countdown latch to artificially introduce a delay after it has found entries to evict
        Thread.sleep(Duration.ofSeconds(2).toMillis());

        // update the state for pool1
        connectionStateManager.registerState("pool1", null, Optional.empty());

        // now let the state manager continue with the eviction
        countDownLatch.countDown();
        Thread.sleep(Duration.ofSeconds(2).toMillis());
        assertTrue("Background eviction task has not completed", evictionTask.isDone());

        assertEquals(1, connectionStateManager.size());
        assertStateExists("pool1");

        assertNotSame("mismatch in cached state", pool1Version1, connectionStateManager.getState("pool1"));
    }

    private void assertStateExists(String ...poolNames)
    {
        for (String poolName : poolNames)
        {
            assertNotNull("State not found for pool="+ poolName, connectionStateManager.getState(poolName));
        }
    }
}