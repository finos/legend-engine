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

package org.finos.legend.connection.experimental.demo;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.connection.experimental.StoreSupport;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;

import java.util.List;
import java.util.Objects;

public class RelationalDatabaseStoreSupport extends StoreSupport
{
    private final String databaseType;

    private RelationalDatabaseStoreSupport(String identifier, String databaseType, List<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes)
    {
        super(identifier, authenticationSpecificationTypes);
        this.databaseType = databaseType;
    }

    public String getDatabaseType()
    {
        return databaseType;
    }

    public static class Builder
    {
        private String identifier;
        private String databaseType;
        private final List<Class<? extends AuthenticationSpecification>> authenticationSpecificationTypes = Lists.mutable.empty();

        public Builder withIdentifier(String identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public Builder withDatabaseType(String databaseType)
        {
            this.databaseType = databaseType;
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

        public RelationalDatabaseStoreSupport build()
        {
            return new RelationalDatabaseStoreSupport(
                    Objects.requireNonNull(this.identifier, "Store support identifier is required"),
                    Objects.requireNonNull(this.databaseType, "Store support database type is required"),
                    this.authenticationSpecificationTypes
            );
        }
    }
}
