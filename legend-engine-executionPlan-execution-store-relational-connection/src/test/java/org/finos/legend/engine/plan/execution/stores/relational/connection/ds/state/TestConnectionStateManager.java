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
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestConnectionStateManager
{
    private FakeClock clock;
    private long startTime;
    private ConnectionStateManager connectionStateManager;

    @Before
    public void setup() throws Exception
    {
        resetSingleton();

        this.startTime = System.currentTimeMillis();
        this.clock = new FakeClock(startTime);
        this.connectionStateManager = ConnectionStateManager.getInstanceForTesting(clock);
    }

    private void resetSingleton() throws Exception
    {
        Field field = ConnectionStateManager.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void testGetSet()
    {
        connectionStateManager.registerState("pool1", null, null);
        connectionStateManager.registerState("pool2", null, null);
        connectionStateManager.registerState("pool3", null, null);
        connectionStateManager.registerState("pool4", null, null);

        assertEquals(4, connectionStateManager.size());
        assertStateExists("pool1", "pool2", "pool3", "pool4");
    }

    @Test
    public void testEviction()
    {
        connectionStateManager.registerState("pool1", null, Optional.empty());
        connectionStateManager.registerState("pool2", null, Optional.empty());

        assertEquals(2, connectionStateManager.size());
        assertStateExists("pool1", "pool2");

        ConnectionStateManager.ConnectionStateEvictionTask houseKeeper = new ConnectionStateManager.ConnectionStateEvictionTask(Duration.ofMinutes(5).getSeconds());

        // advance clock by 4 minutes and run housekeeper
        clock.advance(Duration.ofMinutes(4));
        houseKeeper.run();

        assertEquals(2, connectionStateManager.size());
        assertStateExists("pool1", "pool2");

        connectionStateManager.registerState("pool3", null, Optional.empty());
        connectionStateManager.registerState("pool4", null, Optional.empty());
        connectionStateManager.registerState("pool5", null, Optional.empty());

        // advance clock by 2 more minutes and run housekeeper
        clock.advance(Duration.ofMinutes(2));
        houseKeeper.run();

        assertEquals(3, connectionStateManager.size());
        assertStateExists("pool3", "pool4", "pool5");
    }

    @Test
    public void testDefaultEvictionDuration()
    {
        System.clearProperty(ConnectionStateManager.EVICTION_DURATION_SYSTEM_PROPERTY);
        long evictionDurationInSeconds = ConnectionStateManager.resolveEvictionDuration();
        assertEquals(Duration.ofMinutes(5).getSeconds(), evictionDurationInSeconds);
    }

    @Test
    public void testNonDefaultEvictionDuration()
    {
        System.setProperty(ConnectionStateManager.EVICTION_DURATION_SYSTEM_PROPERTY, "4567");
        long evictionDurationInSeconds = ConnectionStateManager.resolveEvictionDuration();
        assertEquals(4567, evictionDurationInSeconds);
    }

    private void assertStateExists(String ...poolNames)
    {
        for (String poolName : poolNames)
        {
            assertNotNull("State not found for pool="+ poolName, connectionStateManager.getState(poolName));
        }
    }

    static class FakeClock extends Clock
    {
        private long currentTimeInMillis;

        public FakeClock(long currentTimeInMillis)
        {
            this.currentTimeInMillis = currentTimeInMillis;
        }

        public void advance(Duration duration)
        {
            this.currentTimeInMillis += duration.toMillis();
        }

        @Override
        public ZoneId getZone()
        {
            return null;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return null;
        }

        @Override
        public Instant instant()
        {
            return null;
        }

        @Override
        public long millis()
        {
            return this.currentTimeInMillis;
        }
    }
}