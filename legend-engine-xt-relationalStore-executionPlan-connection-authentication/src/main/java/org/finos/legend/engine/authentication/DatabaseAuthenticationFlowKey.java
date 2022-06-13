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

package org.finos.legend.engine.authentication;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.Objects;

public class DatabaseAuthenticationFlowKey
{
    private final DatabaseType databaseType;
    private final Class<? extends DatasourceSpecification> datasourceProtocolSpecClass;
    private final Class<? extends AuthenticationStrategy> authStrategyProtocolSpecClass;

    public static DatabaseAuthenticationFlowKey newKey(DatabaseType databaseType, Class<? extends DatasourceSpecification> datasourceClass, Class<? extends AuthenticationStrategy> authenticationStrategyClass)
    {
        return new DatabaseAuthenticationFlowKey(databaseType, datasourceClass, authenticationStrategyClass);
    }

    public static DatabaseAuthenticationFlowKey newKey(DatabaseAuthenticationFlow flow)
    {
        DatabaseType databaseType = flow.getDatabaseType();
        Class<? extends DatasourceSpecification> datasourceClass = flow.getDatasourceClass();
        Class<? extends AuthenticationStrategy> authenticationClass = flow.getAuthenticationStrategyClass();
        return DatabaseAuthenticationFlowKey.newKey(databaseType, datasourceClass, authenticationClass);
    }

    public static DatabaseAuthenticationFlowKey newKey(RelationalDatabaseConnection connection)
    {
        DatabaseType databaseType = connection.databaseType;
        Class<? extends DatasourceSpecification> datasourceClass = connection.datasourceSpecification.getClass();
        Class<? extends AuthenticationStrategy> authenticationClass = connection.authenticationStrategy.getClass();
        return DatabaseAuthenticationFlowKey.newKey(databaseType, datasourceClass, authenticationClass);
    }

    public DatabaseAuthenticationFlowKey(DatabaseType databaseType, Class<? extends DatasourceSpecification> datasourceProtocolSpecClass, Class<? extends AuthenticationStrategy> authStrategyProtocolSpecClass)
    {
        this.databaseType = databaseType;
        this.datasourceProtocolSpecClass = datasourceProtocolSpecClass;
        this.authStrategyProtocolSpecClass = authStrategyProtocolSpecClass;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DatabaseAuthenticationFlowKey that = (DatabaseAuthenticationFlowKey) o;
        return databaseType == that.databaseType &&
                datasourceProtocolSpecClass.equals(that.datasourceProtocolSpecClass) &&
                authStrategyProtocolSpecClass.equals(that.authStrategyProtocolSpecClass);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(databaseType, datasourceProtocolSpecClass, authStrategyProtocolSpecClass);
    }
}