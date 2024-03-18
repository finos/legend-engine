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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.memsql.MemSQLCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.memsql.MemSQLManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.MemSqlDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.MemSqlDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.MemSqlDatasourceSpecification;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

public class MemSQLConnectionExtension implements RelationalConnectionExtension, StrategicConnectionExtension
{
    @Override
    public String type()
    {
        return "(Connection)ConnectionExtension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "MemSQL");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new MemSQLManager());
    }

    @Override
    public Boolean visit(StreamResultToTempTableVisitor visitor, RelationalDatabaseCommands databaseCommands)
    {
        if (databaseCommands instanceof MemSQLCommands)
        {
            throw new UnsupportedOperationException("not yet implemented");
        }
        return null;
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy -> null;
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy -> null;
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort)
    {
        return connection -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof MemSqlDatasourceSpecification)
            {
                MemSqlDatasourceSpecification memsqlDatasourceSpecification = (MemSqlDatasourceSpecification) datasourceSpecification;
                return new MemSqlDatasourceSpecificationKey(
                        memsqlDatasourceSpecification.host,
                        memsqlDatasourceSpecification.port,
                        memsqlDatasourceSpecification.databaseName,
                        memsqlDatasourceSpecification.useSsl
                );
            }
            return null;
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(Function<RelationalDatabaseConnection, AuthenticationStrategy> authenticationStrategyProvider)
    {
        return (connection, connectionKey) -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof MemSqlDatasourceSpecification)
            {
                return new MemSqlDataSourceSpecification(
                        (MemSqlDatasourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new MemSQLManager(),
                        authenticationStrategyProvider.apply(connection),
                        new Properties()
                );
            }
            return null;
        };
    }
}