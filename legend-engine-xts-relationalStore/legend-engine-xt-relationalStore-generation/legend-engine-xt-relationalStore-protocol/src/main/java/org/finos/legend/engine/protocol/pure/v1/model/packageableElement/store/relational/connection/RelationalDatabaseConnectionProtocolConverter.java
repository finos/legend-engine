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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class RelationalDatabaseConnectionProtocolConverter<D extends DatasourceSpecification, A extends AuthenticationStrategy>
{
    private Class<? extends DatasourceSpecification> getDatasourceClass()
    {
        return (Class<? extends DatasourceSpecification>) actualTypeArguments()[0];
    }

    private Class<? extends AuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return (Class<? extends AuthenticationStrategy>) actualTypeArguments()[1];
    }

    private Type[] actualTypeArguments()
    {
        Type genericSuperClass = this.getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
        return parameterizedType.getActualTypeArguments();
    }

    protected abstract DatabaseType getDatabaseType();

    protected abstract RelationalDatabaseConnection convert(D datasourceSpecification, A authenticationStrategy, RelationalDatabaseConnection relationalDatabaseConnection);

    public RelationalDatabaseConnection convert(RelationalDatabaseConnection relationalDatabaseConnection)
    {
        if (
                DatabaseType.Snowflake.equals(relationalDatabaseConnection.databaseType) &&
                        this.getDatasourceClass().equals(relationalDatabaseConnection.datasourceSpecification.getClass()) &&
                        this.getAuthenticationStrategyClass().equals(relationalDatabaseConnection.authenticationStrategy.getClass())
        )
        {
            return this.convert((D) relationalDatabaseConnection.datasourceSpecification, (A) relationalDatabaseConnection.authenticationStrategy, relationalDatabaseConnection);
        }
        return null;
    }
}
