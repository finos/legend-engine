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
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.language.snowflakeApp.api.SnowflakeAppDeploymentTool;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppArtifact;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppContent;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;


public class SnowflakeAppDeploymentManager implements DeploymentManager<SnowflakeAppArtifact, SnowflakeDeploymentResult, SnowflakeAppDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeAppDeploymentManager.class);

    private SnowflakeAppDeploymentTool snowflakeAppDeploymentTool;
    private PlanExecutor planExecutor;
    private ConnectionManagerSelector connectionManager;
    private static final String deploymentSchema = "LEGEND_NATIVE_APPS";
    private static final  String deploymentTable = "APP_METADATA";
    private static final  String limit = "10000";


    private static String deployStub = "/schemas/" + deploymentSchema + "/user-function/%S()";

    private String enrichDeploymentLocation(String deploymentLocation, String appName)
    {
        return deploymentLocation + String.format(deployStub, appName);
    }

    public SnowflakeAppDeploymentManager(SnowflakeAppDeploymentTool deploymentTool)
    {
        this.snowflakeAppDeploymentTool = deploymentTool;
    }

    public SnowflakeAppDeploymentManager(PlanExecutor planExecutor)
    {
        this.planExecutor = planExecutor;
        connectionManager = ((RelationalStoreState)planExecutor.getExtraExecutors().select(c -> c instanceof RelationalStoreExecutor).getFirst().getStoreState()).getRelationalExecutor().getConnectionManager();
    }

    public List<SnowflakeAppDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return org.eclipse.collections.api.factory.Lists.mutable.withAll(availableConfigs).selectInstancesOf(SnowflakeAppDeploymentConfiguration.class);
    }

    @Override
    public boolean canDeploy(FunctionActivatorArtifact artifact)
    {
        return artifact instanceof SnowflakeAppArtifact;
    }

    @Override
    public SnowflakeDeploymentResult deploy(Identity identity, SnowflakeAppArtifact artifact)
    {
        return deploy(identity, artifact, Lists.mutable.empty());
    }

    @Override
    public SnowflakeDeploymentResult deploy(Identity identity, SnowflakeAppArtifact artifact, List<SnowflakeAppDeploymentConfiguration> availableRuntimeConfigurations)
    {
        LOGGER.info("Starting deployment");
        SnowflakeDeploymentResult result;
        //use the system connection if available (as would be the case in sandbox flow) , else use artifact connection (production flow)
        try (Connection jdbcConnection = availableRuntimeConfigurations.isEmpty() ? this.getDeploymentConnection(identity, artifact) : this.getDeploymentConnection(identity, availableRuntimeConfigurations.get(0).connection))
        {
            String appName = ((SnowflakeAppContent)artifact.content).applicationName;
            jdbcConnection.setAutoCommit(false);
            this.deployImpl(jdbcConnection, (SnowflakeAppContent)artifact.content);
            jdbcConnection.commit();
            LOGGER.info("Completed deployment successfully");
            result = new SnowflakeDeploymentResult(appName, true, enrichDeploymentLocation(artifact.deployedLocation, appName));
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
            return new SnowflakeDeploymentResult("",true, " ");
        }
        catch (Exception e)
        {
            return new SnowflakeDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
    }

    public Connection getDeploymentConnection(Identity identity, RelationalDatabaseConnection connection)
    {
        return this.connectionManager.getDatabaseConnection(identity, connection);
    }

    public void deployImpl(Connection jdbcConnection, SnowflakeAppContent content) throws SQLException
    {
        String catalogName = jdbcConnection.getCatalog();
        MutableList<String> statements = generateStatements(catalogName, content);
        for (String s: statements)
        {
            Statement statement = jdbcConnection.createStatement();
            statement.execute(s);
        }
    }

    public MutableList<String> generateStatements(String catalogName, SnowflakeAppContent content)
    {
        MutableList<String> statements = Lists.mutable.empty();
        if (!content.sqlExpressions.isEmpty())
        {
            statements.add(String.format("CREATE OR REPLACE SECURE FUNCTION %S.%S.%s", catalogName, deploymentSchema, content.sqlExpressions.getFirst()));
            if (content.sqlExpressions.size() > 1)
            {
                statements.add(String.format("GRANT USAGE ON FUNCTION %S.%S.%S", catalogName, deploymentSchema, content.sqlExpressions.get(1)));
            }
        }
        else
        {
            statements.add(String.format(content.createStatement, catalogName));
            statements.add(String.format(content.grantStatement, catalogName));
        }
        return statements;
    }

    public String getDeploymentTableName(Connection jdbcConnection) throws SQLException
    {
        String catalogName = jdbcConnection.getCatalog();
        return String.format("%s.%s." + deploymentTable, catalogName, deploymentSchema);
    }

    public Connection getDeploymentConnection(Identity identity, SnowflakeAppArtifact artifact)
    {
        RelationalDatabaseConnection connection = extractConnectionFromArtifact(artifact);
        return this.connectionManager.getDatabaseConnection(identity, connection);
    }

    public RelationalDatabaseConnection extractConnectionFromArtifact(SnowflakeAppArtifact artifact)
    {
        return ((SnowflakeAppDeploymentConfiguration)artifact.deploymentConfiguration).connection;
    }

    public MutableList<SnowflakeGrantInfo> getGrants(Identity identity, SnowflakeAppArtifact artifact)
    {
        RelationalDatabaseConnection rel = ((SnowflakeAppDeploymentConfiguration)artifact.deploymentConfiguration).connection;
        MutableList<SnowflakeGrantInfo> grants = Lists.mutable.empty();
        try (Connection jdbcConnection = this.getDeploymentConnection(identity, artifact))
        {
            Statement role = jdbcConnection.createStatement();
            ResultSet roleResult = role.executeQuery("SELECT CURRENT_ROLE()");
            roleResult.next();
            String currentRole = roleResult.getString(1);
            roleResult.close();
            Statement s = jdbcConnection.createStatement();
            ResultSet  res = s.executeQuery(String.format("SHOW GRANTS TO ROLE %S LIMIT %S;", currentRole, limit));
            while (res.next())
            {
                grants.add(new SnowflakeGrantInfo(res.getString("privilege"), res.getString("granted_on"), res.getString("name"), res.getString("grantee_name"), res.getString("granted_by")));
            }
            s.close();
        }
        catch (Exception e)
        {
            LOGGER.info("Unable to query for grants for role. Error:  " + e.getMessage());
        }
        return grants;
    }

    public ImmutableList<DeploymentInfo> getDeployed(Identity identity, RelationalDatabaseConnection connection) throws Exception
    {
        ImmutableList<DeploymentInfo> deployments = null;

        LOGGER.info("Querying deployment");
        try (Connection jdbcConnection = this.getDeploymentConnection(identity, connection))
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
