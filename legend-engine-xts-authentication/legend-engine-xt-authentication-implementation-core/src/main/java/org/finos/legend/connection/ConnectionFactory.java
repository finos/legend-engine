// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection;

import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.ServiceLoader;

public class ConnectionFactory
{
    private final ConnectionSetupFlowProvider flowProviderHolder;
    private final CredentialProviderProvider credentialProviderProvider;

    public ConnectionFactory(ConnectionSetupFlowProvider flowProviderHolder, CredentialProviderProvider credentialProviderProvider)
    {
        this.flowProviderHolder = flowProviderHolder;
        this.credentialProviderProvider = credentialProviderProvider;
    }

    public void initialize()
    {
        for (ConnectionManager connectionManager : ServiceLoader.load(ConnectionManager.class))
        {
            connectionManager.initialize();
        }
    }

    public <T> T getConnection(ConnectionSetupSpecification<T> connectionSetupSpecification, Credential credential) throws Exception
    {
        ConnectionFactoryFlow<T, ConnectionSetupSpecification<T>, Credential> flow = this.flowProviderHolder.lookupFlowOrThrow(connectionSetupSpecification, credential);
        return flow.getConnection(connectionSetupSpecification, credential);
    }

    public <T> T getConnection(ConnectionSetupSpecification<T> connectionSetupSpecification, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        return this.getConnection(connectionSetupSpecification, CredentialBuilder.makeCredential(this.credentialProviderProvider, authenticationSpecification, identity));
    }

    public <T> T configureConnection(T connection, ConnectionSetupSpecification<T> connectionSetupSpecification, Credential credential) throws Exception
    {
        ConnectionFactoryFlow<T, ConnectionSetupSpecification<T>, Credential> flow = this.flowProviderHolder.lookupFlowOrThrow(connectionSetupSpecification, credential);
        return flow.getConnection(connectionSetupSpecification, credential);
    }

    public <T> T configureConnection(T connection, ConnectionSetupSpecification<T> connectionSetupSpecification, AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        return this.configureConnection(connection, connectionSetupSpecification, CredentialBuilder.makeCredential(this.credentialProviderProvider, authenticationSpecification, identity));
    }
}
