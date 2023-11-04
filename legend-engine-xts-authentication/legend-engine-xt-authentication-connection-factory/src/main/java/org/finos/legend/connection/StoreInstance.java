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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.ConnectionSpecification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A StoreInstance represents a named instance of a Store.
 */
public final class StoreInstance
{
    private final String identifier;
    private final StoreSupport storeSupport;
    private final ConnectionSpecification connectionSpecification;
    private final Map<AuthenticationMechanism, AuthenticationMechanismConfiguration> authenticationMechanismConfigurationIndex;
    private final Map<Class<? extends AuthenticationConfiguration>, AuthenticationMechanism> authenticationMechanismIndex;

    private StoreInstance(String identifier, StoreSupport storeSupport, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations, ConnectionSpecification connectionSpecification)
    {
        this.identifier = Objects.requireNonNull(identifier, "Can't create store instance: identifier is missing");
        this.storeSupport = storeSupport;
        this.connectionSpecification = Objects.requireNonNull(connectionSpecification, "Connection specification is missing");

        Map<AuthenticationMechanism, AuthenticationMechanismConfiguration> authenticationMechanismConfigurationIndex = new LinkedHashMap<>();

        if (authenticationMechanismConfigurations.isEmpty())
        {
            for (AuthenticationMechanism authenticationMechanism : this.storeSupport.getAuthenticationMechanisms())
            {
                authenticationMechanismConfigurationIndex.put(authenticationMechanism, this.storeSupport.getAuthenticationMechanismConfiguration(authenticationMechanism));
            }
        }
        else
        {
            for (AuthenticationMechanismConfiguration authenticationMechanismConfiguration : authenticationMechanismConfigurations)
            {
                AuthenticationMechanism authenticationMechanism = authenticationMechanismConfiguration.getAuthenticationMechanism();
                if (authenticationMechanismConfigurationIndex.containsKey(authenticationMechanism))
                {
                    throw new RuntimeException(String.format("Found multiple configurations for authentication mechanism '%s'", authenticationMechanism.getLabel()));
                }
                AuthenticationMechanismConfiguration configFromStoreSupport = this.storeSupport.getAuthenticationMechanismConfiguration(authenticationMechanism);
                if (configFromStoreSupport == null)
                {
                    throw new RuntimeException(String.format("Authentication mechanism '%s' is not covered by store support '%s'. Supported mechanism(s):\n%s",
                            authenticationMechanism.getLabel(),
                            this.storeSupport.getIdentifier(),
                            ListIterate.collect(this.storeSupport.getAuthenticationMechanisms(), mechanism -> "- " + mechanism.getLabel()).makeString("\n")
                    ));
                }
                ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigTypesFromStoreSupport = configFromStoreSupport.getAuthenticationConfigurationTypes();
                List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes = Lists.mutable.empty();
                for (Class<? extends AuthenticationConfiguration> authenticationConfigurationType : authenticationMechanismConfiguration.getAuthenticationConfigurationTypes())
                {
                    if (!authenticationConfigTypesFromStoreSupport.contains(authenticationConfigurationType))
                    {
                        throw new RuntimeException(String.format("Authentication configuration type '%s' is not covered by store support '%s' for authentication mechanism '%s'. Supported configuration type(s):\n%s",
                                authenticationConfigurationType.getSimpleName(),
                                this.storeSupport.getIdentifier(),
                                authenticationMechanism.getLabel(),
                                authenticationConfigTypesFromStoreSupport.collect(type -> "- " + type.getSimpleName()).makeString("\n")
                        ));
                    }
                    else
                    {
                        authenticationConfigurationTypes.add(authenticationConfigurationType);
                    }
                }
                authenticationMechanismConfigurationIndex.put(authenticationMechanism, new AuthenticationMechanismConfiguration.Builder(authenticationMechanism)
                        // NOTE: if no configuration type is specified, it means the store instance supports all configuration types configured for that mechanism in the store support
                        .withAuthenticationConfigurationTypes(!authenticationConfigurationTypes.isEmpty() ? authenticationConfigurationTypes : authenticationConfigTypesFromStoreSupport.toList())
                        .withDefaultAuthenticationConfigurationGenerator(authenticationMechanismConfiguration.getDefaultAuthenticationConfigurationGenerator() != null ? authenticationMechanismConfiguration.getDefaultAuthenticationConfigurationGenerator() : configFromStoreSupport.getDefaultAuthenticationConfigurationGenerator())
                        .build());

            }

        }

        this.authenticationMechanismConfigurationIndex = authenticationMechanismConfigurationIndex;
        Map<Class<? extends AuthenticationConfiguration>, AuthenticationMechanism> authenticationMechanismIndex = new LinkedHashMap<>();
        authenticationMechanismConfigurationIndex.forEach((authenticationMechanism, authenticationMechanismConfiguration) ->
        {
            if (authenticationMechanismConfiguration.getAuthenticationConfigurationTypes().isEmpty())
            {
                throw new RuntimeException(String.format("No authentication configuration type is associated with authentication mechanism '%s'", authenticationMechanism.getLabel()));
            }
            authenticationMechanismConfiguration.getAuthenticationConfigurationTypes().forEach(configurationType ->
            {
                authenticationMechanismIndex.put(configurationType, authenticationMechanism);
            });
        });
        this.authenticationMechanismIndex = authenticationMechanismIndex;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public StoreSupport getStoreSupport()
    {
        return storeSupport;
    }

    public List<AuthenticationMechanism> getAuthenticationMechanisms()
    {
        return new ArrayList<>(this.authenticationMechanismConfigurationIndex.keySet());
    }

    public List<Class<? extends AuthenticationConfiguration>> getAuthenticationConfigurationTypes()
    {
        return new ArrayList<>(this.authenticationMechanismIndex.keySet());
    }

    public AuthenticationMechanism getAuthenticationMechanism(Class<? extends AuthenticationConfiguration> authenticationConfigurationType)
    {
        return this.authenticationMechanismIndex.get(authenticationConfigurationType);
    }

    public ConnectionSpecification getConnectionSpecification()
    {
        return connectionSpecification;
    }

    public AuthenticationMechanismConfiguration getAuthenticationMechanismConfiguration(AuthenticationMechanism authenticationMechanism)
    {
        return authenticationMechanismConfigurationIndex.get(authenticationMechanism);
    }

    public <T extends ConnectionSpecification> T getConnectionSpecification(Class<T> clazz)
    {
        if (!this.connectionSpecification.getClass().equals(clazz))
        {
            throw new RuntimeException(String.format("Can't get connection specification of type '%s' for store '%s'", clazz.getSimpleName(), this.identifier));
        }
        return (T) this.connectionSpecification;
    }

    public static class Builder
    {
        private final LegendEnvironment environment;
        private String identifier;
        private String storeSupportIdentifier;
        private final List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations = Lists.mutable.empty();
        private ConnectionSpecification connectionSpecification;

        public Builder(LegendEnvironment environment)
        {
            this.environment = environment;
        }

        public Builder withIdentifier(String identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public Builder withStoreSupportIdentifier(String storeSupportIdentifier)
        {
            this.storeSupportIdentifier = storeSupportIdentifier;
            return this;
        }

        public Builder withAuthenticationMechanismConfiguration(AuthenticationMechanismConfiguration authenticationMechanismConfiguration)
        {
            this.authenticationMechanismConfigurations.add(authenticationMechanismConfiguration);
            return this;
        }

        public Builder withAuthenticationMechanismConfigurations(List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
        {
            this.authenticationMechanismConfigurations.addAll(authenticationMechanismConfigurations);
            return this;
        }

        public Builder withAuthenticationMechanismConfigurations(AuthenticationMechanismConfiguration... authenticationMechanismConfigurations)
        {
            this.authenticationMechanismConfigurations.addAll(Lists.mutable.of(authenticationMechanismConfigurations));
            return this;
        }

        public Builder withConnectionSpecification(ConnectionSpecification connectionSpecification)
        {
            this.connectionSpecification = connectionSpecification;
            return this;
        }

        public StoreInstance build()
        {
            return new StoreInstance(
                    this.identifier,
                    this.environment.findStoreSupport(Objects.requireNonNull(this.storeSupportIdentifier, "Store support identifier is missing")),
                    this.authenticationMechanismConfigurations,
                    this.connectionSpecification
            );
        }
    }
}
