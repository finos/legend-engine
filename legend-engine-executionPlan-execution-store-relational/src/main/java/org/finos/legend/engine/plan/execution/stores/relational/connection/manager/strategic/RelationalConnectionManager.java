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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;

import java.sql.Connection;
import java.util.List;

public class RelationalConnectionManager implements ConnectionManager {
    private final int testDbPort;
    private final ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey;
    private final ImmutableMap<DatabaseType, RelationalConnectionPlugin> plugins;

    private RelationalExecutorInfo relationalExecutorInfo;

    // TODO : epsstan : Do we need the oauth profiles here ??
    public RelationalConnectionManager(int testDbPort, List<OAuthProfile> oauthProfiles, ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey, RelationalExecutorInfo relationalExecutorInfo) {
        this.relationalExecutorInfo = relationalExecutorInfo;
        this.testDbPort = testDbPort;
        this.dbSpecByKey = dbSpecByKey;
        this.plugins = new RelationalConnectionPluginLoader().loadRelationalPlugins();
    }

    public Connection getTestDatabaseConnection() {
        StaticDatasourceSpecification datasourceSpecification = new StaticDatasourceSpecification();
        datasourceSpecification.databaseName = "testDB";
        datasourceSpecification.host = "127.0.0.1";
        datasourceSpecification.port = testDbPort;

        RelationalDatabaseConnection testConnection = new RelationalDatabaseConnection();
        testConnection.datasourceSpecification = datasourceSpecification;
        testConnection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        testConnection.type = DatabaseType.H2;

        return this.getDataSourceSpecification(testConnection).getConnectionUsingSubject(null);
    }

    // TODO : epsstan : Can we eliminate the concrete key classes ??
    public ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection connection) {
        if (connection instanceof RelationalDatabaseConnection) {
            RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
            return this.plugins.get(relationalDatabaseConnection.type).buildConnectionKey(relationalDatabaseConnection);
        }
        return null;
    }

    public DataSourceSpecification getDataSourceSpecification(DatabaseConnection connection) {
        if (connection instanceof RelationalDatabaseConnection) {
            RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
            ConnectionKey connectionKey = this.generateKeyFromDatabaseConnection(connection);
            return dbSpecByKey.getIfAbsentPutWithKey(connectionKey, key -> this.plugins.get(relationalDatabaseConnection.type).buildDatasourceSpecification(relationalDatabaseConnection, relationalExecutorInfo));
        }
        return null;
    }
}