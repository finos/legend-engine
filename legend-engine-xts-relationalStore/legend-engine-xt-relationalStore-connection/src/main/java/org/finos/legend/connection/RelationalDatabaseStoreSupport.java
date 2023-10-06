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

import java.util.List;
import java.util.Objects;

public class RelationalDatabaseStoreSupport extends StoreSupport
{
    private final Database database;

    private RelationalDatabaseStoreSupport(String identifier, Database database, List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations)
    {
        super(identifier, authenticationMechanismConfigurations);
        this.database = Objects.requireNonNull(database, "Relational database store support database type is missing");
    }

    public Database getDatabase()
    {
        return database;
    }

    public static RelationalDatabaseStoreSupport cast(StoreSupport storeSupport)
    {
        return cast(storeSupport, null);
    }

    public static RelationalDatabaseStoreSupport cast(StoreSupport storeSupport, Database database)
    {
        if (!(storeSupport instanceof RelationalDatabaseStoreSupport))
        {
            throw new RuntimeException("Expected store support for relational databases");
        }
        RelationalDatabaseStoreSupport relationalDatabaseStoreSupport = (RelationalDatabaseStoreSupport) storeSupport;
        if (database != null && !database.equals(relationalDatabaseStoreSupport.getDatabase()))
        {

            throw new RuntimeException(String.format("Expected relational database store support for '%s'", database.getLabel()));
        }
        return relationalDatabaseStoreSupport;
    }

    public static class Builder
    {
        private final Database database;
        private String identifier;
        private final List<AuthenticationMechanismConfiguration> authenticationMechanismConfigurations = Lists.mutable.empty();

        public Builder(Database database)
        {
            this.database = database;
        }

        public Builder withIdentifier(String identifier)
        {
            this.identifier = identifier;
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

        public RelationalDatabaseStoreSupport build()
        {
            return new RelationalDatabaseStoreSupport(
                    this.identifier,
                    this.database,
                    this.authenticationMechanismConfigurations
            );
        }
    }
}
