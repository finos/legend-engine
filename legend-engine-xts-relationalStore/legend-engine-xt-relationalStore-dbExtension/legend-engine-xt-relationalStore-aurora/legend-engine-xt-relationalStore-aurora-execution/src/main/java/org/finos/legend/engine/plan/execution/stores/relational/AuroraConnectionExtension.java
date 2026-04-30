// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.aurora.AuroraManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.AuroraDatasourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.GlobalAuroraDatasourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.AuroraDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.GlobalAuroraDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AuroraDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.GlobalAuroraDatasourceSpecification;

import java.util.List;
import java.util.function.Function;

public class AuroraConnectionExtension implements ConnectionExtension, StrategicConnectionExtension
{
    @Override
    public String type()
    {
        return "MIX_ConnectionExtension_&_Strategic_Connection_Extension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Aurora");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new AuroraManager());
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy -> null;
    }

    @Override
    public AuthenticationStrategyVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy -> null;
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort)
    {
        return connection -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof AuroraDatasourceSpecification)
            {
                AuroraDatasourceSpecification spec = (AuroraDatasourceSpecification) datasourceSpecification;
                return new AuroraDatasourceSpecificationKey(
                        spec.host,
                        spec.port,
                        spec.name,
                        spec.clusterInstanceHostPattern
                );
            }
            if (datasourceSpecification instanceof GlobalAuroraDatasourceSpecification)
            {
                GlobalAuroraDatasourceSpecification spec = (GlobalAuroraDatasourceSpecification) datasourceSpecification;
                return new GlobalAuroraDatasourceSpecificationKey(
                        spec.host,
                        spec.port,
                        spec.name,
                        spec.region,
                        spec.globalClusterInstanceHostPatterns
                );
            }
            return null;
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(Function<RelationalDatabaseConnection, org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy> authenticationStrategyRuntimeProvider)
    {
        return (connection, connectionKey) -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof AuroraDatasourceSpecification)
            {
                return new AuroraDatasourceSpecificationRuntime(
                        (AuroraDatasourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new AuroraManager(),
                        authenticationStrategyRuntimeProvider.apply(connection)
                );
            }
            if (datasourceSpecification instanceof GlobalAuroraDatasourceSpecification)
            {
                return new GlobalAuroraDatasourceSpecificationRuntime(
                        (GlobalAuroraDatasourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new AuroraManager(),
                        authenticationStrategyRuntimeProvider.apply(connection)
                );
            }
            return null;
        };
    }
}
