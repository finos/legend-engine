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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication;

import com.zaxxer.hikari.HikariConfig;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.util.Properties;

public abstract class AuthenticationStrategy
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuthenticationStrategy.class);
    private static final int CONNECTION_TIMEOUT = 30000;
    public static final String AUTHENTICATION_STRATEGY_PROFILE_BY_POOL = "AUTHENTICATION_STRATEGY_PROFILE_BY_POOL";
    public static final String AUTHENTICATION_STRATEGY_KEY = "AUTHENTICATION_STRATEGY_KEY";
    private static final String UNKNOWN_USER = "_UNKNOWN_";

    protected AuthenticationStatistics authenticationStatistics = new AuthenticationStatistics();
    protected String login;
    protected String password;

    private static final ConcurrentMutableMap<String, MutableList<CommonProfile>> profilesByPools = ConcurrentHashMap.newMap();


    protected static MutableList<CommonProfile> getProfiles(String poolId)
    {
        return profilesByPools.get(poolId);
    }

    public static void registerProfilesByPool(String poolName, MutableList<CommonProfile> profiles)
    {
        // The profiles are associated with the Pool as the Pool can create connections by itself in its own threads.
        profilesByPools.put(poolName, profiles);
    }

    public Connection getConnection(DataSourceWithStatistics ds, Subject subject, MutableList<CommonProfile> profiles) throws ConnectionException
    {
        // Refresh the profiles in the pool if they are provided (The profiles were already associated with the Pool at Pool creation)
        if (profiles != null)
        {
            registerProfilesByPool(((HikariConfig)ds.getDataSource()).getPoolName(), profiles);
        }
        try
        {
            return getConnectionImpl(ds, subject, profiles);
        }
        catch (ConnectionException ce)
        {
            this.authenticationStatistics.logConnectionError();
            LOGGER.error("error getting connection (total : {}) {}", this.authenticationStatistics.getTotalConnectionErrors(), ce);
            throw ce;
        }
    }


    protected abstract Connection getConnectionImpl(DataSourceWithStatistics ds, Subject subject, MutableList<CommonProfile> profiles) throws ConnectionException;

    public abstract String getLogin();

    public String getAlternativePrincipal(MutableList<CommonProfile> profiles)
    {
        return UNKNOWN_USER;
    }

    public abstract String getPassword();

    public abstract Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager);

    protected Connection getConnectionUsingKerberos(DataSource ds, Subject subject)
    {
        Connection connection;
        try
        {
            connection = Subject.doAs(subject, (PrivilegedExceptionAction<Connection>)ds::getConnection);
        }
        catch (PrivilegedActionException e)
        {
            LOGGER.error("PrivilegedActionException for subject {} {} []", subject, e);
            throw new ConnectionException(e.getException());
        }
        catch (RuntimeException e)
        {
            LOGGER.error("RuntimeException for subject {} {} []", subject, e);
            throw new ConnectionException(e);
        }
        return connection;
    }

    public AuthenticationStatistics getAuthenticationStatistics()
    {
        return this.authenticationStatistics;
    }

    public int getConnectionTimeout()
    {
        return CONNECTION_TIMEOUT;
    }

    public abstract AuthenticationStrategyKey getKey();
}
