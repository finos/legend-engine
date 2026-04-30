// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.deephaven.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_deephavenApp_DeephavenApp;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_deephavenApp_generation_DeephavenAppArtifact;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_deephaven_java_platform_binding_deephavenAppGeneration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DeephavenAppArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeephavenAppArtifactGenerationExtension.class);
    private static final String ROOT_PATH = "deephavenApp";
    private static final String FILE_NAME = "deephavenAppArtifact.json";

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Function_Activator", "Deephaven");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_external_function_activator_deephavenApp_DeephavenApp;
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> result = Lists.mutable.empty();
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions =
                (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));

        Root_meta_external_function_activator_deephavenApp_DeephavenApp deephavenApp =
                (Root_meta_external_function_activator_deephavenApp_DeephavenApp) element;

        try
        {
            LOGGER.info("Generating DeephavenApp artifact for " + deephavenApp._applicationName());

            Root_meta_external_function_activator_deephavenApp_generation_DeephavenAppArtifact deephavenAppArtifact =
                    core_deephaven_java_platform_binding_deephavenAppGeneration
                            .Root_meta_external_store_deephaven_executionPlan_platformBinding_legendJava_appGeneration_generateArtifact_DeephavenApp_1__Extension_MANY__DeephavenAppArtifact_1_(
                                    deephavenApp,
                                    routerExtensions.apply(pureModel),
                                    pureModel.getExecutionSupport()
                            );

            DeephavenAppArtifactContent content = new DeephavenAppArtifactContent(
                    deephavenApp._applicationName(),
                    deephavenAppArtifact._appConfigContent(),
                    deephavenAppArtifact._javaSourceContent()
            );
            String json = mapper.writeValueAsString(content);
            result.add(new Artifact(json, FILE_NAME, "json"));

            LOGGER.info("Generated artifact for " + deephavenApp._applicationName());
        }
        catch (Exception e)
        {
            LOGGER.error("Error generating artifact for " + deephavenApp._applicationName() + " reason: " + e.getMessage(), e);
        }
        return result;
    }
}

