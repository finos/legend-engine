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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.ConnectionSpecification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A StoreInstance represents a named instance of a Store.
 */
public class StoreInstance
{
    private final String identifier;
    private final StoreSupport storeSupport;
    private final ImmutableList<AuthenticationMechanism> authenticationMechanisms;
    private final ImmutableList<Class<? extends AuthenticationConfiguration>> authenticationConfigurationTypes;
    private final ConnectionSpecification connectionSpecification;

    private StoreInstance(String identifier, StoreSupport storeSupport, List<AuthenticationMechanism> authenticationMechanisms, ConnectionSpecification connectionSpecification)
    {
        this.identifier = identifier;
        this.storeSupport = storeSupport;
        this.authenticationMechanisms = Lists.immutable.withAll(authenticationMechanisms);
        this.authenticationConfigurationTypes = Lists.immutable.withAll(ListIterate.collect(authenticationMechanisms, AuthenticationMechanism::getAuthenticationConfigurationType));
        this.connectionSpecification = connectionSpecification;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public StoreSupport getStoreSupport()
    {
        return storeSupport;
    }

    public ImmutableList<AuthenticationMechanism> getAuthenticationMechanisms()
    {
        return authenticationMechanisms;
    }

    public ImmutableList<Class<? extends AuthenticationConfiguration>> getAuthenticationConfigurationTypes()
    {
        return authenticationConfigurationTypes;
    }

    public ConnectionSpecification getConnectionSpecification()
    {
        return connectionSpecification;
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
        private final Set<AuthenticationMechanism> authenticationMechanisms = new LinkedHashSet<>();
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

        public Builder withAuthenticationMechanisms(AuthenticationMechanism... authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(Lists.mutable.of(authenticationMechanisms));
            return this;
        }

        public Builder withConnectionSpecification(ConnectionSpecification connectionSpecification)
        {
            this.connectionSpecification = connectionSpecification;
            return this;
        }

        public StoreInstance build()
        {
            StoreSupport storeSupport = this.environment.findStoreSupport(Objects.requireNonNull(this.storeSupportIdentifier, "Store instance store support identifier is required"));
            MutableList<AuthenticationMechanism> unsupportedAuthenticationMechanisms = ListIterate.select(new ArrayList<>(this.authenticationMechanisms), mechanism -> !storeSupport.getAuthenticationMechanisms().contains(mechanism));
            if (!unsupportedAuthenticationMechanisms.isEmpty())
            {
                throw new RuntimeException(String.format("Store instance specified with authentication configuration types (%s) which are not covered by its store support '%s'", unsupportedAuthenticationMechanisms.makeString(", "), storeSupport.getIdentifier()));
            }
            return new StoreInstance(
                    Objects.requireNonNull(this.identifier, "Store instance identifier is required"),
                    storeSupport,
                    // NOTE: if no mechanism is specified, it means the store instance supports all mechanisms
                    this.authenticationMechanisms.isEmpty() ? storeSupport.getAuthenticationMechanisms().toList() : new ArrayList<>(this.authenticationMechanisms),
                    Objects.requireNonNull(this.connectionSpecification, "Store instance connection specification is required")
            );
        }
    }
}
