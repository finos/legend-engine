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
import org.finos.legend.connection.protocol.AuthenticationMechanism;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RelationalDatabaseStoreSupport extends StoreSupport
{
    private final String databaseType;
    private final Set<AuthenticationMechanism> authenticationMechanisms = new LinkedHashSet<>();

    private RelationalDatabaseStoreSupport(String identifier, String databaseType, List<AuthenticationMechanism> authenticationMechanisms)
    {
        super(identifier, authenticationMechanisms);
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
        private final Set<AuthenticationMechanism> authenticationMechanisms = new LinkedHashSet<>();

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

        public Builder withAuthenticationMechanisms(List<AuthenticationMechanism> authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(authenticationMechanisms);
            return this;
        }

        public Builder withAuthenticationMechanisms(AuthenticationMechanism... authenticationMechanisms)
        {
            this.authenticationMechanisms.addAll(Lists.mutable.of(authenticationMechanisms));
            return this;
        }

        public Builder withAuthenticationMechanism(AuthenticationMechanism authenticationMechanism)
        {
            this.authenticationMechanisms.add(authenticationMechanism);
            return this;
        }

        public RelationalDatabaseStoreSupport build()
        {
            return new RelationalDatabaseStoreSupport(
                    Objects.requireNonNull(this.identifier, "Store support identifier is required"),
                    Objects.requireNonNull(this.databaseType, "Store support database type is required"),
                    new ArrayList<>(this.authenticationMechanisms)
            );
        }
    }
}
