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
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionContent;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionDeploymentResult;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_memSqlFunction_MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * These deployment functions assume that the artifact has already been validated.
 */
public class MemSqlFunctionDeploymentManager implements DeploymentManager<MemSqlFunctionArtifact, MemSqlFunctionDeploymentResult, MemSqlFunctionDeploymentConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MemSqlFunctionDeploymentManager.class);

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
        return new MemSqlFunctionDeploymentResult("", false);
    }

    public MemSqlFunctionDeploymentResult deployImpl(MemSqlFunctionArtifact artifact, Root_meta_external_function_activator_memSqlFunction_MemSqlFunctionDeploymentConfiguration deploymentConfiguration)
    {

        MemSqlFunctionDeploymentResult result = null;
        String functionName = ((MemSqlFunctionContent) artifact.content).functionName;

        try
        {
            Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification datasourceSpecification = (Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification) deploymentConfiguration._target()._datasourceSpecification();

            String connectionString = String.format("jdbc:mariadb://%s:%s/%s?useSsl=true&user=%s&password=%s", datasourceSpecification._host(), datasourceSpecification._port(), datasourceSpecification._databaseName(), datasourceSpecification._username(), datasourceSpecification._password());

            Connection connection = DriverManager.getConnection(connectionString);
            MemSqlFunctionContent functionContent = (MemSqlFunctionContent) artifact.content;
            //MutableList<String> statements = createFunctionStatements(functionContent);
            for (String s : functionContent.sqlExpressions)
            {
                Statement statement = connection.createStatement();
                statement.execute(s);
            }
            LOGGER.info("Completed deployment successfully");
            result = new MemSqlFunctionDeploymentResult(functionName, true);
        }
        catch (SQLException e)
        {
            LOGGER.info("Completed deployment with error");
            result = new MemSqlFunctionDeploymentResult(functionName, true);
        }
        return result;
    }

    private RelationalDatabaseConnection extractConnectionFromArtifact(MemSqlFunctionArtifact artifact)
    {
        return ((MemSqlFunctionDeploymentConfiguration) artifact.deploymentConfiguration).connection;
    }

    public MutableList<String> createFunctionStatements(MemSqlFunctionContent content)
    {
        MutableList<String> statements = org.eclipse.collections.impl.factory.Lists.mutable.empty();
        statements.add(String.format("CREATE OR REPLACE FUNCTION %S RETURNS TABLE  AS RETURN %s ;", content.functionName, content.sqlExpressions.getOnly()));
        return statements;
    }
}
