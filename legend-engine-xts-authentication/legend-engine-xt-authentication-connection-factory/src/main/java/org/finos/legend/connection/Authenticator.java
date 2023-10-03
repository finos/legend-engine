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
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;
import java.util.Optional;

public class Authenticator
{
    private final StoreInstance storeInstance;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final Class<? extends Credential> sourceCredentialType;
    private final ImmutableList<CredentialBuilder> credentialBuilders;
    private final ConnectionBuilder connectionBuilder;

    public Authenticator(StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration, Class<? extends Credential> sourceCredentialType, List<CredentialBuilder> credentialBuilders, ConnectionBuilder connectionBuilder)
    {
        this.storeInstance = storeInstance;
        this.authenticationConfiguration = authenticationConfiguration;
        this.sourceCredentialType = sourceCredentialType;
        this.credentialBuilders = Lists.immutable.withAll(credentialBuilders);
        this.connectionBuilder = connectionBuilder;
    }

    public Credential makeCredential(Identity identity, LegendEnvironment environment) throws Exception
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
            credential = credentialBuilder.makeCredential(identity, this.authenticationConfiguration, credential, environment);
        }
        return credential;
    }

    public ConnectionBuilder getConnectionBuilder()
    {
        return connectionBuilder;
    }

    public StoreInstance getStoreInstance()
    {
        return storeInstance;
    }

    public Class<? extends Credential> getSourceCredentialType()
    {
        return sourceCredentialType;
    }

    public ImmutableList<CredentialBuilder> getCredentialBuilders()
    {
        return credentialBuilders;
    }
}
