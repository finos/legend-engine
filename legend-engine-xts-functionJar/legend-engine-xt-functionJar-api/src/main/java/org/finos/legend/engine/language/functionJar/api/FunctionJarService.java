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

package org.finos.legend.engine.language.functionJar.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorResult;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorValidator;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionJar.deployment.FunctionJarArtifact;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.functionJar.generation.FunctionJarArtifactGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJarProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.functionJar.deployment.FunctionJarDeploymentResult;
import org.finos.legend.engine.protocol.functionJar.deployment.FunctionJarDeploymentConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.*;

import java.util.List;

public class FunctionJarService implements FunctionActivatorService<Root_meta_external_function_activator_functionJar_FunctionJar, FunctionJarDeploymentConfiguration, FunctionJarDeploymentResult>
{
    private final FunctionJarArtifactGenerator functionJarArtifactgenerator;
    private MutableList<FunctionActivatorValidator> extraValidators = Lists.mutable.empty();

    public FunctionJarService()
    {
        this.functionJarArtifactgenerator = new FunctionJarArtifactGenerator();
    }

    public FunctionJarService(List<FunctionActivatorValidator> extraValidators)
    {
        this();
        this.extraValidators = Lists.mutable.withAll(extraValidators);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Function_Jar");
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Function Jar",
                "Create a Executable Jar",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::functionJar::FunctionJar",
                FunctionJarProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_functionJar_FunctionJar;
    }

    @Override
    public FunctionActivatorResult validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_functionJar_FunctionJar activator, PureModelContext inputModel, List<FunctionJarDeploymentConfiguration> runtimeConfigurations, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<FunctionJarError> errors =  Lists.mutable.empty();
        FunctionActivatorResult result = new FunctionActivatorResult();
        try
        {
            core_functionjar_generation_generation.Root_meta_external_function_activator_functionJar_validator_validateFunctionJar_FunctionJar_1__Boolean_1_(activator, pureModel.getExecutionSupport()); //returns true or errors out

        }
        catch (Exception e)
        {
            errors.add(new FunctionJarError("FunctionJar can't be registered.", e));
        }
        this.extraValidators.select(v -> v.supports(activator)).forEach(v ->
        {
            errors.addAll(v.validate(identity, activator));
        });
//        result.addAll(validateArtifactActions(identity, pureModel, activator, inputModel, runtimeConfigurations, "vX_X_X", routerExtensions));
        result.getErrors().addAll(errors);
        return result;
    }

    @Override
    public FunctionJarArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_functionJar_FunctionJar activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return this.functionJarArtifactgenerator.renderArtifact(pureModel, activator, inputModel, clientVersion, routerExtensions);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_functionJar_FunctionJar activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return "";
    }

    @Override
    public List<FunctionJarDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        List<FunctionJarDeploymentConfiguration> result = Lists.mutable.empty();
        return result;
    }

    @Override
    public FunctionJarDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_functionJar_FunctionJar activator, PureModelContext inputModel, List<FunctionJarDeploymentConfiguration> runtimeConfigs, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return new FunctionJarDeploymentResult();
    }

}
