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

package org.finos.legend.authentication.intermediationrule.impl;

import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;

public class ApiKeyFromVaultRule extends IntermediationRule<ApiKeyAuthenticationSpecification, Credential, ApiTokenCredential>
{
    public ApiKeyFromVaultRule(CredentialVaultProvider credentialVaultProvider)
    {
        super(credentialVaultProvider);
    }

    @Override
    public ApiTokenCredential makeCredential(ApiKeyAuthenticationSpecification specification, Credential credential) throws Exception
    {
        CredentialVaultSecret credentialVaultSecret = specification.value;
        String apiKey = super.lookupSecret(credentialVaultSecret);
        return new ApiTokenCredential(apiKey);
    }
}
