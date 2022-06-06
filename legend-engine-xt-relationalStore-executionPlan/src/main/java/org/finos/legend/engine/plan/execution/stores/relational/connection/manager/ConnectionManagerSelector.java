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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public class ConnectionManagerSelector
{
    private final Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder;
    private MutableList<ConnectionManager> connectionManagers;

    public ConnectionManagerSelector(TemporaryTestDbConfiguration temporaryTestDb, List<OAuthProfile> oauthProfiles)
    {
        this(temporaryTestDb, oauthProfiles, Optional.empty());
    }

    public ConnectionManagerSelector(TemporaryTestDbConfiguration temporaryTestDb, List<OAuthProfile> oauthProfiles, Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder)
    {
        MutableList<ConnectionManagerExtension> extensions = Iterate.addAllTo(ServiceLoader.load(ConnectionManagerExtension.class), Lists.mutable.empty());
        this.connectionManagers = Lists.mutable.<ConnectionManager>with(
                new RelationalConnectionManager(temporaryTestDb.port, oauthProfiles, flowProviderHolder)
        ).withAll(extensions.collect(e -> e.getExtensionManager(temporaryTestDb.port, oauthProfiles)));
        this.flowProviderHolder = flowProviderHolder;
    }

    public Connection getDatabaseConnection(MutableList<CommonProfile> profiles, DatabaseConnection databaseConnection)
    {
        DataSourceSpecification datasource = getDataSourceSpecification(databaseConnection);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        return this.getDatabaseConnectionImpl(identity, databaseConnection, datasource);
    }

    public Connection getDatabaseConnection(Subject subject, DatabaseConnection databaseConnection)
    {
        DataSourceSpecification datasource = getDataSourceSpecification(databaseConnection);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(subject);
        return this.getDatabaseConnectionImpl(identity, databaseConnection, datasource);
    }

    private DataSourceSpecification getDataSourceSpecification(DatabaseConnection databaseConnection)
    {
        DataSourceSpecification datasource = this.connectionManagers.collect(c -> c.getDataSourceSpecification(databaseConnection)).detect(Objects::nonNull);
        if (datasource == null)
        {
            throw new RuntimeException("Not Supported! " + databaseConnection.getClass());
        }
        return datasource;
    }

    public Connection getDatabaseConnection(Identity identity, DatabaseConnection databaseConnection)
    {
        DataSourceSpecification datasource = getDataSourceSpecification(databaseConnection);
        return this.getDatabaseConnectionImpl(identity, databaseConnection, datasource);
    }

    public Connection getDatabaseConnectionImpl(Identity identity, DatabaseConnection databaseConnection, DataSourceSpecification datasource)
    {
        if (databaseConnection instanceof RelationalDatabaseConnection)
        {
            RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) databaseConnection;
            Optional<CredentialSupplier> databaseCredentialHolder = RelationalConnectionManager.getCredential(flowProviderHolder, relationalDatabaseConnection, identity);
            return datasource.getConnectionUsingIdentity(identity, databaseCredentialHolder);
        }
        /*
            In some cases, connection managers can return DatabaseConnections that are not RelationalDatabaseConnection.
            Without the metadata associated with a RelationalDatabaseConnection we cannot compute a credential.
        */
        return datasource.getConnectionUsingIdentity(identity, Optional.empty());
    }

    public ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection databaseConnection)
    {
        ConnectionKey key = this.connectionManagers.collect(c -> c.generateKeyFromDatabaseConnection(databaseConnection)).detect(Objects::nonNull);
        if (key == null)
        {
            throw new RuntimeException("Not Supported! " + databaseConnection.getClass());
        }
        return key;
    }

    public Connection getTestDatabaseConnection()
    {
        Connection connection = this.connectionManagers.collect(ConnectionManager::getTestDatabaseConnection).detect(Objects::nonNull);
        if (connection == null)
        {
            throw new RuntimeException("Not Supported! ");
        }
        return connection;
    }

    public static org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection transformToTestConnectionSpecification(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection originalConnection, String testData, List<String> setupSqls)
    {
        RelationalDatabaseConnection db = new RelationalDatabaseConnection();
        db.datasourceSpecification = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification(testData, setupSqls);
        db.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        db.databaseType = DatabaseType.H2;
        db.type = DatabaseType.H2;
        db.element = originalConnection.element;
        db.timeZone = originalConnection instanceof DatabaseConnection ? ((DatabaseConnection) originalConnection).timeZone : null;
        db.quoteIdentifiers = originalConnection instanceof DatabaseConnection ? ((DatabaseConnection) originalConnection).quoteIdentifiers : null;
        return db;
    }
}
