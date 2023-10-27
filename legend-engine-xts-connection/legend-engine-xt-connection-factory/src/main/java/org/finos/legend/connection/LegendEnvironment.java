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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.connection.impl.CoreAuthenticationMechanismType;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;
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
    protected final ImmutableList<ConnectionExtension> connectionExtensions;
    protected final ImmutableMap<String, AuthenticationMechanismType> authenticationMechanismsTypesIndex;
    protected final ImmutableMap<String, DatabaseType> databaseTypesIndex;
    protected final ImmutableMap<String, Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypesIndex;
    protected final ImmutableMap<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex;
    protected final ImmutableMap<String, DatabaseSupport> databaseSupportsIndex;

    protected LegendEnvironment(List<CredentialVault> vaults, List<DatabaseSupport> databaseSupports)
    {
        this.vaults = Lists.immutable.withAll(vaults);
        Map<Class<? extends CredentialVaultSecret>, CredentialVault<? extends CredentialVaultSecret>> vaultsIndex = new LinkedHashMap<>();
        for (CredentialVault<? extends CredentialVaultSecret> vault : vaults)
        {
            vaultsIndex.put(vault.getSecretType(), vault);
        }
        this.vaultsIndex = Maps.immutable.withAll(vaultsIndex);

        MutableList<ConnectionExtension> connectionExtensions = Lists.mutable.withAll(ServiceLoader.load(ConnectionExtension.class));
        this.connectionExtensions = connectionExtensions.toImmutable();

        // load authentication mechanism types
        List<AuthenticationMechanismType> authenticationMechanismTypes = Lists.mutable.of(CoreAuthenticationMechanismType.values());
        authenticationMechanismTypes.addAll(connectionExtensions.flatCollect(ConnectionExtension::getExtraAuthenticationMechanismTypes));
        Map<String, AuthenticationMechanismType> authenticationMechanismsTypesIndex = new LinkedHashMap<>();
        authenticationMechanismTypes.forEach(authenticationMechanism ->
        {
            if (authenticationMechanismsTypesIndex.containsKey(authenticationMechanism.getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple authentication mechanisms with label '%s'", authenticationMechanism.getIdentifier()));
            }
            authenticationMechanismsTypesIndex.put(authenticationMechanism.getIdentifier(), authenticationMechanism);
        });
        this.authenticationMechanismsTypesIndex = Maps.immutable.withAll(authenticationMechanismsTypesIndex);

        // load database types
        List<DatabaseType> databaseTypes = connectionExtensions.flatCollect(ConnectionExtension::getExtraDatabaseTypes);
        Map<String, DatabaseType> databaseTypesIndex = new LinkedHashMap<>();
        databaseTypes.forEach(databaseType ->
        {
            if (databaseTypesIndex.containsKey(databaseType.getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple authentication mechanisms with label '%s'", databaseType.getIdentifier()));
            }
            databaseTypesIndex.put(databaseType.getIdentifier(), databaseType);
        });
        this.databaseTypesIndex = Maps.immutable.withAll(databaseTypesIndex);

        // load authentication configuration types
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

        // load database supports
        Map<String, DatabaseSupport> databaseSupportsIndex = new LinkedHashMap<>();
        databaseSupports.forEach(databaseSupport ->
        {
            if (databaseSupportsIndex.containsKey(databaseSupport.getDatabaseType().getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple database supports for type '%s'", databaseSupport.getDatabaseType().getIdentifier()));
            }
            databaseSupportsIndex.put(databaseSupport.getDatabaseType().getIdentifier(), databaseSupport);
        });
        this.databaseSupportsIndex = Maps.immutable.withAll(databaseSupportsIndex);
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

    public AuthenticationMechanismType getAuthenticationMechanism(String identifier)
    {
        return Objects.requireNonNull(this.authenticationMechanismsTypesIndex.get(identifier), String.format("Can't find authentication mechanism with label '%s'", identifier));
    }

    public DatabaseType getDatabaseType(String identifier)
    {
        return Objects.requireNonNull(this.databaseTypesIndex.get(identifier), String.format("Can't find database type with identifier '%s'", identifier));
    }

    public Class<? extends AuthenticationConfiguration> getAuthenticationConfigurationType(String identifier)
    {
        return Objects.requireNonNull(this.authenticationConfigurationTypesIndex.get(identifier), String.format("Can't find authentication configuration type with identifier '%s'", identifier));
    }

    public DatabaseSupport getDatabaseSupport(DatabaseType databaseType)
    {
        return Objects.requireNonNull(this.databaseSupportsIndex.get(databaseType.getIdentifier()), String.format("Can't find database support with database type '%s'", databaseType.getIdentifier()));
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final List<CredentialVault> vaults = Lists.mutable.empty();
        private final List<DatabaseSupport> databaseSupports = Lists.mutable.empty();

        private Builder()
        {
        }

        public Builder vaults(List<CredentialVault> vaults)
        {
            this.vaults.addAll(vaults);
            return this;
        }

        public Builder vaults(CredentialVault... vaults)
        {
            this.vaults.addAll(Lists.mutable.with(vaults));
            return this;
        }

        public Builder vault(CredentialVault vault)
        {
            this.vaults.add(vault);
            return this;
        }

        public Builder databaseSupports(List<DatabaseSupport> databaseSupports)
        {
            this.databaseSupports.addAll(databaseSupports);
            return this;
        }

        public Builder databaseSupports(DatabaseSupport... databaseSupports)
        {
            this.databaseSupports.addAll(Lists.mutable.with(databaseSupports));
            return this;
        }

        public Builder databaseSupport(DatabaseSupport databaseSupport)
        {
            this.databaseSupports.add(databaseSupport);
            return this;
        }

        public LegendEnvironment build()
        {
            return new LegendEnvironment(this.vaults, this.databaseSupports);
        }
    }
}
