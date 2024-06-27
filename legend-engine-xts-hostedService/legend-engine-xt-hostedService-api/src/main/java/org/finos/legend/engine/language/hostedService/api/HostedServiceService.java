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

package org.finos.legend.engine.language.hostedService.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.deployment.HostedServiceArtifact;
import org.finos.legend.engine.protocol.hostedService.deployment.HostedServiceDeploymentConfiguration;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.hostedService.generation.deployment.HostedServiceDeploymentManager;
import org.finos.legend.engine.protocol.hostedService.deployment.model.GenerationInfoData;
import org.finos.legend.engine.protocol.hostedService.deployment.HostedServiceDeploymentResult;
import org.finos.legend.engine.language.hostedService.generation.HostedServiceArtifactGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.*;

import java.util.List;

public class HostedServiceService implements FunctionActivatorService<Root_meta_external_function_activator_hostedService_HostedService, HostedServiceDeploymentConfiguration, HostedServiceDeploymentResult>
{
    private final HostedServiceArtifactGenerator hostedServiceArtifactgenerator;
    private final HostedServiceDeploymentManager hostedServiceDeploymentManager;

    public HostedServiceService()
    {

        this.hostedServiceArtifactgenerator = new HostedServiceArtifactGenerator();
        this.hostedServiceDeploymentManager = new HostedServiceDeploymentManager();
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Hosted_Service");
    }

    @Override
    public FunctionActivatorInfo info(PureModel pureModel, String version)
    {
        return new FunctionActivatorInfo(
                "Hosted Service",
                "Create a HostedService that will be deployed to a server environment and executed with a pattern",
                "meta::protocols::pure::" + version + "::metamodel::function::activator::hostedService::HostedService",
                HostedServiceProtocolExtension.packageJSONType,
                pureModel);
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_FunctionActivator functionActivator)
    {
        return functionActivator instanceof Root_meta_external_function_activator_hostedService_HostedService;
    }

    @Override
    public MutableList<? extends FunctionActivatorError> validate(Identity identity, PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<HostedServiceError> errors =  Lists.mutable.empty();
        try
        {
            this.hostedServiceArtifactgenerator.validateOwner(identity, pureModel, activator, routerExtensions);
            core_hostedservice_generation_generation.Root_meta_external_function_activator_hostedService_validator_validateService_HostedService_1__Boolean_1_(activator, pureModel.getExecutionSupport()); //returns true or errors out

        }
        catch (Exception e)
        {
           errors.add(new HostedServiceError("HostedService can't be registered.", e));
        }
        return errors;

    }

    @Override
    public HostedServiceArtifact renderArtifact(PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return new HostedServiceArtifact(activator._pattern(), this.hostedServiceArtifactgenerator.renderArtifact(pureModel,activator,inputModel,clientVersion,routerExtensions), null);
    }

    @Override
    public String generateLineage(PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return this.hostedServiceArtifactgenerator.generateLineage(pureModel, activator, inputModel, routerExtensions);
    }

    @Override
    public List<HostedServiceDeploymentConfiguration> selectConfig(List<FunctionActivatorDeploymentConfiguration> configurations)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof HostedServiceDeploymentConfiguration).collect(e -> (HostedServiceDeploymentConfiguration)e);
    }

    @Override
    public HostedServiceDeploymentResult publishToSandbox(Identity identity, PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, List<HostedServiceDeploymentConfiguration> runtimeConfigs, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        GenerationInfoData generation = this.hostedServiceArtifactgenerator.renderArtifact(pureModel, activator, inputModel, "vX_X_X",routerExtensions);
        HostedServiceArtifact artifact = new HostedServiceArtifact(activator._pattern(), generation, HostedServiceArtifactGenerator.fetchHostedService(activator, (PureModelContextData)inputModel, pureModel), ((PureModelContextData)inputModel).origin != null ? (AlloySDLC) ((PureModelContextData)inputModel).origin.sdlcInfo : null);
        return this.hostedServiceDeploymentManager.deploy(identity, artifact, runtimeConfigs);
    }


}
