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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorResult;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorValidator;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeGrantInfo;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppArtifact;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeAppDeploymentManager;
import org.finos.legend.engine.language.snowflakeApp.deployment.SnowflakeDeploymentResult;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppContent;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.language.snowflakeApp.generator.SnowflakeAppGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppProtocolExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.util.List;

public class SnowflakeAppService implements FunctionActivatorService<Root_meta_external_function_activator_snowflakeApp_SnowflakeApp, SnowflakeAppDeploymentConfiguration, SnowflakeDeploymentResult>
{
    private ConnectionManagerSelector connectionManager;
    private SnowflakeAppDeploymentManager snowflakeDeploymentManager;
    private MutableList<FunctionActivatorValidator> extraValidators = Lists.mutable.empty();

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Snowflake");
    }

    public SnowflakeAppService()
    {
        TemporaryTestDbConfiguration conf = new TemporaryTestDbConfiguration();
        conf.port = Integer.parseInt(System.getProperty("h2ServerPort", "1234"));
        this.connectionManager = new ConnectionManagerSelector(conf, FastList.newList());
        this.snowflakeDeploymentManager = new SnowflakeAppDeploymentManager(new SnowflakeAppDeploymentTool(connectionManager));
    }

    public SnowflakeAppService(ConnectionManagerSelector connectionManager)
    {
        this.connectionManager = connectionManager;
        this.snowflakeDeploymentManager = new SnowflakeAppDeploymentManager(new SnowflakeAppDeploymentTool(connectionManager));
    }

    public SnowflakeAppService(PlanExecutor executor)
    {
        this.snowflakeDeploymentManager = new SnowflakeAppDeploymentManager(executor);
    }

    public SnowflakeAppService(PlanExecutor executor, List<FunctionActivatorValidator> extraValidators)
    {
        this.snowflakeDeploymentManager = new SnowflakeAppDeploymentManager(executor);
        this.extraValidators = Lists.mutable.withAll(extraValidators);
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
    public FunctionActivatorResult validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, List<SnowflakeAppDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        SnowflakeAppArtifact artifact = SnowflakeAppGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
        FunctionActivatorResult errors = validate(identity, artifact);
        this.extraValidators.select(v -> v.supports(activator)).forEach(v -> errors.getErrors().addAll(v.validate(identity, activator)));
        return errors;
    }

    public FunctionActivatorResult validate(Identity identity, SnowflakeAppArtifact artifact)
    {
        MutableList<FunctionActivatorError> errors = Lists.mutable.empty();
        SnowflakeAppContent content = (SnowflakeAppContent)artifact.content;
        if (content.applicationName.trim().equals(""))
        {
           errors.add(new SnowflakeAppError("Application name cannot be empty"));
        }
        if (!StringUtils.isAlphanumeric(content.applicationName))
        {
            errors.add(new SnowflakeAppError("Application name can only contain letters and digits"));
        }
        if (content.deploymentSchema.trim().equals(""))
        {
            errors.add(new SnowflakeAppError("Deployment Schema cannot be empty"));
        }
        if (!content.deploymentSchema.matches("[a-zA-Z0-9_]+"))
        {
            errors.add(new SnowflakeAppError("Deployment schema can only contain letters, digits and underscore"));
        }
        if (!content.sqlExpressions.isEmpty())
        {
            int size = content.sqlExpressions.select(e -> !e.toLowerCase().endsWith("to role public;")).size();
            if (size != 1)
            {
                errors.add(new SnowflakeAppError("SnowflakeApp can't be used with a plan containing '" + size + "' SQL expressions", content.sqlExpressions));
            }
        }
        if (content.permissionScope.equals("SEQUESTERED"))
        {
            MutableList<SnowflakeGrantInfo> grantsToRole = this.snowflakeDeploymentManager.getGrants(identity, artifact);
            MutableList<String> contentTables = ((SnowflakeAppContent) artifact.content).usedTables.collect(String::toUpperCase);
            MutableList<SnowflakeGrantInfo> violations = grantsToRole.select(g -> g.privilege.equals("SELECT") && contentTables.contains(g.objectName.toUpperCase()));
            if (!violations.isEmpty())
            {
                errors.add(new SnowflakeAppError("Privilege Violation for SEQUESTERED scheme. Deployment Role contains SELECT permissions on tables: " + violations.collect(v -> v.objectName).makeString("[", ",", "]")));
            }
        }
        return new FunctionActivatorResult(errors);
    }

    @Override
    public SnowflakeDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, List<SnowflakeAppDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        SnowflakeAppArtifact artifact = SnowflakeAppGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
        MutableList<? extends  FunctionActivatorError> validationError = validate(identity, artifact).getErrors();
        if (validationError.isEmpty())
        {
            return this.snowflakeDeploymentManager.deploy(identity, artifact, runtimeConfigurations);
        }
        return new SnowflakeDeploymentResult(validationError.collect(v -> v.message));

    }

    @Override
    public SnowflakeAppArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return SnowflakeAppGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_snowflakeApp_SnowflakeApp activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return SnowflakeAppGenerator.generateFunctionLineage(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public List<SnowflakeAppDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof SnowflakeAppDeploymentConfiguration).collect(e -> (SnowflakeAppDeploymentConfiguration) e);
    }


}
