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

package org.finos.legend.engine.language.pure.dsl.generation.project;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtensionLoader;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Runs every artifact extension over every element it can generate for, as the build does.
 * <p>
 * Ported from legend-sdlc {@code ArtifactGenerationFactory}, at legend-sdlc 3e2897a8. Until the
 * build calls this class, a change to it has to be made here too. On top of it, the extensions
 * that run can be narrowed with {@link ArtifactGenerationConfig}.
 */
final class ProjectArtifactGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectArtifactGenerator.class);

    private final PureModel pureModel;
    private final PureModelContextData data;
    private final List<ArtifactGenerationExtension> extensions;
    private final String clientVersion;

    ProjectArtifactGenerator(PureModel pureModel, PureModelContextData data, ArtifactGenerationConfig config, String clientVersion)
    {
        this.pureModel = pureModel;
        this.data = data;
        this.extensions = selectExtensions(ArtifactGenerationExtensionLoader.extensions(), config);
        this.clientVersion = clientVersion;
    }

    /**
     * The keys in {@link ArtifactGenerationConfig#includedExtensionKeys} that no extension on this
     * server has, sorted.
     */
    static Set<String> unknownIncludedExtensionKeys(ArtifactGenerationConfig config)
    {
        Set<String> unknown = new TreeSet<>();
        if ((config != null) && !isEmpty(config.includedExtensionKeys))
        {
            unknown.addAll(config.includedExtensionKeys);
            ArtifactGenerationExtensionLoader.extensions().forEach(extension -> unknown.remove(extension.getKey()));
        }
        return unknown;
    }

    static Set<String> availableExtensionKeys()
    {
        Set<String> keys = new TreeSet<>();
        ArtifactGenerationExtensionLoader.extensions().forEach(extension -> keys.add(extension.getKey()));
        return keys;
    }

    private static List<ArtifactGenerationExtension> selectExtensions(List<ArtifactGenerationExtension> extensions, ArtifactGenerationConfig config)
    {
        if (config == null)
        {
            return extensions;
        }
        Set<String> included = config.includedExtensionKeys;
        Set<String> excluded = config.excludedExtensionKeys;
        return ListIterate.select(extensions, extension ->
                (isEmpty(included) || included.contains(extension.getKey())) &&
                (isEmpty(excluded) || !excluded.contains(extension.getKey())));
    }

    private static boolean isEmpty(Collection<?> collection)
    {
        return (collection == null) || collection.isEmpty();
    }

    /**
     * Generates the artifacts of the given elements, adding each result to the step as soon as it
     * is done, so a failure leaves what was done before it.
     */
    void generate(List<PackageableElement> elements, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.ArtifactGenerationStepResult step)
    {
        if (this.extensions.isEmpty() || elements.isEmpty())
        {
            return;
        }
        for (PackageableElement element : elements)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = findPackageableElement(element);
            if (packageableElement != null)
            {
                for (ArtifactGenerationExtension extension : this.extensions)
                {
                    if (extension.canGenerate(packageableElement))
                    {
                        try
                        {
                            step.results.add(generate(packageableElement, element, extension, outputFolder, step));
                        }
                        catch (RuntimeException e)
                        {
                            throw new ProjectGenerationStepException(e, element.getPath(), extension.getKey());
                        }
                    }
                }
            }
        }
    }

    private org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement findPackageableElement(PackageableElement packageableElement)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element = this.pureModel.getPackageableElement_safe(packageableElement.getPath());
        if (element != null)
        {
            return element;
        }
        if (packageableElement instanceof Function)
        {
            Function elementFunc = (Function) packageableElement;
            String fullPath = this.pureModel.buildPackageString(packageableElement._package, HelperModelBuilder.getSignature(elementFunc));
            element = this.pureModel.getPackageableElement_safe(fullPath);
        }
        if (element == null)
        {
            LOGGER.debug("Unable to find element '{}' in Pure Model", packageableElement.getPath());
        }
        return element;
    }

    private ProjectGenerationResult.ArtifactResult generate(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement, PackageableElement element, ArtifactGenerationExtension extension, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.ArtifactGenerationStepResult step)
    {
        LOGGER.info("Start generating artifact extension '{}' for element '{}'", extension.getClass(), element.getPath());
        List<Artifact> artifacts = extension.generate(packageableElement, this.pureModel, this.data, this.clientVersion);
        LOGGER.info("Done generating artifacts, {} artifacts generated", artifacts.size());

        ProjectGenerationResult.ArtifactResult result = new ProjectGenerationResult.ArtifactResult();
        result.element = element.getPath();
        result.extension = extension.getKey();
        result.folder = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(packageableElement, "/") + "/" + extension.getKey();
        for (Artifact artifact : artifacts)
        {
            ProjectGenerationResult.GeneratedFile file = GeneratedFiles.newFile(artifact.content, artifact.path, artifact.format);
            String path = result.folder + "/" + file.fileName;
            if (outputFolder.add(path, file.content, "Duplicate file path found when serializing artifact generation extension  '" + extension.getClass() + "' output: '" + path + "'"))
            {
                ProjectGenerationResult.Warning warning = new ProjectGenerationResult.Warning();
                warning.message = "Duplicate file paths found with the same content: " + path;
                warning.element = element.getPath();
                warning.path = path;
                step.warnings.add(warning);
            }
            result.files.add(file);
        }
        return result;
    }
}
