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

package org.finos.legend.connection.experimental;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.List;

public class ConnectionAuthentication
{
    private final Identity identity;
    private final AuthenticationSpecification authenticationSpecification;

    private final List<CredentialBuilder> credentialBuilders;
    private final ConnectionBuilder connectionBuilder;

    public ConnectionAuthentication(Identity identity, AuthenticationSpecification authenticationSpecification, List<CredentialBuilder> credentialBuilders, ConnectionBuilder connectionBuilder)
    {
        this.identity = identity;
        this.authenticationSpecification = authenticationSpecification;
        this.credentialBuilders = credentialBuilders;
        this.connectionBuilder = connectionBuilder;
    }

    public Credential makeCredential(EnvironmentConfiguration configuration) throws Exception
    {
        Credential credential = null;
        for (CredentialBuilder credentialBuilder : this.credentialBuilders)
        {
            credential = credentialBuilder.makeCredential(this.identity, this.authenticationSpecification, credential, configuration);
        }
        return credential;
    }

    public ConnectionBuilder getConnectionBuilder()
    {
        return connectionBuilder;
    }
}
