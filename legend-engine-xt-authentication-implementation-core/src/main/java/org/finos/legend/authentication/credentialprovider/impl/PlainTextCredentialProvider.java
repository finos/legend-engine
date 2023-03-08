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
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PlaintextAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextCredential;

public class PlainTextCredentialProvider extends CredentialProvider<PlaintextAuthenticationSpecification, PlaintextCredential>
{
    private CredentialVaultProvider credentialVaultProvider;

    public PlainTextCredentialProvider()
    {

    }

    public PlainTextCredentialProvider(CredentialVaultProvider credentialVaultProvider)
    {
        this.credentialVaultProvider = credentialVaultProvider;
    }

    @Override
    public PlaintextCredential makeCredential(PlaintextAuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        CredentialVault credentialVault = this.credentialVaultProvider.getVault(authenticationSpecification.plaintextValue);
        String rawSecret = credentialVault.lookupSecret(authenticationSpecification.plaintextValue, identity);
        return new PlaintextCredential(rawSecret);
    }
}
