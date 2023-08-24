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
import org.finos.legend.engine.language.hostedService.deployment.HostedServiceArtifact;
import org.finos.legend.engine.language.hostedService.generation.model.GenerationInfoData;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentStage;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.hostedService.deployment.HostedServiceDeploymentManager;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedService;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentResult;
import org.finos.legend.engine.language.hostedService.generation.HostedServiceArtifactGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.*;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

import static org.finos.legend.pure.generated.platform_pure_basics_meta_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

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
    public MutableList<? extends FunctionActivatorError> validate(MutableList<CommonProfile> profiles, PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        MutableList<HostedServiceError> errors =  Lists.mutable.empty();
        try
        {
            this.hostedServiceArtifactgenerator.validateOwner(profiles, pureModel, activator, routerExtensions);
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
        return new HostedServiceArtifact(this.hostedServiceArtifactgenerator.renderArtifact(pureModel,activator,inputModel,clientVersion,routerExtensions));
    }

    @Override
    public List<HostedServiceDeploymentConfiguration> selectConfig(List<DeploymentConfiguration> configurations, DeploymentStage stage)
    {
        return Lists.mutable.withAll(configurations).select(e -> e instanceof HostedServiceDeploymentConfiguration && e.stage.equals(stage)).collect(e -> (HostedServiceDeploymentConfiguration)e);
    }


    @Override
    public HostedServiceDeploymentResult publishToSandbox(MutableList<CommonProfile> profiles, PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, List<HostedServiceDeploymentConfiguration> runtimeConfigs, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        GenerationInfoData generation = this.hostedServiceArtifactgenerator.renderArtifact(pureModel, activator, inputModel, "vX_X_X",routerExtensions);
        HostedServiceArtifact artifact = new HostedServiceArtifact(generation, fetchHostedService(activator, (PureModelContextData)inputModel, pureModel));
        return this.hostedServiceDeploymentManager.deploy(profiles, artifact, activator, runtimeConfigs);
//        return new HostedServiceDeploymentResult();
    }

    public static PureModelContextData fetchHostedService(Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContextData data, PureModel pureModel)
    {
        return PureModelContextData.newBuilder()
                .withElements(org.eclipse.collections.api.factory.Lists.mutable.withAll(data.getElements()).select(e -> e instanceof HostedService && elementToPath(activator, pureModel).equals(fullName(e))))
                .withOrigin(data.origin).build();
    }

    private static String elementToPath(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element, PureModel pureModel)
    {
        return Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(element, pureModel.getExecutionSupport());
    }

    private static String fullName(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement e)
    {
        return e._package + "::" + e.name;
    }


}
