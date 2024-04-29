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

package org.finos.legend.engine.functionActivator.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.functionActivator.metamodel.FunctionActivator;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

import java.util.List;

import static org.finos.legend.pure.generated.platform_pure_basics_meta_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

public class FunctionActivatorArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FunctionActivatorArtifactGenerationExtension.class);

    public final String ROOT_PATH = "Function-Activator-Artifact-Generation";

    private static final String FILE_NAME = "functionActivatorMetadata.json";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_external_function_activator_FunctionActivator;
    }


    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> result = Lists.mutable.empty();
        Root_meta_external_function_activator_FunctionActivator activator  = (Root_meta_external_function_activator_FunctionActivator) element;
        try
        {
            PureModelContextData activatorProtocol = fetchActivator(activator, data, pureModel);
            AlloySDLC activatorCoordinates  = (AlloySDLC) activatorProtocol.origin.sdlcInfo;
            FunctionActivator activatorElement = (FunctionActivator) activatorProtocol.getElements().get(0);
            FunctionActivatorMetadata metadata = new FunctionActivatorMetadata(activatorElement.name, activatorElement._package,
                   ((DeploymentOwner)activatorElement.ownership).id, activatorCoordinates.version, activatorCoordinates.groupId, activatorCoordinates.artifactId);
            String content = mapper.writeValueAsString(metadata);
            result.add(new Artifact(content, FILE_NAME, "json"));
        }
        catch (Exception e)
        {
            LOGGER.error("Error generating Activator Metadata ", e);
        }
        return result;
    }

    public static PureModelContextData fetchActivator(Root_meta_external_function_activator_FunctionActivator activator, PureModelContextData data, PureModel pureModel)
    {
        return PureModelContextData.newBuilder()
                .withElements(org.eclipse.collections.api.factory.Lists.mutable.withAll(data.getElements()).select(e -> e instanceof FunctionActivator && elementToPath(activator, pureModel).equals(elementName((FunctionActivator)e))))
                .withOrigin(data.origin).build();
    }

    public static String elementToPath(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element, PureModel pureModel)
    {
        return Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(element, pureModel.getExecutionSupport());
    }

    public static String elementName(FunctionActivator activator)
    {
        return activator._package + "::" + activator.name;
    }


}
