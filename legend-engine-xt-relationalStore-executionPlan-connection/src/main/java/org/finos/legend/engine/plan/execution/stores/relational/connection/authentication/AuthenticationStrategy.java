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

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.sql.DataSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class AuthenticationStrategy
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuthenticationStrategy.class);
    private static final int CONNECTION_TIMEOUT = Math.toIntExact(SECONDS.toMillis(30L));
    public static String AUTHENTICATION_STRATEGY_KEY = "AUTHENTICATION_STRATEGY_KEY";
    protected AuthenticationStatistics authenticationStatistics = new AuthenticationStatistics();

    /*
        Note : This method is called when the DataSource is first initialized to serve a user request.
        Also note that this method can be called multiple times for the same user if the user is routed to different engine backends (JVMs).
     */
    public Connection getConnection(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            return this.getConnectionImpl(ds, identity);
        }
        catch (ConnectionException ce)
        {
            ds.logConnectionError();
            LOGGER.error("error getting connection (total : {}) {}", ds.getStatistics().getTotalConnectionErrors(), ce);
            throw ce;
        }
    }

    public abstract Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException;

    /*
        Note : This method is called when the Hikari DataSource needs a connection. This happens under two conditions :

        1/ The very first time the Hikari DataSource is being initialized. The above getConnection method is invoked in DataSourceSpecification.getConnection. When the Hikari DataSource is being initialized, it invokes the DriverWrapper
        which invokes handleConnection on the authentication strategy

        2/ Whenever the Hikari DataSource's connection pool needs to be refreshed. Using the same DriverWrapper mechanism, the pool invokes handleConnection. This allows the authentication strategy to populate JDBC properties to be used in
        connection creation.
     */

    public abstract Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager);

    protected Connection getConnectionUsingKerberos(DataSource ds, Subject subject)
    {
        Connection connection;
        try
        {
            connection = Subject.doAs(subject, (PrivilegedExceptionAction<Connection>) ds::getConnection);
        }
        catch (PrivilegedActionException e)
        {
            this.authenticationStatistics.logAuthenticationError();
            LOGGER.error("PrivilegedActionException for subject {} {}", subject, e);
            throw new ConnectionException(e.getException());
        }
        catch (RuntimeException e)
        {
            LOGGER.error("RuntimeException for subject {} {}", subject, e);
            throw new ConnectionException(e);
        }
        return connection;
    }

    public AuthenticationStatistics getAuthenticationStatistics()
    {
        return this.authenticationStatistics;
    }

    //this should return an int
    public int getConnectionTimeout()
    {
        return CONNECTION_TIMEOUT;
    }

    public abstract AuthenticationStrategyKey getKey();

    protected Credential getDatabaseCredential(IdentityState identityState)
    {
        try
        {
            Identity identity = identityState.getIdentity();
            CredentialSupplier credentialSupplier = identityState.getCredentialSupplier().get();
            return credentialSupplier.getCredential(identity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Properties getAuthenticationPropertiesForConnection()
    {
        return new Properties();
    }

}
