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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManagerPOJO.buildConnectionPool;

/*
    This class implements a least-recently-used cache of pool state objects.
    The objects are keyed by pool name and maintained in a concurrent hash map.
    An eviction thread removes state objects which have not been used in the last N minutes.

    Thread safety:
    ----------------
    The connection state manager is **mostly** thread safe.

    Threads:
    ----------------
    The connection state manager is accessed by three types of threads :

    1/ Connection serving thread - This thread serves user connection requests.
    For every connection request, irrespective of whether a physical database connection is created or not, this thread updates lastConnectionRequest for the pool statistics from which the connection is requested.
    This timestamp is later used to evict the pool state object.

    2/ Connection creation thread - This thread actually creates the database connection. This can either be the connection serving thread or a Hikari pool thread.
    When the connection serving thread initializes the Hikari datasource, Hikari *synchronously* creates a connection( due to fail fast setting in Hikari).
    After a Hikari datasource has been initialized, Hikari can refresh the pool by creating connections in its own threads.
    In both cases, the thread creating the connection reads the pool object that was previously written.

    3/ Connection State manager HouseKeeper thread - A scheduled task evicts pool state objects.
    The thread evicts pool state objects that have not been used in the last N minutes. It iterates over the map and removes objects based on lastConnectionRequest timestamp.

    4/ "DevOps" thread - These are other threads that read the state map for debugging/logging purposes.
    The state manager exposes "get/getAll/dump" methods that iterate over the map.

    Thread Interactions :
    ----------------
    1/ The DevOps threads can observe an inconsistent state of the map. This is because we do not lock the entire map when it is being read. While this can produce an inconsistent view, it does not affect correctness/safety.

    2/ The connection serving threads and connection creation threads are properly synchronized.
    The state objects are keyed by the name of the pool. Writes and reads to this pool's state are synchronized using a lock. We do use an explicit lock,
    this pool specific lock enforces a "happens before" relationship between the write in the serving thread and read in the creation thread.

    3/ The eviction thread races with connection serving/creation threads.
    Consider the following sequence :
        time t0 : Eviction Thread : Pool state object S1 with key K1 becomes eligible for eviction
        time t1 : Eviction Thread : Thread is ready to evict S1. It has a reference to S1 but has not yet removed S1 from the map
        time t2 : Connection Serving Thread : Thread "touches" key K1 with a new pool state object S2
        time t3 : Eviction thread : Thread evicts S1 by removing the map entry with key K1
        time t4 : Connection Thread : Thread fetches state object for key K1 but does not find it (and encounters a null pointer exception)
    To protect against this scenario, the eviction thread uses a two step eviction process.

    - First, it computes the map entries to be evicted. This read is done *without* acquiring a full lock on the map.
    - Second, for each entry to be evicted, it checks if the entry has not been updated (since the first read). If the entry has been updated, it simply skips the entry.
    - The second read of each state object is synchronized using the pool specific lock. This lock enforces a "happens before" relationship between this read and any other thread that might have previously updated this state object.

    4/ Connection acquisition threads for the same pool (i.e same logical identity, same database, same auth type) race. Consider the following sequence :
        time t0 : Connection thread1 : Creates state object S1 for key K1
        time t1 : Connection thread2 : Creates state object S2 for key K1
        time t2 : Connection thread1 : Creates connection using state object S2 created by thread2 instead of state object S1
    This is generally (??) safe. Since the racing threads are for the same pool and since the credential suppliers in the state objects are thread safe, it is safe for thread1 to use a state object created by thread2.
    However, the fact that thread1 uses thread2's identity might be confusing and in some cases cause connection failures. For e.g thread1 can create S1 with a valid OAuth token and thread2 can create S2 with an expired OAuth token
 */
