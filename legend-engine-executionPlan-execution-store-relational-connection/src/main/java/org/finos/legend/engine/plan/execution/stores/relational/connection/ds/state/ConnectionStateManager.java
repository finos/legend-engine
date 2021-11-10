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
import java.util.*;
import java.util.concurrent.*;
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

    private static ScheduledExecutorService EXECUTOR_SERVICE;

    public static final long DEFAULT_EVICTION_DURATION_IN_SECONDS = Duration.ofMinutes(5).getSeconds();
    public static String EVICTION_DURATION_SYSTEM_PROPERTY = "org.finos.legend.engine.execution.connectionStateEvictionDurationInSeconds";

    public static String POOL_NAME_KEY = "POOL_NAME_KEY";

    private static ConnectionStateManager INSTANCE;

    static
    {
        ThreadFactory threadFactory = r -> new Thread(r, "ConnectionStateManager Housekeeper");
        long evictionDurationInSeconds = resolveEvictionDuration();
        ConnectionStateEvictionTask connectionStateEvictionTask = new ConnectionStateEvictionTask(evictionDurationInSeconds);
        EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1, threadFactory);
        EXECUTOR_SERVICE.scheduleWithFixedDelay(connectionStateEvictionTask, 0, evictionDurationInSeconds, TimeUnit.SECONDS);
        LOGGER.info("Connection state eviction thread frequency. Time period={}, Time unit={}", evictionDurationInSeconds, TimeUnit.SECONDS);
    }

    public static long resolveEvictionDuration()
    {
        Long evictionDurationInSeconds = Long.getLong(EVICTION_DURATION_SYSTEM_PROPERTY);
        if (evictionDurationInSeconds == null)
        {
            LOGGER.info("Using default eviction duration of {}", DEFAULT_EVICTION_DURATION_IN_SECONDS);
            return DEFAULT_EVICTION_DURATION_IN_SECONDS;
        }
        else
        {
            LOGGER.info("Using non default eviction duration of {}", evictionDurationInSeconds);
            return evictionDurationInSeconds;
        }
    }

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

    private Clock clock;

    private ConnectionStateManager(Clock clock)
    {
        // singleton
        this.clock = clock;
    }

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
        LOGGER.debug("Connection state dump");
        for (String key : this.stateByPool.keySet())
        {
            ConnectionState state = this.stateByPool.get(key);
            boolean credentialSupplierExists = state.getCredentialSupplier().isPresent();
            LOGGER.debug("Connection state : AgeInMillis={}, CredentialSupplierExists={}, Key={}", state.ageInMillis(clock), credentialSupplierExists, key);
        }
    }

    public List<ConnectionStatePOJO> getAll()
    {
        List<ConnectionStatePOJO> statePOJOS = this.stateByPool.entrySet().stream()
                .map(entry -> new ConnectionStatePOJO(entry.getKey(), entry.getValue().getCredentialSupplier().isPresent(), entry.getValue().ageInMillis(clock)))
                .collect(Collectors.toList());
        return statePOJOS;
    }

    public synchronized void purge(long durationInSeconds)
    {
        int sizeBeforePurge = this.size();
        LOGGER.info("Connection state purge : Starting purge with cache size={}", sizeBeforePurge);
        this.evictStateOlderThan(Duration.ofSeconds(durationInSeconds));
        int sizeAfterPurge = this.size();
        LOGGER.info("Connection state purge : Evicted={}", sizeBeforePurge-sizeAfterPurge);
        this.dump();
        LOGGER.info("Connection state purge : Completed purge with cache size={}", sizeAfterPurge);
    }

    public static class ConnectionStateEvictionTask implements Runnable
    {
        private final long durationInSeconds;

        public ConnectionStateEvictionTask(long durationInSeconds)
        {
            this.durationInSeconds = durationInSeconds;
        }

        @Override
        public void run()
        {
            ConnectionStateManager instance = ConnectionStateManager.getInstance();
            instance.purge(durationInSeconds);
        }
    }

    public static class ConnectionStatePOJO
    {
        String poolName;
        boolean credentialSupplierPresent;
        long ageInMillis;

        public ConnectionStatePOJO() {
        }

        public ConnectionStatePOJO(String poolName, boolean credentialSupplierPresent, long ageInMillis) {
            this.poolName = poolName;
            this.credentialSupplierPresent = credentialSupplierPresent;
            this.ageInMillis = ageInMillis;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public boolean isCredentialSupplierPresent() {
            return credentialSupplierPresent;
        }

        public void setCredentialSupplierPresent(boolean credentialSupplierPresent) {
            this.credentialSupplierPresent = credentialSupplierPresent;
        }

        public long getAgeInMillis() {
            return ageInMillis;
        }

        public void setAgeInMillis(long ageInMillis) {
            this.ageInMillis = ageInMillis;
        }
    }
}