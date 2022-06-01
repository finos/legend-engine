// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.authentication.provider;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;

import java.util.Optional;

public interface DatabaseAuthenticationFlowProvider
{
    Optional<DatabaseAuthenticationFlow> lookupFlow(RelationalDatabaseConnection connection);

    default DatabaseAuthenticationFlow lookupFlowOrThrow(RelationalDatabaseConnection connection)
    {
        Optional<DatabaseAuthenticationFlow> flowHolder = this.lookupFlow(connection);
        return flowHolder.orElseThrow(() -> flowNotFoundException(connection));
    }

    static RuntimeException flowNotFoundException(RelationalDatabaseConnection connection)
    {
        String message = String.format("Unsupported db authn flow. Database=%s,Datasource=%s,Authentication=%s",
                AbstractDatabaseAuthenticationFlowProvider.resolveType(connection.type, connection.databaseType),
                connection.datasourceSpecification.getClass().getSimpleName(),
                connection.authenticationStrategy.getClass().getSimpleName());
        throw new RuntimeException(message);
    }

    void configure(DatabaseAuthenticationFlowProviderConfiguration configuration);

    int count();
}