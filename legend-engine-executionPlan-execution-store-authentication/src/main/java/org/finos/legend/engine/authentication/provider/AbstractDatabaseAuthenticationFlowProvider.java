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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;

public abstract class AbstractDatabaseAuthenticationFlowProvider implements DatabaseAuthenticationFlowProvider
{
    protected Map<DatabaseAuthenticationFlowKey, DatabaseAuthenticationFlow> flows = new HashMap<>();

    public void registerFlow(DatabaseAuthenticationFlow flow)
    {
        this.validateProperUseOfH2Flows(flow);
        DatabaseAuthenticationFlowKey key = DatabaseAuthenticationFlowKey.newKey(flow);
        this.flows.put(key, flow);
    }

    /*
        This validation exists to limit the use of H2 in flows.
        The flow implementation maintains some state for the lifetime of the datasource/connection.
        We create a lot of use-and-throw H2 connections for running user tests. This results in us a shortlived state.
        Therefore we do not allow the use of H2 with the LocalH2 connections.
        We only allow StaticH2 connections for developer testing. We do not expect static h2 to be used frequently in the wild.
     */
    private void validateProperUseOfH2Flows(DatabaseAuthenticationFlow flow)
    {
        DatabaseType databaseType = flow.getDatabaseType();
        if (databaseType != DatabaseType.H2)
        {
            return;
        }
        Class<? extends DatasourceSpecification> datasourceClass = flow.getDatasourceClass();
        if (!datasourceClass.equals(StaticDatasourceSpecification.class))
        {
            String message = String.format("Attempt to register a H2 flow with datasource spec '%s'. Only '%s' is supported for H2", datasourceClass.getSimpleName(), StaticDatasourceSpecification.class.getSimpleName());
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public Optional<DatabaseAuthenticationFlow> lookupFlow(RelationalDatabaseConnection connection)
    {
        DatabaseType databaseType = this.resolveType(connection.type, connection.databaseType);
        Class<? extends DatasourceSpecification> datasourceClass = connection.datasourceSpecification.getClass();
        Class<? extends AuthenticationStrategy> authenticationClass = connection.authenticationStrategy.getClass();
        DatabaseAuthenticationFlowKey key = new DatabaseAuthenticationFlowKey(databaseType, datasourceClass, authenticationClass);
        return Optional.ofNullable(this.flows.get(key));
    }

    public static DatabaseType resolveType(DatabaseType type1, DatabaseType type2)
    {
        if (type1 != null & type2 != null && type1 != type2)
        {
            throw new RuntimeException(String.format("Connection has conflicting database types : Type1=%s, Type2=%s", type1, type2));
        }
        if (type1 != null)
        {
            return type1;
        }
        return type2;
    }

    @Override
    public int count()
    {
        return this.flows.size();
    }
}