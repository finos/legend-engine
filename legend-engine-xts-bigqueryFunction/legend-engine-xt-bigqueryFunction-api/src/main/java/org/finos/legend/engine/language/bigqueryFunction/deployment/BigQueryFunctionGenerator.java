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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionArtifact;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;

public class BigQueryFunctionGenerator
{
    public static BigQueryFunctionArtifact generateArtifact(PureModel pureModel, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        Pair<Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification, RichIterable<String>> artifactDetails = extractArtifactDetails(pureModel, activator, routerExtensions);
        Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification bigQueryDatasourceSpecification = artifactDetails.getOne();
        RichIterable<String> sqlExpressions = artifactDetails.getTwo();
        return new BigQueryFunctionArtifact(activator._functionName(), Lists.mutable.withAll(sqlExpressions), bigQueryDatasourceSpecification._projectId(), bigQueryDatasourceSpecification._defaultDataset(), (AlloySDLC) ((PureModelContextData)inputModel).origin.sdlcInfo);
    }

    private static Pair<Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification, RichIterable<String>> extractArtifactDetails(PureModel pureModel, Root_meta_external_function_activator_bigQueryFunction_BigQueryFunction activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        PackageableFunction<?> function = activator._function();
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) function, null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        RichIterable<Root_meta_relational_mapping_SQLExecutionNode> sqlExecutionNodes =
                collectAllNodes(executionPlan._rootExecutionNode()).selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class);

        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection relationalDatabaseConnection = (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) sqlExecutionNodes.getAny()._connection();
        Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification bigQueryDatasourceSpecification = ((Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification) relationalDatabaseConnection._datasourceSpecification());

        return Tuples.pair(
                bigQueryDatasourceSpecification,
                sqlExecutionNodes
                    .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                    .select(x -> !x.toLowerCase().startsWith("alter")));
    }

    private static RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(BigQueryFunctionGenerator::collectAllNodes));
    }
}
