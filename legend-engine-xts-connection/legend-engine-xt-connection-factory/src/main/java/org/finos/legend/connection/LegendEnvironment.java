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

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.engine.protocol.pure.v1.connection.AuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * This is the runtime instance of configuration for Legend Engine, the place we package common configs,
 * such as vaults, that can be passed to various parts of engine, authentication, connection factory, etc.
 */
public class LegendEnvironment
{
    protected final ImmutableList<CredentialVault> vaults;
    protected final ImmutableMap<String, AuthenticationMechanism> authenticationMechanismsIndex;
    protected final ImmutableMap<String, Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypesIndex;
    protected final ImmutableMap<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex;
    protected final ImmutableMap<String, StoreSupport> storeSupportsIndex;

    protected LegendEnvironment(List<CredentialVault> vaults, List<StoreSupport> storeSupports)
    {
        this.vaults = Lists.immutable.withAll(vaults);
        Map<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex = new LinkedHashMap<>();
        for (CredentialVault<? extends CredentialVaultSecret> vault : vaults)
        {
            vaultsIndex.put(vault.getSecretType(), vault);
        }
        this.vaultsIndex = Maps.immutable.withAll(vaultsIndex);

        List<AuthenticationMechanism> authenticationMechanisms = Lists.mutable.empty();
        for (AuthenticationMechanismLoader extension : ServiceLoader.load(AuthenticationMechanismLoader.class))
        {
            authenticationMechanisms.addAll(extension.load());
        }
        Map<String, AuthenticationMechanism> authenticationMechanismsIndex = new LinkedHashMap<>();
        authenticationMechanisms.forEach(authenticationMechanism ->
        {
            if (authenticationMechanismsIndex.containsKey(authenticationMechanism.getLabel()))
            {
                throw new RuntimeException(String.format("Found multiple authentication mechanisms with label '%s'", authenticationMechanism.getLabel()));
            }
            authenticationMechanismsIndex.put(authenticationMechanism.getLabel(), authenticationMechanism);
        });
        this.authenticationMechanismsIndex = Maps.immutable.withAll(authenticationMechanismsIndex);

        Map<String, Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypesIndex = new LinkedHashMap<>();
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                {
                    info.getSubTypes().forEach(subType ->
                    {
                        if (AuthenticationConfiguration.class.isAssignableFrom(subType.getOne()))
                        {
                            if (authenticationConfigurationTypesIndex.containsKey(subType.getTwo()))
                            {
                                throw new RuntimeException(String.format("Found multiple authentication configuration types with identifier '%s'", subType.getTwo()));
                            }
                            authenticationConfigurationTypesIndex.put(subType.getTwo(), (Class<? extends AuthenticationConfiguration>) subType.getOne());
                        }
                    });
                }));
        this.authenticationConfigurationTypesIndex = Maps.immutable.withAll(authenticationConfigurationTypesIndex);

        Map<String, StoreSupport> storeSupportsIndex = new LinkedHashMap<>();
        storeSupports.forEach(storeSupport ->
        {
            if (storeSupportsIndex.containsKey(storeSupport.getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple store supports with identifier '%s'", storeSupport.getIdentifier()));
            }
            storeSupportsIndex.put(storeSupport.getIdentifier(), storeSupport);
        });
        this.storeSupportsIndex = Maps.immutable.withAll(storeSupportsIndex);
    }

    public String lookupVaultSecret(CredentialVaultSecret credentialVaultSecret, Identity identity) throws Exception
    {
        Class<? extends CredentialVaultSecret> secretClass = credentialVaultSecret.getClass();
        if (!this.vaultsIndex.containsKey(secretClass))
        {
            throw new RuntimeException(String.format("Can't find secret: credential vault for secret of type '%s' has not been registered", secretClass.getSimpleName()));
        }
        CredentialVault vault = this.vaultsIndex.get(secretClass);
        return vault.lookupSecret(credentialVaultSecret, identity);
    }

    public AuthenticationMechanism getAuthenticationMechanism(String label)
    {
        return Objects.requireNonNull(this.authenticationMechanismsIndex.get(label), String.format("Can't find authentication mechanism with label '%s'", label));
    }

    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType(String type)
    {
        return Objects.requireNonNull(this.authenticationConfigurationTypesIndex.get(type), String.format("Can't find authentication configuration type with identifier '%s'", type));
    }

    public StoreSupport getStoreSupport(String identifier)
    {
        return Objects.requireNonNull(this.storeSupportsIndex.get(identifier), String.format("Can't find store support with identifier '%s'", identifier));
    }

    public static class Builder
    {
        private final List<CredentialVault> vaults = Lists.mutable.empty();
        private final List<StoreSupport> storeSupports = Lists.mutable.empty();

        public Builder()
        {
        }

        public Builder withVaults(List<CredentialVault> vaults)
        {
            this.vaults.addAll(vaults);
            return this;
        }

        public Builder withVaults(CredentialVault... vaults)
        {
            this.vaults.addAll(Lists.mutable.with(vaults));
            return this;
        }

        public Builder withVault(CredentialVault vault)
        {
            this.vaults.add(vault);
            return this;
        }

        public Builder withStoreSupports(List<StoreSupport> storeSupports)
        {
            this.storeSupports.addAll(storeSupports);
            return this;
        }

        public Builder withStoreSupports(StoreSupport... storeSupports)
        {
            this.storeSupports.addAll(Lists.mutable.with(storeSupports));
            return this;
        }

        public Builder withStoreSupport(StoreSupport storeSupport)
        {
            this.storeSupports.add(storeSupport);
            return this;
        }

        public LegendEnvironment build()
        {
            return new LegendEnvironment(this.vaults, this.storeSupports);
        }
    }
}
