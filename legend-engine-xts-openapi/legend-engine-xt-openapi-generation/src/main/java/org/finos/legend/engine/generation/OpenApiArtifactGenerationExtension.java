//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.pure.generated.Root_meta_external_function_description_openapi_metamodel_Server_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_openapi_transformation_fromPure_pureToOpenApi;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OpenApiArtifactGenerationExtension implements ArtifactGenerationExtension
{

    private static final String ROOT_PATH = "openapi_spec";
    private static final String HOST = "${HOST}";
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OpenApiArtifactGenerationExtension.class);


    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Generation", "Artifact", "OpenAPI");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_legend_service_metamodel_Service && checkIfOpenApiProfile(element);
    }

    private boolean checkIfOpenApiProfile(PackageableElement element)
    {
        return element._stereotypes() != null && element._stereotypes().anySatisfy(stereotype ->
                        stereotype._profile()._name().equals("ServiceSpecGeneration") &&
                                stereotype._profile()._p_stereotypes().anySatisfy(s -> s.getName().equals("OpenAPI"))
                );
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        try
        {
            RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> packageableElements = Lists.immutable.withAll(data.getElements().stream().filter(e -> e instanceof Class).map(e -> pureModel.getPackageableElement(e._package + "::" +  e.name)).collect(Collectors.toList()));
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            String result = core_external_format_openapi_transformation_fromPure_pureToOpenApi.Root_meta_external_function_description_openapi_transformation_fromPure_serviceToOpenApi_Service_1__PackageableElement_MANY__Server_1__Extension_MANY__String_1_((Root_meta_legend_service_metamodel_Service) element, packageableElements, new Root_meta_external_function_description_openapi_metamodel_Server_Impl("")._url(HOST), routerExtensions, pureModel.getExecutionSupport());
            Artifact output = new Artifact(result, element.getName() + "_spec.json", "json");
            return Collections.singletonList(output);
        }
        catch (Exception ex)
        {
            LOGGER.warn("Error generating openapi specification", ex);
        }
        return Collections.emptyList();
    }
}
