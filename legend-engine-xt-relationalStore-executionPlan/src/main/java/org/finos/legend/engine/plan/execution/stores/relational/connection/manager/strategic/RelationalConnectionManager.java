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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

public class RelationalConnectionManager implements ConnectionManager
{
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String TEST_DB = "testDB";
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalConnectionManager.class);
    private final int testDbPort;
    private MutableList<AuthenticationStrategyVisitor<AuthenticationStrategyKey>> authenticationStrategyKeyVisitors;
    private MutableList<AuthenticationStrategyVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy>> authenticationStrategyTrans;
    private MutableList<Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>> dataSourceKeys;
    private MutableList<Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>>> dataSourceTrans;
    private Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder;

    public RelationalConnectionManager(int testDbPort, List<OAuthProfile> oauthProfiles)
    {
        this(testDbPort, oauthProfiles, Optional.empty());
    }

    public RelationalConnectionManager(int testDbPort, List<OAuthProfile> oauthProfiles, Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder)
    {
        this.flowProviderHolder = flowProviderHolder;
        MutableList<StrategicConnectionExtension> extensions = Iterate.addAllTo(ServiceLoader.load(StrategicConnectionExtension.class), Lists.mutable.empty());

        this.testDbPort = testDbPort;
        this.authenticationStrategyKeyVisitors =
                Lists.mutable.<AuthenticationStrategyVisitor<AuthenticationStrategyKey>>with(new AuthenticationStrategyKeyGenerator())
                        .withAll(extensions.collect(StrategicConnectionExtension::getExtraAuthenticationKeyGenerators));

        this.authenticationStrategyTrans =
                Lists.mutable.<AuthenticationStrategyVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy>>with(new AuthenticationStrategyTransformer(oauthProfiles))
                        .withAll(extensions.collect(extension -> extension.getExtraAuthenticationStrategyTransformGenerators(oauthProfiles)));

        this.dataSourceKeys =
                Lists.mutable.<Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>>with(c -> new DataSourceSpecificationKeyGenerator(testDbPort, c))
                        .withAll(extensions.collect(a -> a.getExtraDataSourceSpecificationKeyGenerators(testDbPort)));

        Function<RelationalDatabaseConnection, org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy> authenticationStrategyProvider =
                r -> ListIterate.collect(this.authenticationStrategyTrans, visitor -> r.authenticationStrategy.accept(visitor))
                        .select(Objects::nonNull)
                        .getFirstOptional()
                        .orElseThrow(() -> new UnsupportedOperationException("No transformer provided for Authentication Strategy - " + r.authenticationStrategy.getClass().getName()));

        this.dataSourceTrans =
                Lists.mutable.<Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>>>with(
                        (r, c) -> new DataSourceSpecificationTransformer(
                                c.getDataSourceSpecificationKey(),
                                authenticationStrategyProvider.apply(r),
                                r)
                ).withAll(extensions.collect(a -> a.getExtraDataSourceSpecificationTransformerGenerators(authenticationStrategyProvider)));
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
        // TODO : pass identity into this method
        RelationalDatabaseConnection testConnection = buildTestDatabaseDatasourceSpecification();
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity((Subject) null);
        Optional<CredentialSupplier> credentialHolder = RelationalConnectionManager.getCredential(flowProviderHolder, testConnection, identity, StoreExecutionState.emptyRuntimeContext());
        return this.getDataSourceSpecification(testConnection).getConnectionUsingIdentity(identity, credentialHolder);
    }

    public RelationalDatabaseConnection buildTestDatabaseDatasourceSpecification()
    {
        StaticDatasourceSpecification datasourceSpecification = new StaticDatasourceSpecification();
        datasourceSpecification.databaseName = TEST_DB;
        datasourceSpecification.host = LOCAL_HOST;
        datasourceSpecification.port = testDbPort;

        RelationalDatabaseConnection testConnection = new RelationalDatabaseConnection();
        testConnection.datasourceSpecification = datasourceSpecification;
        testConnection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        testConnection.type = DatabaseType.H2;
        return testConnection;
    }

    public static Optional<CredentialSupplier> getCredential(Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder, RelationalDatabaseConnection connection, Identity identity, StoreExecutionState.RuntimeContext runtimeContext)
    {
        if (!flowProviderHolder.isPresent())
        {
            // The use of flows has not been enabled
            return Optional.empty();
        }
        return RelationalConnectionManager.getCredential(flowProviderHolder.get(), connection, identity, runtimeContext);
    }

    public static Optional<CredentialSupplier> getCredential(DatabaseAuthenticationFlowProvider flowProvider, RelationalDatabaseConnection connection, Identity identity, StoreExecutionState.RuntimeContext runtimeContext)
    {
        Optional<DatabaseAuthenticationFlow> flowHolder = flowProvider.lookupFlow(connection);
        if (!flowHolder.isPresent())
        {
            /*
              When the flow feature is fully enabled, a missing flow is a bug and should be failed at runtime.
              Fow now, we are lenient and fallback to the existing implementation which uses identity directly.
            */
            String message = String.format(
                    "Database authentication flow feature has been enabled. But flow for DbType=%s, AuthType=%s has not been configured",
                    connection.datasourceSpecification.getClass().getSimpleName(),
                    connection.authenticationStrategy.getClass().getSimpleName());
            LOGGER.warn(message);
            return Optional.empty();
        }
        CredentialSupplier credentialSupplier = new CredentialSupplier(flowHolder.get(), connection.datasourceSpecification, connection.authenticationStrategy, DatabaseAuthenticationFlow.RuntimeContext.newWith(runtimeContext.getContextParams()));
        return Optional.of(credentialSupplier);
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
            return buildDataSourceTrans(relationalDatabaseConnection.datasourceSpecification, relationalDatabaseConnection, connectionKey);
        }
        return null;
    }
}