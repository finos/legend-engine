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

package org.finos.legend.authentication.credentialprovider.impl;

import org.finos.legend.authentication.credentialprovider.CredentialProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

public class UserPasswordCredentialProvider extends CredentialProvider<UserPasswordAuthenticationSpecification, PlaintextUserPasswordCredential>
{
    @Override
    public PlaintextUserPasswordCredential makeCredential(UserPasswordAuthenticationSpecification AuthenticationSpecification, Identity identity) throws Exception
    {

        return (PlaintextUserPasswordCredential) super.makeCredential(AuthenticationSpecification, identity, PlaintextUserPasswordCredential.class);

    }
}
