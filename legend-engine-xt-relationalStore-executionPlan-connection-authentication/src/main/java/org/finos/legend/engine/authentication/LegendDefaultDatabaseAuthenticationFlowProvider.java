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

package org.finos.legend.engine.authentication;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.flows.BigQueryWithGCPApplicationDefaultCredentialsFlow;
import org.finos.legend.engine.authentication.flows.BigQueryWithGCPWorkloadIdentityFederationFlow;
import org.finos.legend.engine.authentication.flows.DatabricksWithApiTokenFlow;
import org.finos.legend.engine.authentication.flows.H2StaticWithTestUserPasswordFlow;
import org.finos.legend.engine.authentication.flows.RedshiftWithUserPasswordFlow;
import org.finos.legend.engine.authentication.flows.SnowflakeWithKeyPairFlow;
import org.finos.legend.engine.authentication.flows.SqlServerStaticWithUserPasswordFlow;
import org.finos.legend.engine.authentication.provider.AbstractDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

public final class LegendDefaultDatabaseAuthenticationFlowProvider extends AbstractDatabaseAuthenticationFlowProvider
{
    private LegendDefaultDatabaseAuthenticationFlowProviderConfiguration databaseAuthenticationFlowProviderConfiguration;

    public LegendDefaultDatabaseAuthenticationFlowProvider()
    {
    }

    private ImmutableList<DatabaseAuthenticationFlow<? extends DatasourceSpecification, ? extends AuthenticationStrategy>> flows()
    {
        return Lists.immutable.of(
                new BigQueryWithGCPApplicationDefaultCredentialsFlow(),
                new BigQueryWithGCPWorkloadIdentityFederationFlow(databaseAuthenticationFlowProviderConfiguration),
                new DatabricksWithApiTokenFlow(),
                new H2StaticWithTestUserPasswordFlow(),
                new SnowflakeWithKeyPairFlow(),
                new SqlServerStaticWithUserPasswordFlow(),
                new RedshiftWithUserPasswordFlow()
        );
    }

    @Override
    public void configure(DatabaseAuthenticationFlowProviderConfiguration configuration)
    {
        if (!(configuration instanceof LegendDefaultDatabaseAuthenticationFlowProviderConfiguration))
        {
            String message = "Mismatch in flow provider configuration. It should be an instance of " + LegendDefaultDatabaseAuthenticationFlowProviderConfiguration.class.getSimpleName();
            throw new RuntimeException(message);
        }
        this.databaseAuthenticationFlowProviderConfiguration = (LegendDefaultDatabaseAuthenticationFlowProviderConfiguration) configuration;
        flows().forEach(this::registerFlow);
    }
}