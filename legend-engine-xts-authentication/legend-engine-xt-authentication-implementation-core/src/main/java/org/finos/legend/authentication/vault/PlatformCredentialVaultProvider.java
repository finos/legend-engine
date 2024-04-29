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

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

public class PlatformCredentialVaultProvider
{
     private MutableMap<Class<? extends CredentialVaultSecret>, CredentialVault> vaultsByType = Maps.mutable.empty();

     private ImmutableList<CredentialVault> vaults;

     public PlatformCredentialVaultProvider(ImmutableList<CredentialVault> vaults)
     {
          this.vaults = vaults;
          for (CredentialVault vault : vaults)
          {
               this.vaultsByType.put(vault.getSecretType(), vault);
          }
     }

     public ImmutableList<CredentialVault> getVaults()
     {
          return vaults;
     }

     public CredentialVault getVault(CredentialVaultSecret credentialVaultSecret) throws Exception
     {
          Class<? extends CredentialVaultSecret> secretClass = credentialVaultSecret.getClass();
          if (!this.vaultsByType.containsKey(secretClass))
          {
               throw new RuntimeException(String.format("CredentialVault for secret of type '%s' has not been registered in the system", secretClass));
          }
          return this.vaultsByType.get(secretClass);
     }

     public static Builder builder()
     {
          return new Builder();
     }

     public static class Builder
     {
          private MutableList<CredentialVault> vaults = Lists.mutable.empty();

          public Builder with(CredentialVault credentialVault)
          {
               this.vaults.add(credentialVault);
               return this;
          }

          public PlatformCredentialVaultProvider build()
          {
               return new PlatformCredentialVaultProvider(this.vaults.toImmutable());
          }
     }
}
