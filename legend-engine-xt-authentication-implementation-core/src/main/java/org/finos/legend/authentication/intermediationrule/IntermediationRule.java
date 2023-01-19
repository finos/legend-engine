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

package org.finos.legend.authentication.intermediationrule;

import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/*
    An IntermediationRule implements business logic to acquire a Credential.

    It accepts as inputs :
    - AuthenticationSpecification that provides configuration data that can be used during credential creation.
    - Identity of the current runtime user. The user's credentials can be used to perform further authentication with other systems if needed.

 */
public abstract class IntermediationRule<SPEC extends AuthenticationSpecification, INPUT_CRED extends Credential, OUTPUT_CRED extends Credential>
{
    protected CredentialVaultProvider credentialVaultProvider;

    public IntermediationRule(CredentialVaultProvider credentialVaultProvider)
    {
        this.credentialVaultProvider = credentialVaultProvider;
    }

    public abstract OUTPUT_CRED makeCredential(SPEC spec, INPUT_CRED cred) throws Exception;

    public Class<? extends AuthenticationSpecification> getAuthenticationSpecificationType()
    {
        return (Class<? extends AuthenticationSpecification>) actualTypeArguments()[0];
    }

    public Class<? extends Credential> getInputCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[1];
    }

    public Class<? extends Credential> getOutputCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[2];
    }

    private Type[] actualTypeArguments()
    {
        Type genericSuperClass = this.getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
        return parameterizedType.getActualTypeArguments();
    }

    public boolean consumesAuthenticationSpecification(Class<? extends AuthenticationSpecification> authenticationSpecificationType)
    {
        return this.getAuthenticationSpecificationType().equals(authenticationSpecificationType);
    }

    public boolean consumesInputCredential(Class<? extends Credential> inputCredentialType)
    {
        if (this.consumesAnyCredential())
        {
            return true;
        }
        return this.getInputCredentialType().equals(inputCredentialType);
    }

    public boolean consumesInputCredentials(ImmutableSet<? extends Class<? extends Credential>> inputCredentialTypes)
    {
        if (this.consumesAnyCredential())
        {
            return true;
        }
        return inputCredentialTypes.contains(this.getInputCredentialType());
    }

    public boolean producesOutputCredential(Class<? extends Credential> outputCredentialType)
    {
        return this.getOutputCredentialType().equals(outputCredentialType);
    }

    public boolean consumesAnyCredential()
    {
        return this.getInputCredentialType().equals(Credential.class);
    }

    protected String lookupSecret(CredentialVaultSecret credentialVaultSecret, Identity identity) throws Exception
    {
        CredentialVault vault = this.credentialVaultProvider.getVault(credentialVaultSecret);
        return vault.lookupSecret(credentialVaultSecret, identity);
    }

    protected String lookupSecret(CredentialVaultSecret credentialVaultSecret) throws Exception
    {
        CredentialVault vault = this.credentialVaultProvider.getVault(credentialVaultSecret);
        return vault.lookupSecret(credentialVaultSecret, null);
    }
}
