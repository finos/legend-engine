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
import org.finos.legend.engine.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppArtifact;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppContent;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppGenerator;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentStage;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeDeploymentConfiguration;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeDeploymentManager;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeDeploymentResult;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppProtocolExtension;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionNode;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_SQLExecutionNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

public class SnowflakeAppService implements FunctionActivatorService<Root_meta_external_function_activator_snowflakeApp_SnowflakeApp, SnowflakeAppDeploymentConfiguration, SnowflakeDeploymentResult>
{
    private ConnectionManagerSelector connectionManager;
    private SnowflakeDeploymentManager snowflakeDeploymentManager;

    public SnowflakeAppService()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
        this.snowflakeDeploymentManager = new SnowflakeDeploymentManager(new SnowflakeAppDeploymentTool(connectionManager));
    }

    public SnowflakeAppService(ConnectionManagerSelector connectionManager)
    {
        this.connectionManager = connectionManager;
        this.snowflakeDeploymentManager = new SnowflakeDeploymentManager(new SnowflakeAppDeploymentTool(connectionManager));
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Snowflake App",
                "Create a SnowflakeApp that can activate the function in Snowflake. It then can be used in SQL expressions and be shared with other accounts",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::snowflakeApp::SnowflakeApp",
                SnowflakeAppProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(MutableList<CommonProfile> profiles, PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        SnowflakeAppArtifact artifact = SnowflakeAppGenerator.generateArtifact(pureModel, activator, routerExtensions);
        int size = ((SnowflakeAppContent)artifact.content).sqlExpressions.size();
        return size != 1 ?
                Lists.mutable.with(new SnowflakeAppError("SnowflakeApp can't be used with a plan containing '" + size + "' SQL expressions", ((SnowflakeAppContent)artifact.content).sqlExpressions)) :
                Lists.mutable.empty();

    }

    @Override
    public SnowflakeDeploymentResult publishToSandbox(MutableList<CommonProfile> profiles, PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, List<SnowflakeAppDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        SnowflakeAppArtifact artifact = SnowflakeAppGenerator.generateArtifact(pureModel, activator, routerExtensions);
        return this.snowflakeDeploymentManager.deploy(profiles, artifact, runtimeConfigurations);
    }

    @Override
    public SnowflakeAppArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return SnowflakeAppGenerator.generateArtifact(pureModel, activator, routerExtensions);
    }

    @Override
    public List<SnowflakeAppDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof SnowflakeAppDeploymentConfiguration).collect(e -> (SnowflakeAppDeploymentConfiguration) e);
    }


}
