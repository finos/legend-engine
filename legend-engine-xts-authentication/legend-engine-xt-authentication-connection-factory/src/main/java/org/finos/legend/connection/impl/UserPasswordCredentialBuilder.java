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

package org.finos.legend.connection.impl;

import org.finos.legend.connection.CredentialBuilder;
import org.finos.legend.connection.EnvironmentConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

public class UserPasswordCredentialBuilder extends CredentialBuilder<UserPasswordAuthenticationSpecification, Credential, PlaintextUserPasswordCredential>
{
    @Override
    public PlaintextUserPasswordCredential makeCredential(Identity identity, UserPasswordAuthenticationSpecification specification, Credential credential, EnvironmentConfiguration configuration) throws Exception
    {

        String password = configuration.lookupVaultSecret(specification.password, identity);
        return new PlaintextUserPasswordCredential(specification.username, password);
    }
}
