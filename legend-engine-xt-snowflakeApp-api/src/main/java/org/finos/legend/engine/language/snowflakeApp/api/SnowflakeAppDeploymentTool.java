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

package org.finos.legend.engine.language.snowflakeApp.api;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class SnowflakeAppDeploymentTool
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeAppDeploymentTool.class);

    private final ConnectionManagerSelector connectionManager;

    public SnowflakeAppDeploymentTool(ConnectionManagerSelector connectionManager)
    {
        this.connectionManager = connectionManager;
    }

    public void deploy(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy, String context) throws Exception
    {
        LOGGER.info("Starting deployment");
        try (Connection jdbcConnection = this.getJdbcConnection(datasourceSpecification, authenticationStrategy))
        {
            jdbcConnection.setAutoCommit(false);
            this.deployImpl(jdbcConnection, context);
            jdbcConnection.commit();
            LOGGER.info("Completed deployment successfully");
        }
        catch (Exception e)
        {
            LOGGER.info("Completed deployment with error");
            throw e;
        }
    }

    public ImmutableList<DeploymentInfo> getDeployed(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy) throws Exception
    {
        ImmutableList<DeploymentInfo> deployments = null;

        LOGGER.info("Querying deployment");
        try (Connection jdbcConnection = this.getJdbcConnection(datasourceSpecification, authenticationStrategy))
        {
            deployments = this.getDeployedImpl(jdbcConnection);
            LOGGER.info("Completed querying deployments successfully");
        }
        catch (Exception e)
        {
            LOGGER.info("Completed querying deployments with error");
            throw e;
        }
        return deployments;
    }

    public Connection getJdbcConnection(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy)
    {
        RelationalDatabaseConnection databaseConnection = this.adaptConnection(datasourceSpecification, authenticationStrategy);
        databaseConnection.type = DatabaseType.Snowflake;
        Identity identity = new Identity("unknown");
        Connection jdbcConnection = this.connectionManager.getDatabaseConnection(identity, (DatabaseConnection) databaseConnection);
        return jdbcConnection;
    }

    // override this method as needed
    public void deployImpl(Connection jdbcConnection, String context) throws Exception
    {
        Statement statement = jdbcConnection.createStatement();
        String deploymentTableName = this.getDeploymentTableName(jdbcConnection);
        String createTableSQL = String.format("create table %s (id INTEGER, message VARCHAR(1000)) if not exists", deploymentTableName);
        boolean createTableStatus = statement.execute(createTableSQL);
        String insertSQL = String.format("insert into %s(id, message) values(%d, '%s')", deploymentTableName, System.currentTimeMillis(), context);
        boolean insertStatus = statement.execute(insertSQL);
    }

    public String getDeploymentTableName(Connection jdbcConnection) throws SQLException
    {
        String catalogName = jdbcConnection.getCatalog();
        String schema = "NATIVE_APP";
        String deploymentTableName = String.format("%s.%s.LEGEND_SNOWFLAKE_APP_DEPLOYMENT", catalogName, schema);
        return deploymentTableName;
    }

    public ImmutableList<DeploymentInfo> getDeployedImpl(Connection jdbcConnection) throws Exception
    {
        MutableList<DeploymentInfo> deployments = Lists.mutable.empty();
        String deploymentTableName = this.getDeploymentTableName(jdbcConnection);
        String querySql = String.format("select * from %s order by id", deploymentTableName);
        Statement statement = jdbcConnection.createStatement();
        ResultSet resultSet = statement.executeQuery(querySql);
        while (resultSet.next())
        {
            DeploymentInfo deploymentInfo = new DeploymentInfo();
            deploymentInfo.attributes.put("id", resultSet.getLong(1));
            deploymentInfo.attributes.put("message", resultSet.getString(2));
            deployments.add(deploymentInfo);
        }
        return deployments.toImmutable();
    }

    private RelationalDatabaseConnection adaptConnection(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy)
    {
        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();

        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = datasourceSpecification._accountName();
        snowflakeDatasourceSpecification.databaseName = datasourceSpecification._databaseName();
        snowflakeDatasourceSpecification.role = datasourceSpecification._role();
        snowflakeDatasourceSpecification.warehouseName = datasourceSpecification._warehouseName();
        snowflakeDatasourceSpecification.region = datasourceSpecification._region();
        snowflakeDatasourceSpecification.cloudType = datasourceSpecification._cloudType();

        SnowflakePublicAuthenticationStrategy snowflakeAuthenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        snowflakeAuthenticationStrategy.privateKeyVaultReference = authenticationStrategy._privateKeyVaultReference();
        snowflakeAuthenticationStrategy.passPhraseVaultReference = authenticationStrategy._passPhraseVaultReference();
        snowflakeAuthenticationStrategy.publicUserName = authenticationStrategy._publicUserName();

        connection.authenticationStrategy = snowflakeAuthenticationStrategy;
        connection.datasourceSpecification = snowflakeDatasourceSpecification;
        connection.type = DatabaseType.Snowflake;

        return connection;
    }

    public static class DeploymentInfo
    {
        public Map<String, Object> attributes = Maps.mutable.empty();
    }
}
