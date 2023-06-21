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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppProtocolExtension;
import org.finos.legend.pure.generated.Root_meta_external_functionActivator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionNode;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_SQLExecutionNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;

public class SnowflakeAppService implements FunctionActivatorService<Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp>
{
    private ConnectionManagerSelector connectionManager;
    private  SnowflakeAppDeploymentTool snowflakeDeploymentTool;

    public SnowflakeAppService()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
        this.snowflakeDeploymentTool = new SnowflakeAppDeploymentTool(connectionManager);
    }

    public SnowflakeAppService(ConnectionManagerSelector connectionManager)
    {
        this.connectionManager = connectionManager;
        this.snowflakeDeploymentTool = new SnowflakeAppDeploymentTool(connectionManager);
    }

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
    public MutableList<? extends FunctionActivatorError> validate(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RichIterable<String> sqlExpressions = extractSQLExpressions(pureModel, activator, routerExtensions);
        return sqlExpressions.size() != 1 ?
                Lists.mutable.with(new SnowflakeAppError("SnowflakeApp can't be used with a plan containing '" + sqlExpressions.size() + "' SQL expressions", sqlExpressions.toList())) :
                Lists.mutable.empty();

    }

    @Override
    public MutableList<? extends FunctionActivatorError> publishToSandbox(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        Object[] objects = this.extractSQLExpressionsAndConnectionMetadata(pureModel, activator, routerExtensions);
        RichIterable<String> sqlExpressions = (RichIterable<String>) objects[0];

        Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification ds  = (Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification) objects[1];
        Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy as = (Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy) objects[2];

        String applicationName = activator._applicationName();
        try
        {
            this.snowflakeDeploymentTool.deploy(ds, as, applicationName);
            return Lists.mutable.empty();
        }
        catch (Exception e)
        {
            FunctionActivatorError functionActivatorError = new FunctionActivatorError(e.getMessage());
            return Lists.mutable.with(functionActivatorError);
        }
    }

    @Override
    public SnowflakeAppArtifact renderArtifact(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RichIterable<String> sqlExpressions = extractSQLExpressions(pureModel, activator, routerExtensions);
        return new SnowflakeAppArtifact(sqlExpressions);
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

    private Object[] extractSQLExpressionsAndConnectionMetadata(PureModel pureModel, Root_meta_external_functionActivator_snowflakeApp_SnowflakeApp activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        PackageableFunction<?> function = activator._function();
        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure((FunctionDefinition<?>) function, null, null, null, pureModel, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel));
        Root_meta_pure_executionPlan_ExecutionNode node = executionPlan._rootExecutionNode();

        RichIterable<String> expressions = collectAllNodes(node)
                .selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .collect(Root_meta_relational_mapping_SQLExecutionNode::_sqlQuery)
                .select(x -> !x.toLowerCase().startsWith("alter"));

        Root_meta_pure_alloy_connections_RelationalDatabaseConnection relCOnn = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection)collectAllNodes(node).selectInstancesOf(Root_meta_relational_mapping_SQLExecutionNode.class)
                .getAny()
                ._connection();
        Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification ds = (Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification) relCOnn._datasourceSpecification();
        Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy as = (Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy) relCOnn._authenticationStrategy();

        return new Object[]{expressions, ds, as};
    }

    private RichIterable<Root_meta_pure_executionPlan_ExecutionNode> collectAllNodes(Root_meta_pure_executionPlan_ExecutionNode node)
    {
        return Lists.mutable.with(node).withAll(node._executionNodes().flatCollect(this::collectAllNodes));
    }

}
