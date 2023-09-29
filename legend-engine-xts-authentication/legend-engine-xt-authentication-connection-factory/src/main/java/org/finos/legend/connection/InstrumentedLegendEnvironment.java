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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

import java.util.List;
import java.util.Map;

/**
 * This is the instrumented version of {@link LegendEnvironment} which is used for testing.
 */
public class InstrumentedLegendEnvironment extends LegendEnvironment
{
    protected final MutableList<CredentialVault> vaults;
    protected final MutableMap<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex;
    protected final MutableMap<String, StoreSupport> storeSupportsIndex;

    protected final MutableMap<String, AuthenticationMechanism> authenticationMechanismsIndex;

    protected InstrumentedLegendEnvironment(List<CredentialVault> vaults, Map<String, StoreSupport> storeSupportsIndex, Map<String, AuthenticationMechanism> authenticationMechanismsIndex)
    {
        super(vaults, storeSupportsIndex, authenticationMechanismsIndex);
        this.vaults = super.vaults.toList();
        this.vaultsIndex = super.vaultsIndex.toMap();
        this.storeSupportsIndex = super.storeSupportsIndex.toMap();
        this.authenticationMechanismsIndex = super.authenticationMechanismsIndex.toMap();
    }

    public void injectVault(CredentialVault vault)
    {
        if (this.vaultsIndex.containsKey(vault.getSecretType()))
        {
            throw new RuntimeException(String.format("Can't register credential vault: found multiple vaults with secret type '%s'", vault.getSecretType().getSimpleName()));
        }
        this.vaultsIndex.put(vault.getSecretType(), vault);
        this.vaults.add(vault);
    }

    public void injectStoreSupport(StoreSupport storeSupport)
    {
        if (this.storeSupportsIndex.containsKey(storeSupport.getIdentifier()))
        {
            throw new RuntimeException(String.format("Can't register store support: found multiple store supports with identifier '%s'", storeSupport.getIdentifier()));
        }
        this.storeSupportsIndex.put(storeSupport.getIdentifier(), storeSupport);
    }
}
