package org.finos.legend.engine.authentication;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.authentication.flows.BigQueryWithGCPApplicationDefaultCredentialsFlow;
import org.finos.legend.engine.authentication.flows.BigQueryWithGCPWorkloadIdentityFederationFlow;
import org.finos.legend.engine.authentication.provider.AbstractDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

public class BigQueryTestDatabaseAuthenticationFlowProvider extends AbstractDatabaseAuthenticationFlowProvider {
    private ImmutableList<DatabaseAuthenticationFlow<? extends DatasourceSpecification, ? extends AuthenticationStrategy>> flows(BigQueryTestDatabaseAuthenticationFlowProviderConfiguration configuration)
    {
        return Lists.immutable.of(
                new BigQueryWithGCPWorkloadIdentityFederationFlow(configuration.getAwsConfig(), configuration.getGcpWorkloadConfig()),
                new BigQueryWithGCPApplicationDefaultCredentialsFlow()
        );
    }

    @Override
    public void configure(DatabaseAuthenticationFlowProviderConfiguration configuration)
    {
        if (!(configuration instanceof BigQueryTestDatabaseAuthenticationFlowProviderConfiguration))
        {
            String message = "Mismatch in flow provider configuration. It should be an instance of " + BigQueryTestDatabaseAuthenticationFlowProviderConfiguration.class.getSimpleName();
            throw new RuntimeException(message);
        }
        flows((BigQueryTestDatabaseAuthenticationFlowProviderConfiguration) configuration).forEach(this::registerFlow);
    }
}
