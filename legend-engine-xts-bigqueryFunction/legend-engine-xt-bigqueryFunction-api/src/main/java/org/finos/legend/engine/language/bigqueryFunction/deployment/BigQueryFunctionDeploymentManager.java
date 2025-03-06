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

package org.finos.legend.engine.language.bigqueryFunction.deployment;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.RoutineId;
import com.google.cloud.bigquery.RoutineInfo;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.functionActivator.deployment.DeploymentManager;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionArtifact;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionContent;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionDeploymentResult;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentDetails;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * These deployment functions assume that the artifact has already been validated.
 */
public class BigQueryFunctionDeploymentManager implements DeploymentManager<BigQueryFunctionArtifact, BigQueryFunctionDeploymentResult, BigQueryFunctionDeploymentConfiguration, FunctionActivatorDeploymentDetails, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryFunctionDeploymentManager.class);

    public List<BigQueryFunctionDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> availableConfigs)
    {
        return Lists.mutable.withAll(availableConfigs).selectInstancesOf(BigQueryFunctionDeploymentConfiguration.class);
    }

    @Override
    public boolean canDeploy(FunctionActivatorArtifact activatorArtifact)
    {
        return activatorArtifact instanceof BigQueryFunctionArtifact;
    }

    @Override
    public BigQueryFunctionDeploymentResult deploy(Identity identity, BigQueryFunctionArtifact artifact)
    {
        return new BigQueryFunctionDeploymentResult("", false);
    }

    @Override
    public BigQueryFunctionDeploymentResult deploy(Identity identity, BigQueryFunctionArtifact artifact, List<BigQueryFunctionDeploymentConfiguration> availableRuntimeConfigurations)
    {
        return new BigQueryFunctionDeploymentResult("", false);
    }

    @Override
    public FunctionActivatorDeploymentDetails getActivatorDetails(Identity identity, BigQueryFunctionDeploymentConfiguration runtimeConfig, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator)
    {
        return new FunctionActivatorDeploymentDetails();
    }

    public BigQueryFunctionDeploymentResult deployImpl(BigQueryFunctionArtifact artifact, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunctionDeploymentConfiguration deploymentConfiguration)
    {
        LOGGER.info("Starting deployment");
        BigQueryFunctionDeploymentResult result;
        try
        {
            Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification datasourceSpecification = (Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification) deploymentConfiguration._target()._datasourceSpecification();
            String dataset = datasourceSpecification._defaultDataset();
            String projectId = datasourceSpecification._projectId();

            BigQueryFunctionContent functionContent = (BigQueryFunctionContent) artifact.content;
            BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();
            RoutineId routineId = RoutineId.of(projectId, dataset, functionContent.functionName);

            String sqlExpression = Iterate.getOnly(functionContent.sqlExpressions);
            String sourceProjectId = artifact.sourceProjectId;
            String sourceDefaultDataset = artifact.sourceDefaultDataset;
            // TODO: Include projectId in core relational BigQuery SQL statement construction
            String fullyQualifiedSqlExpression = sqlExpression.replace(sourceDefaultDataset, String.format("`%s.%s`", sourceProjectId, sourceDefaultDataset));
            RoutineInfo routineInfo =
                    RoutineInfo
                            .newBuilder(routineId)
                            .setRoutineType("TABLE_VALUED_FUNCTION")
                            .setLanguage("SQL")
                            .setBody(fullyQualifiedSqlExpression)
                            .build();
            bigQuery.create(routineInfo);

            LOGGER.info("Completed deployment successfully");
            result = new BigQueryFunctionDeploymentResult(functionContent.functionName, true);
        }
        catch (Exception e)
        {
            LOGGER.info("Completed deployment with error");
            result = new BigQueryFunctionDeploymentResult(Lists.mutable.with(e.getMessage()));
        }
        return result;
    }
}
