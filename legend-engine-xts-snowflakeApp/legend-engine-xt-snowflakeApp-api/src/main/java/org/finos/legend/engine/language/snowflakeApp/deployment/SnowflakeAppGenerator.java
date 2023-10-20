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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionNode;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_SQLExecutionNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;

public class SnowflakeAppGenerator
{

    public static SnowflakeAppArtifact generateArtifact(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, Function<PureModel,RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RichIterable<String> sqlExpressions = extractSQLExpressions(pureModel, activator, routerExtensions);
        return new SnowflakeAppArtifact(activator._applicationName(), Lists.mutable.withAll(sqlExpressions));
    }

    private static RichIterable<String> extractSQLExpressions(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        PackageableFunction<?> function = activator._function();
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) function, null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        Root_meta_pure_executionPlan_ExecutionNode node = executionPlan._rootExecutionNode();
        return collectAllNodes(node)
                .selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                .select(x -> !x.toLowerCase().startsWith("alter"));
    }

    private static Object[] extractSQLExpressionsAndConnectionMetadata(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
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
        Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification ds = (Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification) relCOnn._datasourceSpecification();
        Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy as = (Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy) relCOnn._authenticationStrategy();

        return new Object[]{expressions, ds, as};
    }

    private static RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(SnowflakeAppGenerator::collectAllNodes));
    }
}
