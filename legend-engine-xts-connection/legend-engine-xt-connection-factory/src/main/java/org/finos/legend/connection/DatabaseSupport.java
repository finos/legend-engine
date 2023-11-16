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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A DatabaseSupport describes the capabilities supported by a database.
 * For now, it describes the authentication mechanisms.
 */
public final class DatabaseSupport
{
    private final DatabaseType databaseType;
    private final ImmutableMap<String, AuthenticationMechanism> authenticationMechanismsIndex;
    private final ImmutableList<AuthenticationMechanismType> authenticationMechanismTypes;

    private DatabaseSupport(DatabaseType databaseType, List<AuthenticationMechanism> authenticationMechanisms)
    {
        this.databaseType = Objects.requireNonNull(databaseType, "Database type is missing");

        Map<String, AuthenticationMechanism> authenticationMechanismsIndex = new LinkedHashMap<>();
        List<AuthenticationMechanismType> authenticationMechanismTypes = Lists.mutable.empty();
        Map<Class<? extends AuthenticationConfiguration>, AuthenticationMechanismType> authenticationConfigurationTypeIndex = new LinkedHashMap<>();
        for (AuthenticationMechanism authenticationMechanism : authenticationMechanisms)
        {
            AuthenticationMechanismType authenticationMechanismType = authenticationMechanism.getAuthenticationMechanismType();
            if (authenticationMechanismsIndex.containsKey(authenticationMechanismType.getIdentifier()))
            {
                throw new RuntimeException(String.format("Found multiple authentication mechanisms with type '%s'", authenticationMechanismType.getIdentifier()));
            }
            authenticationMechanismsIndex.put(authenticationMechanismType.getIdentifier(), authenticationMechanism);
            authenticationMechanism.getAuthenticationConfigurationTypes().forEach(authenticationConfigurationType ->
            {
                if (authenticationConfigurationTypeIndex.containsKey(authenticationConfigurationType))
                {
                    throw new RuntimeException(String.format("Authentication configuration type '%s' is associated with multiple authentication mechanisms", authenticationConfigurationType.getSimpleName()));
                }
                authenticationConfigurationTypeIndex.put(authenticationConfigurationType, authenticationMechanismType);
            });
            authenticationMechanismTypes.add(authenticationMechanism.getAuthenticationMechanismType());
        }

        this.authenticationMechanismTypes = Lists.immutable.withAll(authenticationMechanismTypes);
        this.authenticationMechanismsIndex = Maps.immutable.withAll(authenticationMechanismsIndex);

        authenticationMechanisms.forEach((authenticationMechanism) ->
        {
            if (authenticationMechanism.getAuthenticationConfigurationTypes().isEmpty())
            {
                throw new RuntimeException(String.format("No authentication configuration type is associated with authentication mechanism '%s'", authenticationMechanism.getAuthenticationMechanismType().getIdentifier()));
            }
        });
    }

    public DatabaseType getDatabaseType()
    {
        return this.databaseType;
    }

    public AuthenticationMechanism getAuthenticationMechanism(AuthenticationMechanismType authenticationMechanismType)
    {
        return authenticationMechanismsIndex.get(authenticationMechanismType.getIdentifier());
    }

    public ImmutableList<AuthenticationMechanismType> getAuthenticationMechanismTypes()
    {
        return this.authenticationMechanismTypes;
    }

    public static void verifyDatabaseType(DatabaseSupport databaseSupport, DatabaseType databaseType)
    {
        if (!databaseType.equals(databaseSupport.getDatabaseType()))
        {

            throw new RuntimeException(String.format("Expected database type '%s'", databaseType.getIdentifier()));
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private DatabaseType databaseType;
        private final List<AuthenticationMechanism> authenticationMechanisms = Lists.mutable.empty();

        private Builder()
        {
        }

        public Builder type(DatabaseType databaseType)
        {
            this.databaseType = databaseType;
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

        public Builder fromProtocol(org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.DatabaseSupport databaseSupport, LegendEnvironment environment)
        {
            return this
                    .type(environment.getDatabaseType(databaseSupport.databaseType))
                    .authenticationMechanisms(
                            databaseSupport.authenticationMechanisms != null
                                    ? ListIterate.collect(databaseSupport.authenticationMechanisms, mechanism ->
                                    AuthenticationMechanism
                                            .builder()
                                            .type(environment.getAuthenticationMechanism(mechanism.authenticationMechanismType))
                                            .authenticationConfigurationTypes(
                                                    ListIterate.collect(mechanism.configurationTypes, environment::getAuthenticationConfigurationType)
                                            )
                                            .build())
                                    : Lists.mutable.empty()
                    );
        }

        public DatabaseSupport build()
        {
            return new DatabaseSupport(
                    this.databaseType,
                    this.authenticationMechanisms
            );
        }
    }
}
