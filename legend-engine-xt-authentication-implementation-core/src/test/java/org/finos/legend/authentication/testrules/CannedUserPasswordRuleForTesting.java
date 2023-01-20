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

package org.finos.legend.authentication.testrules;

import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

public class CannedUserPasswordRuleForTesting extends IntermediationRule<UserPasswordAuthenticationSpecification, AnonymousCredential, PlaintextUserPasswordCredential>
{
    private String user;
    private String password;

    public CannedUserPasswordRuleForTesting(CredentialVaultProvider vaultProvider)
    {
        super(vaultProvider);
        this.user = "hello";
        this.password = "world";
    }

    public CannedUserPasswordRuleForTesting(String user, String password)
    {
        super(null);
        this.user = user;
        this.password = password;
    }

    @Override
    public PlaintextUserPasswordCredential makeCredential(UserPasswordAuthenticationSpecification authenticationSpecification, AnonymousCredential credential) throws Exception
    {
        return new PlaintextUserPasswordCredential(this.user, this.password);
    }
}