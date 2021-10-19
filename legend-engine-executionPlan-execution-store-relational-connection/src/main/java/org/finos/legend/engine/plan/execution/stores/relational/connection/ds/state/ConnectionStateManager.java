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

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    This class implements a simple singleton cache which maintains a pool of state objects.
    The objects are keyed by name and maintained in a concurrent hash map.
    A scheduled task evicts pool entries older than a certain duration.

    The eviction task runs in a thread separate from the readers and writers of this cache.
    We explicitly do not synchronize the eviction thread with the reader and writer threads. Any inconsistent state observed by the eviction thread does not affect correctness/safety.
 */
public class ConnectionStateManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStateManager.class);

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    private static final Duration EVICTION_DURATION = Duration.ofMinutes(10);
    private Clock clock;

    private ConnectionStateManager(Clock clock)
    {
        // singleton
        this.clock = clock;
        ConnectionStateEvictionTask evictionTask = new ConnectionStateEvictionTask(EVICTION_DURATION);
        EXECUTOR_SERVICE.scheduleAtFixedRate(evictionTask, 0, EVICTION_DURATION.toMinutes(), TimeUnit.MINUTES);
        LOGGER.info("Connection state eviction thread frequency. Time period={}, Time unit={}", EVICTION_DURATION.toMinutes(), TimeUnit.MINUTES);
    }

    public static String POOL_NAME_KEY = "POOL_NAME_KEY";

    private static ConnectionStateManager INSTANCE;

    public static synchronized final ConnectionStateManager getInstance()
    {
        return getInstanceImpl(Clock.systemUTC());
    }

    public static synchronized final ConnectionStateManager getInstanceForTesting(Clock clock)
    {
        return getInstanceImpl(clock);
    }

    private static ConnectionStateManager getInstanceImpl(Clock clock)
    {
        if (INSTANCE == null)
        {
            INSTANCE = new ConnectionStateManager(clock);
        }
        return INSTANCE;
    }

    private ConcurrentHashMap<String, ConnectionState> stateByPool = new ConcurrentHashMap<>();

    public void registerState(String poolName, Identity identity, Optional<CredentialSupplier> databaseCredentialSupplier)
    {
        this.stateByPool.put(poolName, new ConnectionState(this.clock.millis(), identity, databaseCredentialSupplier));
    }

    public ConnectionState getStateUsing(Properties properties)
    {
        String poolName = properties.getProperty(ConnectionStateManager.POOL_NAME_KEY);
        return this.getState(poolName);
    }

    public ConnectionState getState(String poolName)
    {
        return this.stateByPool.get(poolName);
    }

    public Optional<CredentialSupplier>  getCredentialSupplier(String poolName)
    {
        if (!this.stateByPool.containsKey(poolName))
        {
            return null;
        }
        return this.stateByPool.get(poolName).getCredentialSupplier();
    }

    public Identity getIdentity(String poolName)
    {
        if (!this.stateByPool.containsKey(poolName))
        {
            return null;
        }
        return this.stateByPool.get(poolName).getIdentity();
    }

    public Set<Map.Entry<String, ConnectionState>> findStateOlderThan(Duration duration)
    {
        return this.stateByPool.entrySet().stream()
                .filter(entry -> entry.getValue().ageInMillis(this.clock) > duration.toMillis())
                .collect(Collectors.toSet());
    }

    public void evictStateOlderThan(Duration duration)
    {
        Set<Map.Entry<String, ConnectionState>> entriesToPurge = this.findStateOlderThan(duration);
        entriesToPurge.stream()
                .forEach(state -> this.stateByPool.remove(state.getKey()));
    }

    public int size()
    {
        return this.stateByPool.size();
    }

    public void dump()
    {
        System.out.println(this.stateByPool.keySet());
    }

    public static class ConnectionStateEvictionTask implements Runnable
    {
        private final Duration duration;

        public ConnectionStateEvictionTask(Duration duration)
        {
            this.duration = duration;
        }

        @Override
        public void run()
        {
            ConnectionStateManager instance = ConnectionStateManager.getInstance();
            int sizeBeforePurge = instance.size();
            instance.evictStateOlderThan(this.duration);
            int sizeAfterPurge = instance.size();
            LOGGER.info("Connection state purge stats : Size before purge={}, Size after purge={}", sizeBeforePurge, sizeAfterPurge);
        }
    }
}