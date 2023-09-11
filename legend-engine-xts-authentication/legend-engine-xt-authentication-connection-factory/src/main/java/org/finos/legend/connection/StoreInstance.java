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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;

import java.util.List;
import java.util.Objects;

public class StoreInstance
{
    private final String identifier;
    private final StoreSupport storeSupport;
    private final List<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes;
    private final ConnectionSpecification connectionSpecification;

    private StoreInstance(String identifier, StoreSupport storeSupport, List<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes, ConnectionSpecification connectionSpecification)
    {
        this.identifier = identifier;
        this.storeSupport = storeSupport;
        this.authenticationSpecificationTypes = authenticationSpecificationTypes;
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

    public List<Class<? extends AuthenticationSpecification>> getAuthenticationSpecificationTypes()
    {
        return authenticationSpecificationTypes;
    }

    public ConnectionSpecification  getConnectionSpecification()
    {
        return connectionSpecification;
    }

    public static class Builder
    {
        private final EnvironmentConfiguration environmentConfiguration;
        private String identifier;
        private String storeSupportIdentifier;
        private final MutableList<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes = Lists.mutable.empty();
        private ConnectionSpecification connectionSpecification;

        public Builder(EnvironmentConfiguration environmentConfiguration)
        {
            this.environmentConfiguration = environmentConfiguration;
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

        public Builder withAuthenticationSpecificationTypes(List<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes)
        {
            this.authenticationSpecificationTypes.addAll(authenticationSpecificationTypes);
            return this;
        }

        public Builder withAuthenticationSpecificationType(Class<? extends AuthenticationSpecification> authenticationSpecificationType)
        {
            this.authenticationSpecificationTypes.add(authenticationSpecificationType);
            return this;
        }

        public Builder withConnectionSpecification(ConnectionSpecification connectionSpecification)
        {
            this.connectionSpecification = connectionSpecification;
            return this;
        }

        public StoreInstance build()
        {
            StoreSupport storeSupport = this.environmentConfiguration.findStoreSupport(Objects.requireNonNull(this.storeSupportIdentifier, "Store instance store support identifier is required"));
            MutableList<Class<? extends AuthenticationSpecification>> unsupportedAuthenticationSpecificationTypes = this.authenticationSpecificationTypes.select(authenticationSpecificationType -> !storeSupport.getAuthenticationSpecificationTypes().contains(authenticationSpecificationType));
            if (!unsupportedAuthenticationSpecificationTypes.isEmpty())
            {
                throw new RuntimeException(String.format("Store instance specified with authentication specification types (%s) which are not covered by its store support '%s'", unsupportedAuthenticationSpecificationTypes.makeString(", "), storeSupport.getIdentifier()));
            }
            return new StoreInstance(
                    Objects.requireNonNull(this.identifier, "Store instance identifier is required"),
                    storeSupport,
                    this.authenticationSpecificationTypes,
                    Objects.requireNonNull(this.connectionSpecification, "Store instance connection specification is required")
            );
        }
    }
}
