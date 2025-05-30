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


package org.finos.legend.engine.language.memsqlFunction.generator;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionArtifact;
import org.finos.legend.engine.protocol.memsqlFunction.deployment.MemSqlFunctionContent;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.MemSqlDatasourceSpecification;

import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;

public class MemSqlFunctionGenerator
{

    public static MemSqlFunctionArtifact generateArtifact(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        String sqlFunctionExpression = core_memsqlfunction_generation_generation.Root_meta_external_function_activator_memSqlFunction_generation_generateArtifact_MemSqlFunction_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());

        RelationalDatabaseConnection connection;
        AlloySDLC sdlc = null;
        if (((PureModelContextData)inputModel).getOrigin() != null)
        {
            SDLC sdlcInfo = ((PureModelContextData)inputModel).origin.sdlcInfo;
            if (sdlcInfo instanceof AlloySDLC)
            {
                sdlc = (AlloySDLC) sdlcInfo;
            }
        }
        MemSqlFunctionContent content = new MemSqlFunctionContent(activator._functionName(), Lists.mutable.of(sqlFunctionExpression));
        if (activator._activationConfiguration() != null)
        {
            //identify connection
            MemSqlFunction protocolActivator = Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(MemSqlFunction.class))
                    .select(c -> c.getPath().equals(platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(activator, pureModel.getExecutionSupport())))
                    .getFirst();
            connection   = (RelationalDatabaseConnection) Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(PackageableConnection.class))
                    .select(c -> c.getPath().equals(((org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionDeploymentConfiguration)protocolActivator.activationConfiguration).activationConnection.connection)).getFirst().connectionValue;
//            MemSqlDatasourceSpecification ds = (MemSqlDatasourceSpecification)connection.datasourceSpecification;

            return new MemSqlFunctionArtifact(content.functionName, Lists.mutable.of(sqlFunctionExpression));
        }

        return new MemSqlFunctionArtifact(content.functionName, Lists.mutable.of(sqlFunctionExpression));
    }

    public static String generateLineage(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return core_memsqlfunction_generation_generation.Root_meta_external_function_activator_memSqlFunction_generation_computeLineage_MemSqlFunction_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
    }

    private static RichIterable<String> extractSQLExpressions(Root_meta_pure_executionPlan_ExecutionPlan executionPlan)
    {

        Root_meta_pure_executionPlan_ExecutionNode node = executionPlan._rootExecutionNode();
        return collectAllNodes(node)
                .selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                .select(x -> !x.toLowerCase().startsWith("alter"));
    }

    private static String generateFunctionReturnColumns(Root_meta_pure_executionPlan_TDSResultType planResult)
    {
        return Lists.mutable.withAll(planResult._tdsColumns()).collect(c ->
                c._name().replace(" ","_").replace("/","_")  + " " +  "VARCHAR(16777216)").makeString(" , ");
    }

    private static Object[] extractSQLExpressionsAndConnectionMetadata(PureModel pureModel, Root_meta_external_function_activator_memSqlFunction_MemSqlFunction activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        PackageableFunction<?> function = activator._function();
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) function, null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        Root_meta_pure_executionPlan_ExecutionNode node = executionPlan._rootExecutionNode();

        RichIterable<String> expressions = collectAllNodes(node)
                .selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                .select(x -> !x.toLowerCase().startsWith("alter"));

        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection relCOnn = (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection)collectAllNodes(node).selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .getAny()
                ._connection();
        Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification ds = (Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification) relCOnn._datasourceSpecification();
        Root_meta_pure_alloy_connections_alloy_authentication_MemsqlPublicAuthenticationStrategy as = (Root_meta_pure_alloy_connections_alloy_authentication_MemsqlPublicAuthenticationStrategy) relCOnn._authenticationStrategy();

        return new Object[]{expressions, ds, as};
    }

    private RelationalDatabaseConnection adaptConnection(Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification datasourceSpecification, Root_meta_pure_alloy_connections_alloy_specification_MemsqlDatasourceSpecification authenticationStrategy)
    {
        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();

        MemSqlDatasourceSpecification snowflakeDatasourceSpecification = new MemSqlDatasourceSpecification();
        snowflakeDatasourceSpecification.databaseName = datasourceSpecification._databaseName();
        snowflakeDatasourceSpecification.host = datasourceSpecification._host();
        //snowflakeDatasourceSpecification.port = datasourceSpecification._port();
        snowflakeDatasourceSpecification.useSsl = datasourceSpecification._useSsl();

//        ReAuthe snowflakeAuthenticationStrategy = new SnowflakePublicAuthenticationStrategy();
//        snowflakeAuthenticationStrategy.privateKeyVaultReference = authenticationStrategy._privateKeyVaultReference();
//        snowflakeAuthenticationStrategy.passPhraseVaultReference = authenticationStrategy._passPhraseVaultReference();
//        snowflakeAuthenticationStrategy.publicUserName = authenticationStrategy._publicUserName();
//
//        connection.authenticationStrategy = snowflakeAuthenticationStrategy;
//        connection.datasourceSpecification = snowflakeDatasourceSpecification;
//        connection.type = DatabaseType.Snowflake;

        return connection;
    }

    private static RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(MemSqlFunctionGenerator::collectAllNodes));
    }
}
