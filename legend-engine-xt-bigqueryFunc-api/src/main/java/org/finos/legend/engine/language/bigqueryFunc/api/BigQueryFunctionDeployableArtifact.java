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

package org.finos.legend.engine.language.bigqueryFunc.api;

import com.google.cloud.bigquery.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class BigQueryFunctionDeployableArtifact
{
    private final String functionName;
    private final String functionAsSQLStatement;
    private final Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification datasourceSpecification;
    private final Root_meta_pure_alloy_connections_alloy_authentication_GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy;

    private BigQueryFunctionDeployableArtifact(
            String functionName,
            String functionAsSQLStatement,
            Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification datasourceSpecification,
            Root_meta_pure_alloy_connections_alloy_authentication_GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy)
    {
        this.functionName = functionName;
        this.functionAsSQLStatement = functionAsSQLStatement;
        this.datasourceSpecification = datasourceSpecification;
        this.authenticationStrategy = authenticationStrategy;
    }

    public static BigQueryFunctionDeployableArtifact buildArtifact(
            PureModel pureModel,
            Root_meta_external_functionActivator_bigQueryFunc_BigQueryFunction bigQueryFunction,
            Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions) throws BigQueryFunctionArtifactValidationException
    {
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan =
                PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) bigQueryFunction._function(), null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        RichIterable<Root_meta_relational_mapping_SQLExecutionNode> sqlExecutionNodes =
                BigQueryFunctionDeployableArtifact.collectAllNodes(executionPlan._rootExecutionNode()).selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class);

        RichIterable<String> sqlExpressions =
                sqlExecutionNodes
                        .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                        .select(x -> !x.toLowerCase().startsWith("alter"));

        if (sqlExpressions.size() != 1)
        {
            throw new BigQueryFunctionArtifactValidationException("BigQuery Function can't be used with a plan containing '" + sqlExpressions.size() + "' SQL expressions", sqlExpressions);
        }

        Root_meta_pure_alloy_connections_RelationalDatabaseConnection relationalDatabaseConnection = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) sqlExecutionNodes.getAny()._connection();
        return new BigQueryFunctionDeployableArtifact(
                bigQueryFunction._functionName(),
                sqlExpressions.getOnly(),
                (Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification) relationalDatabaseConnection._datasourceSpecification(),
                (Root_meta_pure_alloy_connections_alloy_authentication_GCPApplicationDefaultCredentialsAuthenticationStrategy) relationalDatabaseConnection._authenticationStrategy());
    }

    private static RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(BigQueryFunctionDeployableArtifact::collectAllNodes));
    }

    public BigQueryFunctionArtifact toArtifact()
    {
        return new BigQueryFunctionArtifact(this.functionAsSQLStatement);
    }

    // TODO HSO: Handle parameterized SQL statements
    public void deploy() throws SQLException
    {
        String projectId = this.datasourceSpecification._projectId();
        String dataset = this.datasourceSpecification._defaultDataset();

//        BigQueryDatasourceSpecification datasourceSpecification = new BigQueryDatasourceSpecification();
//        datasourceSpecification.defaultDataset = this.datasourceSpecification._defaultDataset();
//        datasourceSpecification.projectId = this.datasourceSpecification._projectId();
//        datasourceSpecification.proxyHost = this.datasourceSpecification._proxyHost();
//        datasourceSpecification.proxyPort = this.datasourceSpecification._proxyPort();
//
//        GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy = new GCPApplicationDefaultCredentialsAuthenticationStrategy();
//
//        RelationalDatabaseConnection connection = new RelationalDatabaseConnection(
//                datasourceSpecification,
//                authenticationStrategy,
//                DatabaseType.BigQuery);
//
//        try (Connection jdbcConnection = connectionManagerSelector.getDatabaseConnection(new Identity("anonymous"), connection))
//        {
//            jdbcConnection.setAutoCommit(false);
//            Statement statement = jdbcConnection.createStatement();
//            String createTableFunctionExpression =
//                    String.format("CREATE OR REPLACE TABLE FUNCTION %s.%s() AS (%s)", this.datasourceSpecification._projectId(), this.functionName, this.functionAsSQLStatement);
//            statement.execute(createTableFunctionExpression);
//            jdbcConnection.commit();
//        }

        BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();

        RoutineId routineId = RoutineId.of(projectId, dataset, this.functionName);

        RoutineInfo routineInfo =
                RoutineInfo
                        .newBuilder(routineId)
                        .setRoutineType("TABLE_VALUED_FUNCTION")
                        .setLanguage("SQL")
                        .setBody(this.functionAsSQLStatement)
                        .build();
        try
        {
            bigQuery.create(routineInfo);
            System.out.println("Created table function!");
        }
        catch (BigQueryException e)
        {
            System.out.println(e);
        }
    }

    public String getFunctionName()
    {
        return this.functionName;
    }
}
