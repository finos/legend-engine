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

import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Optional;

public abstract class CredentialExtractor<SPEC extends AuthenticationConfiguration, CRED extends Credential> extends CredentialBuilder<SPEC, CRED, CRED>
{

    @Override
    public Class<? extends Credential> getOutputCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[1];
    }

    @Override
    public CRED makeCredential(Identity identity, SPEC spec, CRED cred, EnvironmentConfiguration configuration) throws Exception
    {
        Optional<CRED> credentialOptional = identity.getCredential((Class<CRED>) this.getOutputCredentialType());
        if (!credentialOptional.isPresent())
        {
            throw new RuntimeException(String.format("Can't extract credential of type '%s' from the specified identity", this.getOutputCredentialType().getSimpleName()));
        }
        return credentialOptional.get();
    }
}
