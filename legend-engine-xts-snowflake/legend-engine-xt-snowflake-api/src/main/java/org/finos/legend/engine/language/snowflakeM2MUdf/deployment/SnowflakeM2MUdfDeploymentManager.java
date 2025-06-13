// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.snowflakeM2MUdf.deployment;

import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.language.snowflakeM2MUdf.generator.SnowflakeM2MUdfGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentDetails;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfContent;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


public class SnowflakeM2MUdfDeploymentManager implements DeploymentManager<SnowflakeM2MUdfArtifact, SnowflakeM2MUdfDeploymentResult, SnowflakeM2MUdfDeploymentConfiguration, FunctionActivatorDeploymentDetails, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeM2MUdfDeploymentManager.class);

    private PlanExecutor planExecutor;
    private ConnectionManagerSelector connectionManager;
    private Function<String, String> engineDownloadUrlProvider;

    public SnowflakeM2MUdfDeploymentManager(PlanExecutor planExecutor, Function<String, String> engineDownloadUrlProvider)
    {
        this.engineDownloadUrlProvider = engineDownloadUrlProvider;
        this.planExecutor = planExecutor;
        connectionManager = ((RelationalStoreState)planExecutor.getExtraExecutors().select(c -> c instanceof RelationalStoreExecutor).getFirst().getStoreState()).getRelationalExecutor().getConnectionManager();
    }

    public List<SnowflakeM2MUdfDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return org.eclipse.collections.api.factory.Lists.mutable.withAll(availableConfigs).selectInstancesOf(SnowflakeM2MUdfDeploymentConfiguration.class);
    }

    @Override
    public boolean canDeploy(FunctionActivatorArtifact artifact)
    {
        return artifact instanceof SnowflakeM2MUdfArtifact;
    }

    @Override
    public SnowflakeM2MUdfDeploymentResult deploy(Identity identity, SnowflakeM2MUdfArtifact artifact)
    {
        return deploy(identity, artifact, Lists.mutable.empty());
    }

    @Override
    public SnowflakeM2MUdfDeploymentResult deploy(Identity identity, SnowflakeM2MUdfArtifact artifact, List<SnowflakeM2MUdfDeploymentConfiguration> availableRuntimeConfigurations)
    {
        LOGGER.info("Starting SnowflakeM2MUDf deployment for " + ((SnowflakeM2MUdfContent)artifact.content).udfName);
        SnowflakeM2MUdfDeploymentResult result;

        //use the system connection if available (as would be the case in sandbox flow) , else use artifact connection (production flow)
        try (Connection jdbcConnection = availableRuntimeConfigurations.isEmpty() ? this.getDeploymentConnection(identity, artifact) : this.getDeploymentConnection(identity, availableRuntimeConfigurations.get(0).connection))
        {
            String udfName = ((SnowflakeM2MUdfContent)artifact.content).udfName;
            jdbcConnection.setAutoCommit(false);
            this.deployImpl(jdbcConnection, (SnowflakeM2MUdfContent)artifact.content);
            jdbcConnection.commit();
            LOGGER.info("Completed deployment successfully");
            result = new SnowflakeM2MUdfDeploymentResult(udfName, true, artifact.deployedLocation);
        }
        catch (Exception e)
        {
            LOGGER.info("Completed deployment with error");
            result = new SnowflakeM2MUdfDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
        return result;
    }

    @Override
    public FunctionActivatorDeploymentDetails getActivatorDetails(Identity identity, SnowflakeM2MUdfDeploymentConfiguration runtimeConfig, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator)
    {
        return new FunctionActivatorDeploymentDetails();
    }

    public Connection getDeploymentConnection(Identity identity, RelationalDatabaseConnection connection)
    {
        return this.connectionManager.getDatabaseConnection(identity, connection);
    }

    public void deployImpl(Connection jdbcConnection, SnowflakeM2MUdfContent content) throws SQLException
    {
        String engineDownloadUrl = this.engineDownloadUrlProvider.apply(content.engineVersion);
        try
        {
            downloadFile(engineDownloadUrl,SnowflakeM2MUdfGenerator.EXECUTION_JAR_FILENAME);
            writeStringToFile(SnowflakeM2MUdfGenerator.EXECUTION_PLAN_FILENAME,content.executionPlan);
            int i = 0;

            for (String s: content.sqlCommands)
            {
                Statement statement = jdbcConnection.createStatement();
                statement.execute(s);
                i++;
                LOGGER.info("{} sql execution successfull", i);
            }
        }
        catch (SQLException e)
        {
            LOGGER.info("Error executing the sql commands for deployment");
            throw e;
        }
        finally
        {
            deleteFile(SnowflakeM2MUdfGenerator.EXECUTION_JAR_FILENAME);
            deleteFile(SnowflakeM2MUdfGenerator.EXECUTION_PLAN_FILENAME);
        }
    }

    public Connection getDeploymentConnection(Identity identity, SnowflakeM2MUdfArtifact artifact)
    {
        RelationalDatabaseConnection connection = extractConnectionFromArtifact(artifact);
        return this.connectionManager.getDatabaseConnection(identity, connection);
    }

    public RelationalDatabaseConnection extractConnectionFromArtifact(SnowflakeM2MUdfArtifact artifact)
    {
        return ((SnowflakeM2MUdfDeploymentConfiguration)artifact.deploymentConfiguration).connection;
    }

    private static void downloadFile(String url, String fileName)
    {
        try
        {
            FileUtils.copyURLToFile(new URL(url), new File(fileName));
        }
        catch (Exception e)
        {
            LOGGER.info("Not able to download the file");
            throw new RuntimeException(e);
        }
    }

    private static void deleteFile(String fileName)
    {
        try
        {
            Files.deleteIfExists(Paths.get(fileName));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeStringToFile(String fileName, String content)
    {
        try
        {
            Files.write(Paths.get(fileName), content.getBytes());
        }
        catch (Exception e)
        {
            LOGGER.info("Not able to convert string execution plan to temp file");
            throw new RuntimeException(e);
        }
    }

}
