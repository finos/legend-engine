//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.SnowflakePublicAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;

import java.util.List;
import java.util.function.Function;

public class SnowflakeConnectionExtension implements ConnectionExtension, StrategicConnectionExtension
{
    @Override
    public String type()
    {
        return "MIX_ConnectionExtension_&_Strategic_Connection_Extension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Snowflake");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new SnowflakeManager());
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
            {
                SnowflakePublicAuthenticationStrategy snowflakePublicAuthenticationStrategy = (SnowflakePublicAuthenticationStrategy) authenticationStrategy;

                return new SnowflakePublicAuthenticationStrategyKey(
                        snowflakePublicAuthenticationStrategy.privateKeyVaultReference,
                        snowflakePublicAuthenticationStrategy.passPhraseVaultReference,
                        snowflakePublicAuthenticationStrategy.publicUserName
                );
            }
            return null;
        };
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
            {
                SnowflakePublicAuthenticationStrategy snowflakePublicAuthenticationStrategy = (SnowflakePublicAuthenticationStrategy) authenticationStrategy;
                return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy(
                        snowflakePublicAuthenticationStrategy.privateKeyVaultReference,
                        snowflakePublicAuthenticationStrategy.passPhraseVaultReference,
                        snowflakePublicAuthenticationStrategy.publicUserName
                );
            }
            return null;
        };
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort)
    {
        return connection -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
            {
                SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
                return new SnowflakeDataSourceSpecificationKey(
                        snowflakeDatasourceSpecification.accountName,
                        snowflakeDatasourceSpecification.region,
                        snowflakeDatasourceSpecification.warehouseName,
                        snowflakeDatasourceSpecification.databaseName,
                        snowflakeDatasourceSpecification.cloudType,
                        connection.quoteIdentifiers,
                        snowflakeDatasourceSpecification.enableQueryTags,
                        snowflakeDatasourceSpecification.proxyHost,
                        snowflakeDatasourceSpecification.proxyPort,
                        snowflakeDatasourceSpecification.nonProxyHosts,
                        snowflakeDatasourceSpecification.accountType,
                        snowflakeDatasourceSpecification.organization,
                        snowflakeDatasourceSpecification.role,
                        snowflakeDatasourceSpecification.tempTableDb,
                        snowflakeDatasourceSpecification.tempTableSchema);
            }
            return null;
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(Function<RelationalDatabaseConnection, AuthenticationStrategy> authenticationStrategyProvider)
    {
        return (connection, connectionKey) -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
            {
                return new SnowflakeDataSourceSpecification(
                        (SnowflakeDataSourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new SnowflakeManager(),
                        authenticationStrategyProvider.apply(connection));
            }
            return null;
        };
    }

    @Override
    public Boolean getQuotedIdentifiersIgnoreCase(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
            return snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase == null || !snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase;
        }
        return null;
    }

    @Override
    public boolean isLocalMode(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification)datasourceSpecification;
            return snowflakeDatasourceSpecification.accountName.startsWith("legend-local-snowflake-accountName");
        }
        return false;
    }
}
