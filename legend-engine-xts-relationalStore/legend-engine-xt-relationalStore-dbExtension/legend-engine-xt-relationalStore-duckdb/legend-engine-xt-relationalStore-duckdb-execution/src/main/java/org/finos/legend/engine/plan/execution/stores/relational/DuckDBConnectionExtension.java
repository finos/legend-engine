//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational;

import java.util.List;
import java.util.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.authentication.strategy.key.DuckDBS3AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.duckdb.DuckDBCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.duckdb.DuckDBManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.ds.specifications.DuckDBDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.ds.specifications.keys.DuckDBDataSourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.authentication.DuckDBS3AuthenticationStrategy;

public class DuckDBConnectionExtension implements RelationalConnectionExtension, StrategicConnectionExtension
{

    @Override
    public String type()
    {
        return "MIX_ConnectionExtension_&_Strategic_Connection_Extension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "DuckDB");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new DuckDBManager());
    }

    @Override
    public Boolean visit(StreamResultToTempTableVisitor visitor, RelationalDatabaseCommands databaseCommands)
    {
        if (databaseCommands instanceof DuckDBCommands)
        {
            throw new UnsupportedOperationException("not yet implemented");
        }
        return null;
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators()
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof DuckDBS3AuthenticationStrategy)
            {
                DuckDBS3AuthenticationStrategy s3AuthenticationStrategy = (DuckDBS3AuthenticationStrategy) authenticationStrategy;
                return new DuckDBS3AuthenticationStrategyKey(
                        s3AuthenticationStrategy.region,
                        s3AuthenticationStrategy.accessKeyId,
                        s3AuthenticationStrategy.secretAccessKeyVaultReference,
                        s3AuthenticationStrategy.endpoint
                );
            }
            return null;
        };
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles)
    {
        return authenticationStrategy ->
        {
            if (authenticationStrategy instanceof DuckDBS3AuthenticationStrategy)
            {
                DuckDBS3AuthenticationStrategy s3 = (DuckDBS3AuthenticationStrategy) authenticationStrategy;
                DuckDBS3AuthenticationStrategyKey key = new DuckDBS3AuthenticationStrategyKey(s3.region, s3.accessKeyId, s3.secretAccessKeyVaultReference, s3.endpoint);
                return new org.finos.legend.engine.plan.execution.stores.relational.authentication.strategy.DuckDBS3AuthenticationStrategy(key);
            }
            return null;
        };
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort)
    {
        return connection -> datasourceSpecification ->
        {
            if (datasourceSpecification instanceof DuckDBDatasourceSpecification)
            {
                DuckDBDatasourceSpecification databricksSpecification = (DuckDBDatasourceSpecification) datasourceSpecification;
                return new DuckDBDataSourceSpecificationKey(
                        databricksSpecification.path
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
            if (datasourceSpecification instanceof DuckDBDatasourceSpecification)
            {
                return new DuckDBDataSourceSpecification(
                        (DuckDBDataSourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                        new DuckDBManager(),
                        authenticationStrategyProvider.apply(connection)
                );
            }
            return null;
        };
    }
}
