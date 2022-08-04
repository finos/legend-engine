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

package org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.extensions;

import java.util.List;
import java.util.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPApplicationDefaultCredentialsAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.driver.SpannerManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.ds.specifications.SpannerDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.spanner.ds.specifications.keys.SpannerDataSourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;

public class SpannerStrategicConnectionExtension implements StrategicConnectionExtension
{
    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof GCPApplicationDefaultCredentialsAuthenticationStrategy)
            {
                return new GCPApplicationDefaultCredentialsAuthenticationStrategyKey();
            }
            return null;
        };
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(
        List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof GCPApplicationDefaultCredentialsAuthenticationStrategy)
            {
                return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy();
            }
            return null;
        };
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(
        int testDbPort)
    {
        return relationalDatabaseConnection -> (DatasourceSpecificationVisitor<DataSourceSpecificationKey>) datasourceSpecification ->
        {
            if (datasourceSpecification instanceof SpannerDatasourceSpecification)
            {
                SpannerDatasourceSpecification spannerSpec =
                    (SpannerDatasourceSpecification) datasourceSpecification;
                return new SpannerDataSourceSpecificationKey(
                    spannerSpec.projectId,
                    spannerSpec.instanceId,
                    spannerSpec.databaseId,
                    spannerSpec.proxyHost,
                    spannerSpec.proxyPort
                );
            }
            return null;
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey,
        DatasourceSpecificationVisitor<DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(
        List<OAuthProfile> oauthProfiles, RelationalExecutorInfo relationalExecutorInfo)
    {
        return (relationalDatabaseConnection, connectionKey) -> (DatasourceSpecificationVisitor<DataSourceSpecification>) datasourceSpecification ->
        {

            if (datasourceSpecification instanceof SpannerDatasourceSpecification)
            {
                AuthenticationStrategyVisitor<AuthenticationStrategy> visitor =
                    getExtraAuthenticationStrategyTransformGenerators(oauthProfiles);
                if (visitor == null)
                {
                    return null;
                }
                AuthenticationStrategy authenticationStrategy =
                    visitor.visit(relationalDatabaseConnection.authenticationStrategy);

                return new SpannerDataSourceSpecification(
                    (SpannerDataSourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                    new SpannerManager(),
                    authenticationStrategy
                );
            }
            return null;
        };
    }
}
