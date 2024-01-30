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

package org.finos.legend.engine.language.memsql.deployment;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionContent;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentResult;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * These deployment functions assume that the artifact has already been validated.
 */
public class MemSqlFunctionDeploymentManager implements DeploymentManager<MemSqlFunctionArtifact, MemSqlFunctionDeploymentResult, MemSqlFunctionDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MemSqlFunctionDeploymentManager.class);

    private PlanExecutor planExecutor;
    private ConnectionManagerSelector connectionManager;

    public MemSqlFunctionDeploymentManager(PlanExecutor executor)
    {
        this.planExecutor = planExecutor;
        connectionManager = ((RelationalStoreState)planExecutor.getExtraExecutors().select(c -> c instanceof RelationalStoreExecutor).getFirst().getStoreState()).getRelationalExecutor().getConnectionManager();
    }

    @Override
    public boolean canDeploy(FunctionActivatorArtifact activatorArtifact)
    {
        return activatorArtifact instanceof MemSqlFunctionArtifact;
    }

    @Override
    public MemSqlFunctionDeploymentResult deploy(Identity identity, MemSqlFunctionArtifact artifact)
    {
        return new MemSqlFunctionDeploymentResult("", false);
    }

    @Override
    public MemSqlFunctionDeploymentResult deploy(Identity identity, MemSqlFunctionArtifact artifact, List<MemSqlFunctionDeploymentConfiguration> availableRuntimeConfigurations)
    {
        MemSqlFunctionDeploymentResult result = null;
        String functionName = ((MemSqlFunctionContent)artifact.content).functionName;

        try (Connection jdbcConnection = availableRuntimeConfigurations.isEmpty() ? this.getDeploymentConnection(identity, artifact) : this.getDeploymentConnection(identity, availableRuntimeConfigurations.get(0).connection))
        {
            jdbcConnection.setAutoCommit(false);
            this.deployImpl(jdbcConnection, (MemSqlFunctionContent)artifact.content);
            jdbcConnection.commit();
            LOGGER.info("Completed deployment successfully");
            result = new MemSqlFunctionDeploymentResult(functionName, true);
        }
        catch (Exception e)
        {
            LOGGER.info("Completed deployment with error");
            result = new MemSqlFunctionDeploymentResult(functionName, true);
        }
        return result;
   }

    public void deployImpl(Connection jdbcConnection, MemSqlFunctionContent memSqlFunctionContent) throws SQLException
    {
        MutableList<String> statements = createFunctionStatements(memSqlFunctionContent);
        for (String s: statements)
        {
            Statement statement = jdbcConnection.createStatement();
            statement.execute(s);
        }
    }


    private Connection getDeploymentConnection(Identity identity, MemSqlFunctionArtifact artifact)
    {
        RelationalDatabaseConnection connection = extractConnectionFromArtifact(artifact);
        return this.connectionManager.getDatabaseConnection(identity, connection);
    }

    public Connection getDeploymentConnection(Identity identity, RelationalDatabaseConnection connection)
    {
        return this.connectionManager.getDatabaseConnection(identity, (DatabaseConnection) connection);
    }

    private RelationalDatabaseConnection extractConnectionFromArtifact(MemSqlFunctionArtifact artifact)
    {
        return ((MemSqlFunctionDeploymentConfiguration)artifact.deploymentConfiguration).connection;
    }

    public MutableList<String> createFunctionStatements(MemSqlFunctionContent content)
    {
        MutableList<String> statements = org.eclipse.collections.impl.factory.Lists.mutable.empty();
        statements.add(String.format("CREATE OR REPLACE FUNCTION %S RETURNS TABLE  AS RETURN %s ;", content.functionName, content.sqlExpressions.getOnly()));
        return statements;
    }
}
