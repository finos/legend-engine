//  Copyright 2025 Goldman Sachs
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


package org.finos.legend.engine.language.functionJar.generation.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.generation.FunctionActivatorGenerator;
import org.finos.legend.engine.language.functionJar.generation.FunctionJarArtifactGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.functionJar.deployment.FunctionJarArtifact;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJar;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_FunctionJar;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

import java.util.List;

public class FunctionJarArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FunctionJarArtifactGenerationExtension.class);

    public final String ROOT_PATH = "Function-Jar-Artifact-Generation";

    private static final String FILE_NAME = "functionJarArtifact.json";
    private static final String LINEAGE_FILE_NAME = "functionJarLineageArtifact.json";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Function_Jar");
    }


    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
       return element instanceof Root_meta_external_function_activator_functionJar_FunctionJar;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> result = Lists.mutable.empty();
        try
        {
            LOGGER.info("Generating functionJar deploy artifact for " + element.getName());
            Root_meta_external_function_activator_functionJar_FunctionJar activator = (Root_meta_external_function_activator_functionJar_FunctionJar) element;
            Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
            PureModelContextData serviceData = FunctionJarArtifactGenerator.fetchFunctionJar(activator, data, pureModel);
            result.add(new Artifact(mapper.writeValueAsString(new FunctionJarArtifact(generatePointerForActivator(serviceData, data), ((Root_meta_external_function_activator_DeploymentOwnership)activator._ownership())._id(), FunctionActivatorGenerator.generateActions(activator, pureModel), (AlloySDLC) data.origin.sdlcInfo)), FILE_NAME, "json"));
            LOGGER.info("Generated artifacts for " + element.getName());

        }
        catch (Exception e)
        {
            LOGGER.error("Error generating functionJar artifacts ", e);
        }
        return result;
    }

    public PureModelContextPointer generatePointerForActivator(PureModelContextData activator, PureModelContextData originalModel)
    {
        PureModelContextPointer origin = originalModel.getOrigin();
        origin.sdlcInfo.packageableElementPointers = Lists.mutable.with(new PackageableElementPointer(PackageableElementType.SERVICE, activator.getElementsOfType(FunctionJar.class).get(0).getPath()),
                                                                        new PackageableElementPointer(PackageableElementType.FUNCTION, activator.getElementsOfType(org.finos.legend.engine.protocol.pure.m3.function.Function.class).get(0).getPath()));
        return origin;
    }

    public String elementName(FunctionJar service)
    {
        return service._package + "::" + service.name;
    }
}
