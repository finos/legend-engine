package org.finos.legend.engine.authentication.flows;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.cloud.GCPWorkforceConfig;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

public class BigQueryWithGCPWorkforceIdentityFederationFlow implements DatabaseAuthenticationFlow<BigQueryDatasourceSpecification, GCPWorkloadIdentityFederationAuthenticationStrategy>
{
    private final GCPWorkforceConfig gcpWorkforceConfig;

    public BigQueryWithGCPWorkforceIdentityFederationFlow(GCPWorkforceConfig gcpWorkforceConfig)
    {
        this.gcpWorkforceConfig = gcpWorkforceConfig;
    }
    @Override
    public Class<BigQueryDatasourceSpecification> getDatasourceClass()
    {
        return BigQueryDatasourceSpecification.class;
    }

    @Override
    public Class<GCPWorkloadIdentityFederationAuthenticationStrategy> getAuthenticationStrategyClass() {
        return null;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return null;
    }

    @Override
    public Credential makeCredential(Identity identity, BigQueryDatasourceSpecification datasourceSpecification, GCPWorkloadIdentityFederationAuthenticationStrategy authenticationStrategy) throws Exception {
        return null;
    }
}
