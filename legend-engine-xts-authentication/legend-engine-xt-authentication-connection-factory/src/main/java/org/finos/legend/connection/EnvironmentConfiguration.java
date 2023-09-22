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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.identity.Identity;

import javax.ws.rs.core.Link;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This is meant to the place we package common configs, such as vaults,
 * that can be passed to various parts of engine, authentication, connection factory, etc.
 */
public class EnvironmentConfiguration
{
    private final ImmutableList<CredentialVault<? extends CredentialVaultSecret>> vaults;
    private final ImmutableMap<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex;
    private final Map<String, StoreSupport> storeSupportsIndex;

    private final Map<String, AuthenticationMechanism> authenticationMechanismsIndex;

    private EnvironmentConfiguration(List<CredentialVault<? extends CredentialVaultSecret>> vaults, Map<String, StoreSupport> storeSupportsIndex, Map<String, AuthenticationMechanism> authenticationMechanismsIndex)
    {
        this.vaults = Lists.immutable.withAll(vaults);
        MutableMap<Class<? extends CredentialVaultSecret>, CredentialVault<?>> vaultsIndex = Maps.mutable.empty();
        for (CredentialVault<? extends CredentialVaultSecret> vault : vaults)
        {
            vaultsIndex.put(vault.getSecretType(), vault);
        }
        this.vaultsIndex = Maps.immutable.withAll(vaultsIndex);
        this.storeSupportsIndex = storeSupportsIndex;
        this.authenticationMechanismsIndex = authenticationMechanismsIndex;
    }

    public StoreSupport findStoreSupport(String identifier)
    {
        return Objects.requireNonNull(this.storeSupportsIndex.get(identifier), String.format("Can't find store support with identifier '%s'", identifier));
    }

    public String lookupVaultSecret(CredentialVaultSecret credentialVaultSecret, Identity identity) throws Exception
    {
        Class<? extends CredentialVaultSecret> secretClass = credentialVaultSecret.getClass();
        if (!this.vaultsIndex.containsKey(secretClass))
        {
            throw new RuntimeException(String.format("CredentialVault for secret of type '%s' has not been registered in the system", secretClass.getSimpleName()));
        }
        CredentialVault vault = this.vaultsIndex.get(secretClass);
        return vault.lookupSecret(credentialVaultSecret, identity);
    }

    public AuthenticationMechanism findAuthenticationMechanismForConfiguration(AuthenticationConfiguration configuration)
    {
        return this.authenticationMechanismsIndex.get(configuration.getClass().getSimpleName());
    }

    public static class Builder
    {
        private final List<CredentialVault<? extends CredentialVaultSecret>> vaults = Lists.mutable.empty();
        private final Map<String, StoreSupport> storeSupportsIndex = new LinkedHashMap<>();
        private AuthenticationMechanismProvider authenticationMechanismProvider;
        private final Set<AuthenticationMechanism> authenticationMechanisms = new LinkedHashSet<>();

        public Builder()
        {

        }

        public Builder withVaults(List<CredentialVault<? extends CredentialVaultSecret>> vaults)
        {
            this.vaults.addAll(vaults);
            return this;
        }

        public Builder withVault(CredentialVault<? extends CredentialVaultSecret> vault)
        {
            this.vaults.add(vault);
            return this;
        }

        public Builder withStoreSupports(List<StoreSupport> storeSupports)
        {
            storeSupports.forEach(this::registerStoreSupport);
            return this;
        }

        public Builder withStoreSupport(StoreSupport storeSupport)
        {
            this.registerStoreSupport(storeSupport);
            return this;
        }

        private void registerStoreSupport(StoreSupport storeSupport)
        {
            if (this.storeSupportsIndex.containsKey(storeSupport.getIdentifier()))
            {
                throw new RuntimeException(String.format("Can't register store support: found multiple store supports with identifier '%s'", storeSupport.getIdentifier()));
            }
            this.storeSupportsIndex.put(storeSupport.getIdentifier(), storeSupport);
        }

        public Builder withAuthenticationMechanismProvider(AuthenticationMechanismProvider authenticationMechanismProvider)
        {
            this.authenticationMechanismProvider = authenticationMechanismProvider;
            return this;
        }

        public Builder withAuthenticationMechanisms(List<AuthenticationMechanism> authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(authenticationMechanisms);
            return this;
        }

        public Builder withAuthenticationMechanism(AuthenticationMechanism authenticationMechanism)
        {
            this.authenticationMechanisms.add(authenticationMechanism);
            return this;
        }

        public EnvironmentConfiguration build()
        {
            List<AuthenticationMechanism> authenticationMechanisms = this.authenticationMechanismProvider != null ? ListIterate.flatCollect(this.authenticationMechanismProvider.getLoaders(), AuthenticationMechanismLoader::getMechanisms) : Lists.mutable.empty();
            authenticationMechanisms.addAll(this.authenticationMechanisms);
            Map<String, AuthenticationMechanism> authenticationMechanismsIndex = new LinkedHashMap<>();
            authenticationMechanisms.forEach(mechanism ->
            {
                String key = mechanism.getAuthenticationConfigurationType().getSimpleName();
                if (authenticationMechanismsIndex.containsKey(key))
                {
                    throw new IllegalStateException(String.format("Can't build environment configuration: found multiple authentication mechanisms (%s, %s) associated with the same configuration type '%s'",
                            authenticationMechanismsIndex.get(key).getLabel(),
                            mechanism.getLabel(),
                            key
                    ));
                }
                AuthenticationConfiguration configuration = mechanism.generateConfiguration();
                if (configuration != null && !configuration.getClass().equals(mechanism.getAuthenticationConfigurationType()))
                {
                    throw new IllegalStateException(String.format("Can't build environment configuration: authentication mechanism '%s' is misconfigured, its associated configuration type is '%s' and its generated configuration type is '%s'",
                            mechanism.getLabel(),
                            mechanism.getAuthenticationConfigurationType().getSimpleName(),
                            configuration.getClass().getSimpleName()
                    ));
                }
                authenticationMechanismsIndex.put(key, mechanism);
            });

            return new EnvironmentConfiguration(this.vaults, this.storeSupportsIndex, authenticationMechanismsIndex);
        }
    }
}
