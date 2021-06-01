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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public class RelationalConnectionManager implements ConnectionManager
{
    private final int testDbPort;
    private final ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey;

    private MutableList<AuthenticationStrategyVisitor<AuthenticationStrategyKey>> authenticationStrategyKeyVisitors;
    private MutableList<AuthenticationStrategyVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy>> authenticationStrategyTrans;
    private MutableList<Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>> dataSourceKeys;
    private MutableList<Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>>> dataSourceTrans;

    public RelationalConnectionManager(int testDbPort, List<OAuthProfile> oauthProfiles, ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey, RelationalExecutorInfo relationalExecutorInfo)
    {
        MutableList<StrategicConnectionExtension> extensions = Iterate.addAllTo(ServiceLoader.load(StrategicConnectionExtension.class), Lists.mutable.empty());

        this.testDbPort = testDbPort;
        this.dbSpecByKey = dbSpecByKey;

        this.authenticationStrategyKeyVisitors =
                Lists.mutable.<AuthenticationStrategyVisitor<AuthenticationStrategyKey>>with(new AuthenticationStrategyKeyGenerator())
                .withAll(extensions.collect(StrategicConnectionExtension::getExtraAuthenticationKeyGenerators));

        this.authenticationStrategyTrans =
                Lists.mutable.<AuthenticationStrategyVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy>>with(new AuthenticationStrategyTransformer(oauthProfiles))
                        .withAll(extensions.collect(extension -> extension.getExtraAuthenticationStrategyTransformGenerators(oauthProfiles)));

        this.dataSourceKeys =
                Lists.mutable.<Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>>with(c -> new DataSourceSpecificationKeyGenerator(testDbPort, c))
                .withAll(extensions.collect(a -> a.getExtraDataSourceSpecificationKeyGenerators(testDbPort)));

        this.dataSourceTrans =
                Lists.mutable.<Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>>>with(
                    (r, c) -> new DataSourceSpecificationTransformer(
                            relationalExecutorInfo,
                            c.getDataSourceSpecificationKey(),
                            ListIterate.collect(this.authenticationStrategyTrans, visitor -> r.authenticationStrategy.accept(visitor))
                                    .select(Objects::nonNull)
                                    .getFirstOptional()
                                    .orElseThrow(() -> new UnsupportedOperationException("No transformer provided for Authentication Strategy - " + r.authenticationStrategy.getClass().getName())),
                            r)
                ).withAll(extensions.collect(a -> a.getExtraDataSourceSpecificationTransformerGenerators(oauthProfiles,relationalExecutorInfo)));
    }

    private DataSourceSpecificationKey buildDataSourceKey(DatasourceSpecification datasourceSpecification, RelationalDatabaseConnection relationalDatabaseConnection)
    {
        DataSourceSpecificationKey key = this.dataSourceKeys.collect(f -> datasourceSpecification.accept(f.apply(relationalDatabaseConnection))).detect(Objects::nonNull);
        if (key == null)
        {
            throw new RuntimeException(datasourceSpecification.getClass() + " is not supported!");
        }
        return key;
    }

    private AuthenticationStrategyKey buildAuthStrategyKey(AuthenticationStrategy authenticationStrategy)
    {
        AuthenticationStrategyKey key = this.authenticationStrategyKeyVisitors.collect(f -> authenticationStrategy.accept(f)).detect(Objects::nonNull);
        if (key == null)
        {
            throw new RuntimeException(authenticationStrategy.getClass() + " is not supported!");
        }
        return key;
    }

    private DataSourceSpecification buildDataSourceTrans(DatasourceSpecification datasourceSpecification, RelationalDatabaseConnection relationalDatabaseConnection, ConnectionKey connectionKey)
    {
        DataSourceSpecification key = this.dataSourceTrans.collect(f -> datasourceSpecification.accept(f.value(relationalDatabaseConnection, connectionKey))).detect(Objects::nonNull);
        if (key == null)
        {
            throw new RuntimeException(datasourceSpecification.getClass() + " is not supported!");
        }
        return key;
    }


    public Connection getTestDatabaseConnection()
    {
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

    public ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection connection)
    {
        if (connection instanceof RelationalDatabaseConnection)
        {
            RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
            return new ConnectionKey(
                    buildDataSourceKey(relationalDatabaseConnection.datasourceSpecification, relationalDatabaseConnection),
                    buildAuthStrategyKey(relationalDatabaseConnection.authenticationStrategy)
            );
        }
        return null;
    }

    public DataSourceSpecification getDataSourceSpecification(DatabaseConnection connection)
    {
        if (connection instanceof RelationalDatabaseConnection)
        {
            RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
            ConnectionKey connectionKey = this.generateKeyFromDatabaseConnection(connection);
            return dbSpecByKey.getIfAbsentPutWithKey(connectionKey, key -> buildDataSourceTrans(relationalDatabaseConnection.datasourceSpecification, relationalDatabaseConnection, connectionKey));
        }
        return null;
    }
}