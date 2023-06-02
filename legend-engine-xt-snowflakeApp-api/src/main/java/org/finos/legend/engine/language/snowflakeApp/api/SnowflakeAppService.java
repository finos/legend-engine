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

package org.finos.legend.engine.language.snowflakeApp.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppProtocolExtension;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;

public class SnowflakeAppService implements FunctionActivatorService<Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp>
{
    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Snowflake App",
                "Create a SnowflakeApp that can activate the function in Snowflake. It then can be used in SQL expressions and be shared with other accounts",
                "meta::protocols::pure::" + version + "::metamodel::functionActivator::snowflakeApp::SnowflakeApp",
                SnowflakeAppProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_functionActivator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RichIterable<String> sqlExpressions = extractSQLExpressions(pureModel, activator, routerExtensions);
        return sqlExpressions.size() != 1 ?
                Lists.mutable.with(new SnowflakeAppError("SnowflakeApp can't be used with a plan containing '" + sqlExpressions.size() + "' SQL expressions", sqlExpressions.toList())) :
                Lists.mutable.empty();

    }

    @Override
    public MutableList<? extends FunctionActivatorError> publishToSandbox(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RichIterable<String> sqlExpressions = extractSQLExpressions(pureModel, activator, routerExtensions);
        return Lists.mutable.empty();
    }

    private RichIterable<String> extractSQLExpressions(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        PackageableFunction<?> function = activator._function();
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) function, null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        Root_meta_pure_executionPlan_ExecutionNode node = executionPlan._rootExecutionNode();
        return collectAllNodes(node)
                .selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                .select(x -> !x.toLowerCase().startsWith("alter"));
    }

    private RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(this::collectAllNodes));
    }
}
