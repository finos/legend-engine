//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.


package org.finos.legend.engine.language.hostedService.generation.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.postDeployment.PostDeploymentLoader;
import org.finos.legend.engine.language.hostedService.generation.HostedServiceArtifactGenerator;
import org.finos.legend.engine.protocol.hostedService.deployment.HostedServiceArtifact;
import org.finos.legend.engine.protocol.hostedService.deployment.model.GenerationInfoData;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedService;
import org.finos.legend.engine.protocol.pure.v1.model.context.*;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

import java.util.List;

public class HostedServiceArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HostedServiceArtifactGenerationExtension.class);

    public final String ROOT_PATH = "Hosted-Service-Artifact-Generation";

    private static final String FILE_NAME = "hostedServiceArtifact.json";
    private static final String LINEAGE_FILE_NAME = "hostedServiceLineageArtifact.json";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Hosted_Service");
    }


    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
       return element instanceof Root_meta_external_function_activator_hostedService_HostedService;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> result = Lists.mutable.empty();
        try
        {
            LOGGER.info("Generating hostedService deploy artifact for " + element.getName());
            Root_meta_external_function_activator_hostedService_HostedService activator = (Root_meta_external_function_activator_hostedService_HostedService) element;
            Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
            GenerationInfoData info = HostedServiceArtifactGenerator.renderArtifact(pureModel, activator, data, clientVersion, routerExtensions);
            PureModelContextData serviceData = HostedServiceArtifactGenerator.fetchHostedService(activator, data, pureModel);
            result.add(new Artifact(mapper.writeValueAsString(new HostedServiceArtifact(activator._pattern(), info, generatePointerForActivator(serviceData, data), ((Root_meta_external_function_activator_DeploymentOwnership)activator._ownership())._id(), PostDeploymentLoader.generateActions(activator), (AlloySDLC) data.origin.sdlcInfo)), FILE_NAME, "json"));
            if (!(element._stereotypes().anySatisfy(stereotype ->
                    stereotype._profile()._name().equals("devStatus") && stereotype._profile()._p_stereotypes().anySatisfy(s -> s._value().equals("inProgress")))))
            {
                //lineage
                LOGGER.info("Generating hostedService lineage artifacts for " + element.getName());
                String lineage = HostedServiceArtifactGenerator.generateLineage(pureModel, activator, data, routerExtensions);
                result.add(new Artifact(lineage, LINEAGE_FILE_NAME, "json"));
            }
            LOGGER.info("Generated artifacts for " + element.getName());

        }
        catch (Exception e)
        {
            LOGGER.error("Error generating hostedService artifacts ", e);
        }
        return result;
    }

    public PureModelContextPointer generatePointerForActivator(PureModelContextData activator, PureModelContextData originalModel)
    {
        PureModelContextPointer origin = originalModel.getOrigin();
        origin.sdlcInfo.packageableElementPointers = Lists.mutable.with(new PackageableElementPointer(PackageableElementType.SERVICE, elementName(activator.getElementsOfType(HostedService.class).get(0))));
        return origin;
    }

    public String elementName(HostedService service)
    {
        return service._package + "::" + service.name;
    }
}
