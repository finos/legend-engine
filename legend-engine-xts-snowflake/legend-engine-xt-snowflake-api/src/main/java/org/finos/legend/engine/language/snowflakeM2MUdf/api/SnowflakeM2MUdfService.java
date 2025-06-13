// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.snowflakeM2MUdf.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorResult;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentManager;
import org.finos.legend.engine.language.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentResult;
import org.finos.legend.engine.language.snowflakeM2MUdf.generator.SnowflakeM2MUdfGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.snowflake.SnowflakeProtocolExtension;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_snowflake_core_snowflakem2mUdf_validation_validation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;

import java.util.List;
import java.util.ServiceLoader;

public class SnowflakeM2MUdfService implements FunctionActivatorService<Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf, SnowflakeM2MUdfDeploymentConfiguration, SnowflakeM2MUdfDeploymentResult>
{
    private SnowflakeM2MUdfDeploymentManager snowflakeDeploymentManager;
    private MutableList<FunctionActivatorValidator> extraValidators = Lists.mutable.empty();

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Function_Activator", "Snowflake");
    }

    public SnowflakeM2MUdfService()
    {
        //To Run SnowflakeM2MUdf Validation test
    }

    public SnowflakeM2MUdfService(PlanExecutor executor, Function<String, String> engineDownloadUrlProvider)
    {
        this.snowflakeDeploymentManager = new SnowflakeM2MUdfDeploymentManager(executor,engineDownloadUrlProvider);
    }

    public SnowflakeM2MUdfService(PlanExecutor executor, List<FunctionActivatorValidator> extraValidators, Function<String, String> engineDownloadUrlProvider)
    {
        this.snowflakeDeploymentManager = new SnowflakeM2MUdfDeploymentManager(executor,engineDownloadUrlProvider);
        this.extraValidators = Lists.mutable.withAll(extraValidators);
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Snowflake M2MUdf",
                "Create a scalar UDF function which can be applied on a VARIANT column to execute the M2M transform",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::snowflakeM2MUdf::SnowflakeM2MUdf",
                SnowflakeProtocolExtension.snowflakeM2MUdfPackageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf;
    }

    @Override
    public FunctionActivatorResult validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, PureModelContext inputModel, List<SnowflakeM2MUdfDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<PlanGeneratorExtension> generatorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(
                (FunctionDefinition<?>) activator._function(),
                null,
                null,
                null,
                pureModel,
                PureClientVersions.production,
                PlanPlatform.JAVA,
                null,
                routerExtensions.apply(pureModel),
                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
        );
        FunctionActivatorResult errors = validate(pureModel, activator);
        this.extraValidators.select(v -> v.supports(activator)).forEach(v -> errors.getErrors().addAll(v.validate(identity, activator)));
        return errors;
    }

    public FunctionActivatorResult validate(PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator)
    {
        MutableList<FunctionActivatorError> errors = Lists.mutable.empty();

        if (activator._activationConfiguration() == null)
        {
            errors.add(new SnowflakeM2MUdfError("SnowflakeM2MUdf function activator must have an activation configuration"));
        }
        validateFormat(activator._udfName(), "Udf name", errors);
        validateFormat(activator._deploymentSchema(), "Deployment schema", errors);
        // Validate function parameters
        String functionParameterError = core_snowflake_core_snowflakem2mUdf_validation_validation.Root_meta_external_function_activator_snowflakeM2MUdf_generation_validateParameters_Function_1__String_$0_1$_(activator._function(),pureModel.getExecutionSupport());
        if (functionParameterError != null && !functionParameterError.isEmpty())
        {
            errors.add(new SnowflakeM2MUdfError(functionParameterError));
        }
        // Validate function return type
        String functionReturnTypeError = core_snowflake_core_snowflakem2mUdf_validation_validation.Root_meta_external_function_activator_snowflakeM2MUdf_generation_validateReturnType_Function_1__String_$0_1$_(activator._function(), pureModel.getExecutionSupport());
        if (functionReturnTypeError != null && !functionReturnTypeError.isEmpty())
        {
            errors.add(new SnowflakeM2MUdfError(functionReturnTypeError));
        }
        return new FunctionActivatorResult(errors);
    }

    public void validateFormat(String name, String entity, MutableList<FunctionActivatorError> errors)
    {
        if (name.trim().equals(""))
        {
            errors.add(new SnowflakeM2MUdfError(entity + " cannot be empty"));
        }
        if (name.trim().length() > 0 && !Character.toString(name.charAt(0)).matches("[a-zA-Z_]"))
        {
            errors.add(new SnowflakeM2MUdfError(entity + " first character can only be letter, digit or underscore"));
        }
        if (name.trim().length() > 1 && !name.substring(1).matches("[a-zA-Z0-9_$]+"))
        {
            errors.add(new SnowflakeM2MUdfError(entity + " can only contains letter, digit, underscore and dollar"));
        }
    }

    @Override
    public SnowflakeM2MUdfDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, PureModelContext inputModel, List<SnowflakeM2MUdfDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<FunctionActivatorError> errors = Lists.mutable.empty();
        errors.add(new SnowflakeM2MUdfError("Deploy to sandbox flow is not supported for Snowflake M2M Udf"));;
        return new SnowflakeM2MUdfDeploymentResult(errors.collect(v -> v.message));
    }

    @Override
    public SnowflakeM2MUdfArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return SnowflakeM2MUdfGenerator.generateArtifact(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return SnowflakeM2MUdfGenerator.generateFunctionLineage(pureModel, activator, routerExtensions);
    }

    @Override
    public List<SnowflakeM2MUdfDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof SnowflakeM2MUdfDeploymentConfiguration).collect(e -> (SnowflakeM2MUdfDeploymentConfiguration) e);
    }

}
