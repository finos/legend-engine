//  Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TrinoDelegatedKerberosAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.TrinoDelegatedKerberosAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.trino.TrinoManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.TrinoDatasourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.TrinoDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TrinoDelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

public class TrinoConnectionExtension implements ConnectionExtension, StrategicConnectionExtension
{
    @Override
    public String type()
    {
        return "MIX_ConnectionExtension_&_Strategic_Connection_Extension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new TrinoManager());
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof TrinoDelegatedKerberosAuthenticationStrategy)
            {
                TrinoDelegatedKerberosAuthenticationStrategy trinoDelegatedKerberosAuthenticationStrategy = (TrinoDelegatedKerberosAuthenticationStrategy) authenticationStrategy;

                return new TrinoDelegatedKerberosAuthenticationStrategyKey(trinoDelegatedKerberosAuthenticationStrategy.serverPrincipal,
                        trinoDelegatedKerberosAuthenticationStrategy.kerberosRemoteServiceName, trinoDelegatedKerberosAuthenticationStrategy.kerberosUseCanonicalHostname);
            }
            return null;
        };
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof TrinoDelegatedKerberosAuthenticationStrategy)
            {
                TrinoDelegatedKerberosAuthenticationStrategy trinoDelegatedKerberosAuthenticationStrategy = (TrinoDelegatedKerberosAuthenticationStrategy) authenticationStrategy;
                return new TrinoDelegatedKerberosAuthenticationStrategyRuntime(
                        trinoDelegatedKerberosAuthenticationStrategy.serverPrincipal,
                        trinoDelegatedKerberosAuthenticationStrategy.kerberosRemoteServiceName,
                        trinoDelegatedKerberosAuthenticationStrategy.kerberosUseCanonicalHostname
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
            if (datasourceSpecification instanceof TrinoDatasourceSpecification)
            {
                TrinoDatasourceSpecification trinoDatasourceSpecification = (TrinoDatasourceSpecification) datasourceSpecification;
                return new TrinoDatasourceSpecificationKey(
                        trinoDatasourceSpecification.host,
                        trinoDatasourceSpecification.port,
                        trinoDatasourceSpecification.catalog,
                        trinoDatasourceSpecification.schema,
                        trinoDatasourceSpecification.clientTags,
                        trinoDatasourceSpecification.sslSpecification
                );
            }
            return null;
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(Function<RelationalDatabaseConnection, AuthenticationStrategy> authenticationStrategyProvider)
    {
        return (connection, connectionKey) -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof TrinoDatasourceSpecification)
            {
                return new TrinoDatasourceSpecificationRuntime(
                        (TrinoDatasourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new TrinoManager(),
                        authenticationStrategyProvider.apply(connection),
                        new Properties()
                );
            }
            return null;
        };
    }
}
