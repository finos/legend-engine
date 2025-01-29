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

package org.finos.legend.engine.generation.artifact;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactGenerationFactory
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactGenerationFactory.class);
    private final PureModel pureModel;
    private final List<ArtifactGenerationExtension> extensions;
    private final List<PackageableElement> elements;
    private final PureModelContextData data;

    ArtifactGenerationFactory(PureModel pureModel, PureModelContextData data, List<PackageableElement> elements, Set<String> excludedExtensionKeys)
    {
        if (pureModel == null || elements == null)
        {
            throw new RuntimeException("Pure model and elements required for artifact generation factory");
        }
        this.data = data;
        this.pureModel = pureModel;
        this.elements = elements;
        List<ArtifactGenerationExtension> extensions = ArtifactGenerationExtensionLoader.extensions();
        if (excludedExtensionKeys != null && !excludedExtensionKeys.isEmpty())
        {
            extensions = extensions.stream().filter(extension ->  !excludedExtensionKeys.contains(extension.getKey())).collect(Collectors.toList());
        }
        this.extensions = extensions;
    }

    public List<ArtifactGenerationExtension> getExtensions()
    {
        return Collections.unmodifiableList(this.extensions);
    }

    public static ArtifactGenerationFactory newFactory(PureModel pureModel, PureModelContextData data, List<PackageableElement> elements)
    {
        return newFactory(pureModel, data, elements, null);
    }

    public static ArtifactGenerationFactory newFactory(PureModel pureModel, PureModelContextData data, List<PackageableElement> elements,  Set<String> excludedExtensions)
    {
        return new ArtifactGenerationFactory(pureModel, data, elements, excludedExtensions);
    }

    public MutableMap<ArtifactGenerationExtension, List<ArtifactGenerationResult>> generate()
    {
        if (this.extensions.isEmpty() || this.elements.isEmpty())
        {
            return Maps.mutable.empty();
        }
        MutableMap<ArtifactGenerationExtension, List<ArtifactGenerationResult>> results = Maps.mutable.empty();
        for (PackageableElement element : this.elements)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement(element);
            if (packageableElement != null)
            {
                for (ArtifactGenerationExtension extension : this.extensions)
                {
                    if (extension.canGenerate(packageableElement))
                    {
                        List<Artifact> artifacts = this.generateArtifacts(packageableElement, element, extension);
                        List<GenerationOutput> outputs = ListIterate.collect(artifacts, artifact -> new GenerationOutput(artifact.content, artifact.path, artifact.format));
                        ArtifactGenerationResult result = new ArtifactGenerationResult(packageableElement, outputs, extension);
                        results.getIfAbsentPut(result.getGenerator(), Lists.mutable::empty).add(result);
                    }
                }
            }
        }
        return results;
    }

    private List<Artifact> generateArtifacts(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement, PackageableElement element, ArtifactGenerationExtension extension)
    {
        try
        {
            LOGGER.info("Start generating artifact extension '{}' for element '{}'", extension.getClass(), element.getPath());
            List<Artifact> outputs = extension.generate(packageableElement, this.pureModel, this.data, PureClientVersions.production);
            LOGGER.info("Done generating artifacts, {} artifacts generated", outputs.size());
            return outputs;
        }
        catch (Exception exception)
        {
            LOGGER.error("Error generating artifacts for extension '{}':", extension.getClass(), exception);
            throw exception;
        }
    }

}

