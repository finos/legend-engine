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

package org.finos.legend.engine.language.snowflakeApp.deployment;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.language.snowflakeApp.api.SnowflakeAppDeploymentTool;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;


public class SnowflakeDeploymentManager implements DeploymentManager<SnowflakeAppArtifact, SnowflakeDeploymentResult, SnowflakeAppDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeDeploymentManager.class);

    private SnowflakeAppDeploymentTool snowflakeAppDeploymentTool;
    private PlanExecutor planExecutor;
    private ConnectionManagerSelector connectionManager;
    private static final String deploymentSchema = "LEGEND_GOVERNANCE";
    private static final  String deploymentTable = "BUSINESS_OBJECTS";

    public SnowflakeDeploymentManager(SnowflakeAppDeploymentTool deploymentTool)
    {
        this.snowflakeAppDeploymentTool = deploymentTool;
    }

    public SnowflakeDeploymentManager(PlanExecutor planExecutor)
    {
        this.planExecutor = planExecutor;
        connectionManager = ((RelationalStoreState)planExecutor.getExtraExecutors().select(c -> c instanceof RelationalStoreExecutor).getFirst().getStoreState()).getRelationalExecutor().getConnectionManager();
    }

    @Override
    public boolean canDeploy(FunctionActivatorArtifact artifact)
    {
        return artifact instanceof SnowflakeAppArtifact;
    }

    @Override
    public SnowflakeDeploymentResult deploy(MutableList<CommonProfile> profiles, SnowflakeAppArtifact artifact)
    {
        return new SnowflakeDeploymentResult("",true);
    }

    @Override
    public SnowflakeDeploymentResult deploy(MutableList<CommonProfile> profiles, SnowflakeAppArtifact artifact, List<SnowflakeAppDeploymentConfiguration> availableRuntimeConfigurations)
    {
        LOGGER.info("Starting deployment");
        SnowflakeDeploymentResult result;
        try (Connection jdbcConnection = this.getDeploymentConnection(profiles, artifact))
        {
            String appName = ((SnowflakeAppContent)artifact.content).applicationName;
            jdbcConnection.setAutoCommit(false);
            this.deployImpl(jdbcConnection, (SnowflakeAppContent)artifact.content);
            jdbcConnection.commit();
            LOGGER.info("Completed deployment successfully");
            result = new SnowflakeDeploymentResult(appName, true);
        }
        catch (Exception e)
        {
            LOGGER.info("Completed deployment with error");
            result = new SnowflakeDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
        return result;
    }


    public SnowflakeAppDeploymentTool getSnowflakeAppDeploymentTool()
    {
        return snowflakeAppDeploymentTool;
    }


    public SnowflakeDeploymentResult fakeDeploy(Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy authenticationStrategy, String applicationName)
    {
        try
        {
            this.snowflakeAppDeploymentTool.deploy(datasourceSpecification, authenticationStrategy, applicationName);
            return new SnowflakeDeploymentResult("",true);
        }
        catch (Exception e)
        {
            return new SnowflakeDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
    }

    public java.sql.Connection getDeploymentConnection(MutableList<CommonProfile> profiles, RelationalDatabaseConnection connection)
    {
        return this.connectionManager.getDatabaseConnection(profiles, (DatabaseConnection) connection);
    }

    public void deployImpl(Connection jdbcConnection, SnowflakeAppContent context) throws Exception
    {
        Statement statement = jdbcConnection.createStatement();
        String deploymentTableName = this.getDeploymentTableName(jdbcConnection);

        //String createTableSQL = String.format("create table %s (id INTEGER, message VARCHAR(1000)) if not exists", deploymentTableName);
        //boolean createTableStatus = statement.execute(createTableSQL);
        String insertSQL = String.format("insert into %s(CREATE_DATETIME, APP_NAME, SQL_FRAGMENT, VERSION_NUMBER, OWNER) values('%s', '%s', '%s', '%s', '%s')", deploymentTableName, context.creationTime, context.applicationName, context.sqlExpressions.getFirst(), context.getVersionInfo(), Lists.mutable.withAll(context.owners).makeString(","));
        boolean insertStatus = statement.execute(insertSQL);
    }

    public String getDeploymentTableName(Connection jdbcConnection) throws SQLException
    {
        String catalogName = jdbcConnection.getCatalog();
        return String.format("%s.%s." + deploymentTable, catalogName, deploymentSchema);
    }

    public java.sql.Connection getDeploymentConnection(MutableList<CommonProfile> profiles, SnowflakeAppArtifact artifact)
    {
        RelationalDatabaseConnection connection = extractConnectionFromArtifact(artifact);
        return this.connectionManager.getDatabaseConnection(profiles, connection);
    }

    public RelationalDatabaseConnection extractConnectionFromArtifact(SnowflakeAppArtifact artifact)
    {
        return ((SnowflakeAppDeploymentConfiguration)artifact.deploymentConfiguration).connection;
    }

    public ImmutableList<DeploymentInfo> getDeployed(MutableList<CommonProfile> profiles, RelationalDatabaseConnection connection) throws Exception
    {
        ImmutableList<DeploymentInfo> deployments = null;

        LOGGER.info("Querying deployment");
        try (Connection jdbcConnection = this.getDeploymentConnection(profiles, connection))
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

    public static class DeploymentInfo
    {
        public Map<String, Object> attributes = Maps.mutable.empty();
    }

}
