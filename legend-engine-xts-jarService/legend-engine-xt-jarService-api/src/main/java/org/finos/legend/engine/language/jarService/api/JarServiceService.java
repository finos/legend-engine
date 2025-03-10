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

package org.finos.legend.engine.language.jarService.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorValidator;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceArtifact;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceDeploymentConfiguration;
import org.finos.legend.engine.functionActivator.validation.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.jarService.generation.deployment.JarServiceDeploymentManager;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceDeploymentResult;
import org.finos.legend.engine.language.jarService.generation.JarServiceArtifactGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceDestination;
import org.finos.legend.engine.protocol.jarService.metamodel.JarServiceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.*;

import java.util.List;

public class JarServiceService implements FunctionActivatorService<Root_meta_external_function_activator_jarService_JarService, JarServiceDeploymentConfiguration, JarServiceDeploymentResult>
{
    private final JarServiceArtifactGenerator jarServiceArtifactgenerator;
//    private final JarServiceDeploymentManager jarServiceDeploymentManager;
    private MutableList<FunctionActivatorValidator> extraValidators = Lists.mutable.empty();


    public JarServiceService()
    {

        this.jarServiceArtifactgenerator = new JarServiceArtifactGenerator();
//        this.jarServiceDeploymentManager = new JarServiceDeploymentManager();
    }

    public JarServiceService(List<FunctionActivatorValidator> extraValidators)
    {
        this();
        this.extraValidators = Lists.mutable.withAll(extraValidators);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Jar_Service");
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Jar Service",
                "Create a Executable Jar",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::jarService::JarService",
                JarServiceProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_jarService_JarService;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<JarServiceError> errors =  Lists.mutable.empty();
        try
        {
            core_jarservice_generation_generation.Root_meta_external_function_activator_jarService_validator_validateService_JarService_1__Boolean_1_(activator, pureModel.getExecutionSupport()); //returns true or errors out

        }
        catch (Exception e)
        {
            errors.add(new JarServiceError("JarService can't be registered.", e));
        }
        this.extraValidators.select(v -> v.supports(activator)).forEach(v ->
        {
            errors.addAll(v.validate(identity, activator));
            errors.addAll(v.validate(activator, pureModel));
        });
        return errors;

    }

    @Override
    public JarServiceArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return this.jarServiceArtifactgenerator.renderServiceArtifact(pureModel, activator, inputModel, clientVersion, routerExtensions);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return this.jarServiceArtifactgenerator.generateLineage(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public List<JarServiceDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof JarServiceDeploymentConfiguration).collect(e -> (JarServiceDeploymentConfiguration)e);
    }

    @Override
    public JarServiceDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, List<JarServiceDeploymentConfiguration> runtimeConfigs, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return new JarServiceDeploymentResult();
    }

}