public class ConnectionStateManager implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStateManager.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static ScheduledExecutorService EXECUTOR_SERVICE;

    public static final long DEFAULT_EVICTION_DURATION_IN_SECONDS = Duration.ofMinutes(10).getSeconds();
    public static String EVICTION_DURATION_SYSTEM_PROPERTY = "org.finos.legend.engine.execution.connectionStateEvictionDurationInSeconds";

    public static String POOL_NAME_KEY = "POOL_NAME_KEY";
    private static final String SEPARATOR = "_";
    private static final String DBPOOL = "DBPool_";
    private static ConnectionStateManager INSTANCE;

    static {
        ThreadFactory threadFactory = r -> new Thread(r, "ConnectionStateManager.Housekeeper");
        long evictionDurationInSeconds = resolveEvictionDuration();
        ConnectionStateHousekeepingTask connectionStateHousekeepingTask = new ConnectionStateHousekeepingTask(evictionDurationInSeconds);
        EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1, threadFactory);
        EXECUTOR_SERVICE.scheduleWithFixedDelay(connectionStateHousekeepingTask, 0, evictionDurationInSeconds, TimeUnit.SECONDS);
        LOGGER.info("ConnectionStateManager.HouseKeeper thread frequency. Time period={}, Time unit={}", evictionDurationInSeconds, TimeUnit.SECONDS);
    }

    static long resolveEvictionDuration() {
        Long evictionDurationInSeconds = Long.getLong(EVICTION_DURATION_SYSTEM_PROPERTY);
        if (evictionDurationInSeconds == null) {
            LOGGER.info("Using default eviction duration of {}", DEFAULT_EVICTION_DURATION_IN_SECONDS);
            return DEFAULT_EVICTION_DURATION_IN_SECONDS;
        } else {
            LOGGER.info("Using non default eviction duration of {}", evictionDurationInSeconds);
            return evictionDurationInSeconds;
        }
    }

    public static synchronized final ConnectionStateManager getInstance() {
        return getInstanceImpl(Clock.systemUTC());
    }

    static synchronized final ConnectionStateManager getInstanceForTesting(Clock clock) {
        return getInstanceImpl(clock);
    }

    static synchronized final void setInstanceForTesting(ConnectionStateManager connectionStateManager) {
        INSTANCE = connectionStateManager;
    }

    private static ConnectionStateManager getInstanceImpl(Clock clock) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionStateManager(clock);
        }
        return INSTANCE;
    }

    private final KeyLockManager<String> poolLockManager = KeyLockManager.newManager();
    private final ConcurrentMutableMap<String, DataSourceWithStatistics> connectionPools = ConcurrentHashMap.newMap();

    private Clock clock;

    public Clock getClock() {
        return clock;
    }

    ConnectionStateManager(Clock clock) {
        // singleton
        this.clock = clock;
    }

    // Synchronizes using concurrent map's locks
    public IdentityState getIdentityStateUsing(Properties properties) {
        String poolName = properties.getProperty(ConnectionStateManager.POOL_NAME_KEY);
        return this.getConnectionStateManagerPOJO(poolName);
    }

    // Synchronizes using concurrent map's locks
    public IdentityState getConnectionStateManagerPOJO(String poolName) {
        DataSourceWithStatistics dataSourceWithStatistics = this.connectionPools.get(poolName);
        return dataSourceWithStatistics != null ? dataSourceWithStatistics.getIdentityState() : null;
    }

    // Synchronizes using concurrent map's locks
    public DataSourceWithStatistics get(String poolName) {
        return this.connectionPools.get(poolName);
    }

    private void atomicallyRemovePool(String poolName, DataSourceStatistics expectedState) {
        synchronized (poolLockManager.getLock(poolName)) {
            DataSourceWithStatistics currentState = this.connectionPools.get(poolName);
            if (currentState.getStatistics().equals(expectedState)) {
                currentState.close();
                this.connectionPools.remove(poolName);
                LOGGER.info("Removed and closed pool {}", poolName);
            }
        }
    }

    protected Set<Pair<String, DataSourceStatistics>> findUnusedPoolsOlderThan(Duration duration) {
        return this.connectionPools.values().stream()
                .filter(ds -> ds.getStatistics().getLastConnectionRequestAge() > duration.toMillis() && !ds.hasActiveConnections())
                .map(ds -> Tuples.pair(ds.getPoolName(), DataSourceStatistics.clone(ds.getStatistics())))
                .collect(Collectors.toSet());
    }

    public void evictUnusedPoolsOlderThan(Duration duration) {
        // step 1 - gather pools to be deleted without acquiring a global lock
        Set<Pair<String, DataSourceStatistics>> entriesToPurge = this.findUnusedPoolsOlderThan(duration);
        LOGGER.info("ConnectionStateManager.HouseKeeper : pools {} to be evicted", entriesToPurge.size());
        // step 2 - remove atomically - i.e remove iff the state has not been updated since it was read in step 1
        entriesToPurge.forEach(pool -> this.atomicallyRemovePool(pool.getOne(), pool.getTwo()));
    }

    public int size() {
        return this.connectionPools.size();
    }

    public String poolNameFor(Identity identity, ConnectionKey key)
    {
        return DBPOOL + key.shortId() + SEPARATOR + identity.getName() + SEPARATOR + identity.getFirstCredential().getClass().getCanonicalName();
    }

    public ConnectionStateManagerPOJO getConnectionStateManagerPOJO() {
        return new ConnectionStateManagerPOJO(this.connectionPools);
    }

    private synchronized void purge(long durationInSeconds) {
        int sizeBeforePurge = this.size();
        LOGGER.info("ConnectionStateManager.HouseKeeper : Starting  with cache size={}", sizeBeforePurge);
        this.evictUnusedPoolsOlderThan(Duration.ofSeconds(durationInSeconds));
        int sizeAfterPurge = this.size();
        LOGGER.info("ConnectionStateManager.HouseKeeper: Evicted={}", sizeBeforePurge - sizeAfterPurge);
    }

    public Optional<ConnectionStateManagerPOJO.ConnectionPool> findByPoolName(String poolName) {
        DataSourceWithStatistics found = this.connectionPools.get(poolName);
        return found == null ? Optional.empty() : Optional.of(buildConnectionPool(found));
    }

    public DataSourceWithStatistics getDataSourceByPoolName(String poolName) {
        return this.connectionPools.get(poolName);
    }

    public List<ConnectionStateManagerPOJO.ConnectionPool> getPoolInformationByUser(String user) {
        List<ConnectionStateManagerPOJO.ConnectionPool> connectionPools = new ArrayList<>();
        this.connectionPools.valuesView().forEach(pool -> {
            if (pool.getPoolPrincipal().equals(user)) {
                connectionPools.add(buildConnectionPool(pool));
            }
        });
        return connectionPools;
    }

    static class ConnectionStateHousekeepingTask implements Runnable {
        private final long durationInSeconds;

        public ConnectionStateHousekeepingTask(long durationInSeconds) {
            this.durationInSeconds = durationInSeconds;
        }

        @Override
        public void run() {
            ConnectionStateManager instance = ConnectionStateManager.getInstance();
            try {
                instance.purge(durationInSeconds);
            } catch (Exception e) {
                LOGGER.error("ConnectionStateManager.HouseKeeper purge failed {}", e);
            }
        }
    }

    public DataSourceWithStatistics getDataSourceForIdentityIfAbsentBuild(IdentityState identityState, DataSourceSpecification dataSourceSpecification, Supplier<DataSource> dataSourceBuilder) {

        String principal = identityState.getIdentity().getName();
        String poolName = poolNameFor(identityState.getIdentity(), dataSourceSpecification.getConnectionKey());
        ConnectionKey connectionKey = dataSourceSpecification.getConnectionKey();
        //why do we need getIfAbsentPut?  the first ever pool creation request will create a new Hikari Data Source
        //because we have configured hikari to fail fast a new connection will be created.
        //This will invoke the DriverWrapper connect method, for this method to create that test connection we need to pass minimal state
        Function0<DataSourceWithStatistics> dsSupplier = () -> new DataSourceWithStatistics(poolName, identityState, dataSourceSpecification);
        DataSource dataSource = this.connectionPools.getIfAbsentPut(poolName, dsSupplier).getDataSource();
        //why this thread safety pattern?  Consider this scenario: poolOne does not exist, then two threads concurrently request poolOne
        //both threads check if pool has been created and both evaluate to true
        //to avoid both threads to create a pool, we enter a sync block to ensure only one thread at a time attempt pool creation
        //thread1 creates poolOne , thread2 enters block and check if indeed the pools does not exist, so will skip
        //this will ensure threads will wait for pool creation while only locking in the scenario there is no pool
        //not for every pool request
        if (dataSource == null) {
            synchronized (poolLockManager.getLock(poolName)) {
                dataSource = this.connectionPools.getIfAbsentPut(poolName, dsSupplier).getDataSource();
                if (dataSource == null) {
                    LOGGER.info("Pool not found for [{}] for datasource [{}], creating one", principal, connectionKey.shortId());
                    try {
                        DataSourceWithStatistics dataSourceWithStatistics = new DataSourceWithStatistics(poolName, dataSourceBuilder.get(), identityState, dataSourceSpecification);
                        this.connectionPools.put(poolName, dataSourceWithStatistics);
                        LOGGER.info("Pool created for [{}] for datasource [{}], name {}", principal, connectionKey.shortId(), poolName);
                    } catch (Exception e) {
                        LOGGER.error("Error creating pool {} {}", poolName, e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        LOGGER.info("Pool found for [{}] in datasource [{}] : pool Name [{}]", principal, connectionKey.shortId(), poolName);
        return this.connectionPools.get(poolName);
    }

    public Object getPoolStatisticsAsJSON(DataSourceWithStatistics poolState) {
        try {
            return objectMapper.writeValueAsString(ConnectionStateManagerPOJO.buildConnectionPool(poolState));
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public Object getPoolStatisticsAsJSON(String poolName) {
        DataSourceWithStatistics pool = connectionPools.get(poolName);
        try {
            return objectMapper.writeValueAsString(ConnectionStateManagerPOJO.buildConnectionPool(pool));
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this.connectionPools) {
            try
            {
                this.connectionPools.keySet().forEach(k -> closeAndRemoveConnectionPool(k));
                EXECUTOR_SERVICE.shutdown();
                EXECUTOR_SERVICE = null;
            }
            catch (Exception e)
            {
                LOGGER.error("Error closing connection manager",e);
            }
        }
    }

    public boolean closeAndRemoveConnectionPool(String poolKey) {
       DataSourceWithStatistics ds =  this.connectionPools.remove(poolKey);
       if( ds!=null)
       {
           LOGGER.info("Closing {} has active connections ? {}", ds.getPoolName(),ds.hasActiveConnections());
           ds.close();
       }
       return true;
    }

    private static class KeyLockManager<K> {
        private static final Function0<Object> NEW_LOCK = () -> new Object();
        private final ConcurrentMutableMap<K, Object> locks = ConcurrentHashMap.newMap();

        private KeyLockManager() {
        }

        /**
         * Get a lock for key.  This "lock" is simply an Object
         * whose intrinsic lock may be used in a synchronized
         * statement.  Each key yields a unique lock.  Each call
         * to this method with a given key will yield the same
         * lock.  This method supports concurrent access.
         *
         * @param key lock key
         * @return key lock
         */
        public Object getLock(K key) {
            return this.locks.getIfAbsentPut(key, NEW_LOCK);
        }

        /**
         * Create a new key lock manager.
         *
         * @param <T> key type
         * @return new key lock manager
         */
        static <T> KeyLockManager<T> newManager() {
            return new KeyLockManager<T>();
        }
    }
}