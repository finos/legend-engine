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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.ConnectionSpecification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Connection
{
    private final String identifier;
    private final DatabaseSupport databaseSupport;
    private final ConnectionSpecification connectionSpecification;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final ImmutableMap<String, AuthenticationMechanism> authenticationMechanismsIndex;
    private final ImmutableList<AuthenticationMechanismType> authenticationMechanismTypes;
    private final ImmutableMap<Class<? extends AuthenticationConfiguration>, AuthenticationMechanismType> authenticationConfigurationTypesMap;

    private final ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes;

    private Connection(String identifier, DatabaseSupport databaseSupport, List<AuthenticationMechanism> authenticationMechanisms, ConnectionSpecification connectionSpecification, AuthenticationConfiguration authenticationConfiguration)
    {
        this.identifier = Objects.requireNonNull(identifier, "Can't create connection: identifier is missing");
        this.databaseSupport = databaseSupport;
        this.connectionSpecification = Objects.requireNonNull(connectionSpecification, "Connection specification is missing");
        this.authenticationConfiguration = Objects.requireNonNull(authenticationConfiguration, "Authentication configuration is missing");

        Map<String, AuthenticationMechanism> authenticationMechanismsIndex = new LinkedHashMap<>();
        List<AuthenticationMechanismType> authenticationMechanismTypes = Lists.mutable.empty();
        if (authenticationMechanisms.isEmpty())
        {
            for (AuthenticationMechanismType authenticationMechanismType : this.databaseSupport.getAuthenticationMechanismTypes())
            {
                authenticationMechanismsIndex.put(authenticationMechanismType.getIdentifier(), this.databaseSupport.getAuthenticationMechanism(authenticationMechanismType));
            }
            authenticationMechanismTypes.addAll(this.databaseSupport.getAuthenticationMechanismTypes().toList());
        }
        else
        {
            for (AuthenticationMechanism authenticationMechanism : authenticationMechanisms)
            {
                AuthenticationMechanismType authenticationMechanismType = authenticationMechanism.getAuthenticationMechanismType();
                // if no mechanism is specified, it means the connection supports all mechanisms specified in the database support
                if (authenticationMechanismsIndex.containsKey(authenticationMechanismType.getIdentifier()))
                {
                    throw new RuntimeException(String.format("Found multiple configurations for authentication mechanism '%s'", authenticationMechanismType.getIdentifier()));
                }
                AuthenticationMechanism authenticationMechanismsFromDatabaseSupport = this.databaseSupport.getAuthenticationMechanism(authenticationMechanismType);
                if (authenticationMechanismsFromDatabaseSupport == null)
                {
                    throw new RuntimeException(String.format("Authentication mechanism '%s' is not covered by database support '%s'. Supported mechanism(s):\n%s",
                            authenticationMechanismType.getIdentifier(),
                            this.databaseSupport.getDatabaseType().getIdentifier(),
                            this.databaseSupport.getAuthenticationMechanismTypes().collect(mechanism -> "- " + mechanism.getIdentifier()).makeString("\n")
                    ));
                }
                ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigTypesFromDatabaseSupport = authenticationMechanismsFromDatabaseSupport.getAuthenticationConfigurationTypes();
                List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes = Lists.mutable.empty();
                for (Class<? extends AuthenticationConfiguration> authenticationConfigurationType : authenticationMechanism.getAuthenticationConfigurationTypes())
                {
                    if (!authenticationConfigTypesFromDatabaseSupport.contains(authenticationConfigurationType))
                    {
                        throw new RuntimeException(String.format("Authentication configuration type '%s' is not covered by database support '%s' for authentication mechanism '%s'. Supported configuration type(s):\n%s",
                                authenticationConfigurationType.getSimpleName(),
                                this.databaseSupport.getDatabaseType().getIdentifier(),
                                authenticationMechanismType.getIdentifier(),
                                authenticationConfigTypesFromDatabaseSupport.collect(type -> "- " + type.getSimpleName()).makeString("\n")
                        ));
                    }
                    else
                    {
                        authenticationConfigurationTypes.add(authenticationConfigurationType);
                    }
                }
                authenticationMechanismsIndex.put(authenticationMechanismType.getIdentifier(), AuthenticationMechanism
                        .builder()
                        .type(authenticationMechanismType)
                        // if no configuration type is specified, it means the connection supports all configuration types configured for that mechanism in the database support
                        .authenticationConfigurationTypes(!authenticationConfigurationTypes.isEmpty() ? authenticationConfigurationTypes : authenticationConfigTypesFromDatabaseSupport.toList())
                        .build());
                authenticationMechanismTypes.add(authenticationMechanismType);
            }
        }
        this.authenticationMechanismsIndex = Maps.immutable.withAll(authenticationMechanismsIndex);
        this.authenticationMechanismTypes = Lists.immutable.withAll(authenticationMechanismTypes);

        Map<Class<? extends AuthenticationConfiguration>, AuthenticationMechanismType> authenticationConfigurationTypesMap = new LinkedHashMap<>();
        List<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes = Lists.mutable.empty();
        authenticationMechanismsIndex.values().forEach((authenticationMechanism) ->
        {
            authenticationMechanism.getAuthenticationConfigurationTypes().forEach(configurationType ->
            {
                authenticationConfigurationTypesMap.put(configurationType, authenticationMechanism.getAuthenticationMechanismType());
                authenticationConfigurationTypes.add(configurationType);
            });
        });
        this.authenticationConfigurationTypesMap = Maps.immutable.withAll(authenticationConfigurationTypesMap);
        this.authenticationConfigurationTypes = Lists.immutable.withAll(authenticationConfigurationTypes);

        if (!this.authenticationConfigurationTypesMap.containsKey(this.authenticationConfiguration.getClass()))
        {
            throw new RuntimeException(String.format("Specified authentication configuration of type '%s' is not compatible. Supported configuration type(s):\n%s",
                    this.authenticationConfiguration.getClass().getSimpleName(),
                    this.getAuthenticationConfigurationTypes().collect(type -> "- " + type.getSimpleName()).makeString("\n")
            ));
        }
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public DatabaseSupport getDatabaseSupport()
    {
        return databaseSupport;
    }

    public ConnectionSpecification getConnectionSpecification()
    {
        return connectionSpecification;
    }

    public AuthenticationConfiguration getAuthenticationConfiguration()
    {
        return authenticationConfiguration;
    }

    public ImmutableList<AuthenticationMechanismType> getAuthenticationMechanisms()
    {
        return this.authenticationMechanismTypes;
    }

    public ImmutableList<Class<? extends AuthenticationConfiguration>> getAuthenticationConfigurationTypes()
    {
        return this.authenticationConfigurationTypes;
    }

    public AuthenticationMechanismType getAuthenticationMechanism(Class<? extends AuthenticationConfiguration> authenticationConfigurationType)
    {
        return this.authenticationConfigurationTypesMap.get(authenticationConfigurationType);
    }

    public AuthenticationMechanism getAuthenticationMechanism(AuthenticationMechanismType authenticationMechanismType)
    {
        return authenticationMechanismsIndex.get(authenticationMechanismType.getIdentifier());
    }

    public <T extends ConnectionSpecification> T getConnectionSpecification(Class<T> clazz)
    {
        if (!this.connectionSpecification.getClass().equals(clazz))
        {
            throw new RuntimeException(String.format("Can't get connection specification of type '%s' for store '%s'", clazz.getSimpleName(), this.identifier));
        }
        return (T) this.connectionSpecification;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private DatabaseSupport databaseSupport;
        private String identifier;
        private final List<AuthenticationMechanism> authenticationMechanisms = Lists.mutable.empty();
        private ConnectionSpecification connectionSpecification;
        private AuthenticationConfiguration authenticationConfiguration;

        private Builder()
        {
        }

        public Builder databaseSupport(DatabaseSupport databaseSupport)
        {
            this.databaseSupport = databaseSupport;
            return this;
        }

        public Builder identifier(String identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public Builder authenticationMechanism(AuthenticationMechanism authenticationMechanism)
        {
            this.authenticationMechanisms.add(authenticationMechanism);
            return this;
        }

        public Builder authenticationMechanisms(List<AuthenticationMechanism> authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(authenticationMechanisms);
            return this;
        }

        public Builder authenticationMechanisms(AuthenticationMechanism... authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(Lists.mutable.of(authenticationMechanisms));
            return this;
        }

        public Builder connectionSpecification(ConnectionSpecification connectionSpecification)
        {
            this.connectionSpecification = connectionSpecification;
            return this;
        }

        public Builder authenticationConfiguration(AuthenticationConfiguration authenticationConfiguration)
        {
            this.authenticationConfiguration = authenticationConfiguration;
            return this;
        }

        public Builder fromProtocol(org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection protocol, LegendEnvironment environment)
        {
            return this
                    .databaseSupport(environment.getDatabaseSupport(environment.getDatabaseType(protocol.databaseType)))
                    .identifier(protocol.getPath())
                    .authenticationMechanisms(
                            protocol.authenticationMechanisms != null
                                    ? ListIterate.collect(protocol.authenticationMechanisms, mechanism ->
                                    AuthenticationMechanism
                                            .builder()
                                            .type(environment.getAuthenticationMechanism(mechanism.authenticationMechanismType))
                                            .authenticationConfigurationTypes(
                                                    ListIterate.collect(mechanism.configurationTypes, environment::getAuthenticationConfigurationType)
                                            )
                                            .build())
                                    : Lists.mutable.empty()
                    )
                    .connectionSpecification(protocol.connectionSpecification)
                    .authenticationConfiguration(protocol.authenticationConfiguration);
        }


        public Connection build()
        {
            return new Connection(
                    this.identifier,
                    this.databaseSupport,
                    this.authenticationMechanisms,
                    this.connectionSpecification,
                    this.authenticationConfiguration
            );
        }
    }
}
