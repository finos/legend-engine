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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;
import java.util.Optional;

public class Authenticator<CRED extends Credential>
{
    private final StoreInstance storeInstance;
    private final AuthenticationMechanism authenticationMechanism;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final Class<? extends Credential> sourceCredentialType;
    private final Class<? extends Credential> targetCredentialType;
    private final ImmutableList<CredentialBuilder> credentialBuilders;
    private final ConnectionBuilder connectionBuilder;
    private final LegendEnvironment environment;

    public Authenticator(StoreInstance storeInstance, AuthenticationMechanism authenticationMechanism, AuthenticationConfiguration authenticationConfiguration, Class<? extends Credential> sourceCredentialType, Class<? extends Credential> targetCredentialType, List<CredentialBuilder> credentialBuilders, ConnectionBuilder connectionBuilder, LegendEnvironment environment)
    {
        this.storeInstance = storeInstance;
        this.authenticationMechanism = authenticationMechanism;
        this.authenticationConfiguration = authenticationConfiguration;
        this.sourceCredentialType = sourceCredentialType;
        this.targetCredentialType = targetCredentialType;
        this.credentialBuilders = Lists.immutable.withAll(credentialBuilders);
        this.connectionBuilder = connectionBuilder;
        this.environment = environment;
    }

    public CRED makeCredential(Identity identity) throws Exception
    {
        Credential credential = null;
        // no need to resolve the source credential if the flow starts with generic `Credential` node
        if (!this.sourceCredentialType.equals(Credential.class))
        {
            Optional<Credential> credentialOptional = identity.getCredential((Class<Credential>) this.sourceCredentialType);
            if (!credentialOptional.isPresent())
            {
                throw new RuntimeException(String.format("Can't resolve source credential of type '%s' from the specified identity", this.sourceCredentialType.getSimpleName()));
            }
            else
            {
                credential = credentialOptional.get();
            }
        }
        for (CredentialBuilder credentialBuilder : this.credentialBuilders)
        {
            credential = credentialBuilder.makeCredential(identity, this.authenticationConfiguration, credential, this.environment);
        }
        if (!this.targetCredentialType.equals(credential.getClass()))
        {
            throw new RuntimeException(String.format("Generated credential type is expected to be '%s' (found: %s)", this.targetCredentialType.getSimpleName(), credential.getClass().getSimpleName()));
        }
        return (CRED) credential;
    }

    public AuthenticationMechanism getAuthenticationMechanism()
    {
        return authenticationMechanism;
    }

    public AuthenticationConfiguration getAuthenticationConfiguration()
    {
        return authenticationConfiguration;
    }

    public StoreInstance getStoreInstance()
    {
        return storeInstance;
    }

    public Class<? extends Credential> getSourceCredentialType()
    {
        return sourceCredentialType;
    }


    public Class<? extends Credential> getTargetCredentialType()
    {
        return targetCredentialType;
    }

    public ImmutableList<CredentialBuilder> getCredentialBuilders()
    {
        return credentialBuilders;
    }

    public ConnectionBuilder getConnectionBuilder()
    {
        return connectionBuilder;
    }
}
