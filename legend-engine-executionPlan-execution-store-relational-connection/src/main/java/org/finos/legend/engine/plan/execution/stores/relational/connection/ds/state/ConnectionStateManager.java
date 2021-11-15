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
    This class implements a least-frequently-used cache of state objects.
    The objects are keyed by name and maintained in a concurrent hash map.
    An eviction thread removes state objects which have not been used in the last N minutes.

    Thread safety:
    ----------------
    The state manager is (mostly ??) thread safe.

    Threads:
    ----------------
    The state manager is accessed by three types of threads :

    1/ Connection acquisition threads - These are threads that serve user connection requests and threads spawned by Hikari. These threads read/write state objects.
    a/ Write : For every connection request, irrespective of whether a physical database connection is created or not, the thread serving the connection request creates a new state object for the pool from which the connection is requested.
    The created state object captures a creation timestamp which is later used to evict the state object.
    b/ Read : If a new physical database connection has to be created, the thread serving the connection request reads the state object that was previously written.

    2/ State manager eviction thread - A scheduled task evicts state object older than a certain duration.
    a/ Read/Write : The eviction thread evicts state objects that have not been used in the last N minutes. It iterates over the map and removes objects based on their creation timestamp.

    3/ DevOps threads - These are threads that access the state manager for debugging/devops purposes
    a/ Read - The state manager exposes "get/getAll/dump" methods that iterate over the map.

    The DevOps threads can observe an inconsistent state of the map. This is because we do not lock the entire map when it is being read. While this can produce an inconsistent view, it does not affect correctness/safety.

    The connection acquisition threads and the eviction thread race. Concretely, it is possible for the eviction thread to evict a state object that is being used by a connection acquisition thread.
    Consider the following sequence :
        time t0 : state object S1 with key K1 becomes eligible for eviction
        time t1 : Eviction Thread : Thread is ready to evict S1. It has reference to S1 but has not yet removed S1 from the map
        time t2 : Connection Thread : Thread "touches" key K1 with a new state object S2
        time t3 : Eviction thread : Thread evicts S1 by removing the map entry with key K1
        time t4 : Connection Thread : Thread fetches state object for key K1 but does not find it (and encounters a null pointer exception)
    To protect against this scenario, the eviction thread uses a two step eviction process. In the first step it computes the map entries to be evicted.
    In the second step, for each entry to be evicted, it checks if the entry has not been updated. If the entry has been updated, it simply skips the entry.

    Connection acquisition threads for the same pool (i.e same logical identity, same database, same auth type) race. Consider the following sequence :
        time t0 : Connection thread1 : Creates state object S1 for key K1
        time t1 : Connection thread2 : Creates state object S2 for key K1
        time t2 : Connection thread1 : Creates connection using state object S2 created by thread2 instead of state object S1
    This is generally (??) safe. Since the racing threads are for the same pool and since the credential suppliers in the state objects are thread safe, it is safe for thread1 to use a state object created by thread2.
    However, the fact that thread1 uses thread2's identity might be confusing and in some cases cause connection failures. For e.g thread1 can create S1 with a valid OAuth token and thread2 can create S2 with an expired OAuth token
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

    ConnectionStateManager(Clock clock)
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
        for (Map.Entry<String, ConnectionState> entry : entriesToPurge)
        {
            String key = entry.getKey();
            ConnectionState stateForKey = entry.getValue();
            ConnectionState latestStateForKey = this.stateByPool.get(key);
            // Deliberate test based on object hash code. See thread safety documentation at the top of this class
            if (stateForKey.hashCode() != latestStateForKey.hashCode())
            {
                continue;
            }
            this.stateByPool.remove(key);
        }
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