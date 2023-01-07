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

package org.finos.legend.authentication.vault;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

public class CredentialVaultProvider
{
     private MutableMap<Class<? extends CredentialVaultSecret>, CredentialVault> vaults = Maps.mutable.empty();

     public CredentialVaultProvider()
     {
     }

     public void register(CredentialVault credentialVault)
     {
          this.vaults.put(credentialVault.getSecretType(), credentialVault);
     }

     public CredentialVault getVault(CredentialVaultSecret credentialVaultSecret) throws Exception
     {
          Class<? extends CredentialVaultSecret> secretClass = credentialVaultSecret.getClass();
          if (!this.vaults.containsKey(secretClass))
          {
               throw new RuntimeException(String.format("CredentialVault for secret of type '%s' has not been registered in the system", secretClass));
          }
          return this.vaults.get(secretClass);
     }

     public static Builder builder()
     {
          return new Builder();
     }

     public static class Builder
     {
          private CredentialVaultProvider credentialVaultProvider = new CredentialVaultProvider();

          public Builder with(CredentialVault credentialVault)
          {
               credentialVaultProvider.register(credentialVault);
               return this;

          }

          public CredentialVaultProvider build()
          {
               return credentialVaultProvider;
          }
     }
}
