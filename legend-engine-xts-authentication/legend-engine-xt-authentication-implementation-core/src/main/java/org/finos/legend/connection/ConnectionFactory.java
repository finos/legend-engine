package org.finos.legend.connection;

import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

public class ConnectionFactory
{
    private final ConnectionSetupFlowProvider flowProviderHolder;
    private final CredentialProviderProvider credentialProviderProvider;

    public ConnectionFactory(ConnectionSetupFlowProvider flowProviderHolder, CredentialProviderProvider credentialProviderProvider)
    {
        this.flowProviderHolder = flowProviderHolder;
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public <T> T setupConnection(ConnectionSetupSpecification<T> connectionSetupSpecification, Credential credential) throws Exception
    {
        ConnectionSetupFlow<T, ConnectionSetupSpecification<T>, Credential> flow = this.flowProviderHolder.lookupFlowOrThrow(connectionSetupSpecification, credential);
        return flow.setupConnection(connectionSetupSpecification, credential);
    }

    public <T> T setupConnection(ConnectionSetupSpecification<T> connectionSetupSpecification, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        return this.setupConnection(connectionSetupSpecification, CredentialBuilder.makeCredential(this.credentialProviderProvider, authenticationSpecification, identity));
    }
}
