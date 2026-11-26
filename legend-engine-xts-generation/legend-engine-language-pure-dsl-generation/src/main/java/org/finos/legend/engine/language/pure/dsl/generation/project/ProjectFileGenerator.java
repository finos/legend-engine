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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Runs the file generations a generation specification lists, as the build does.
 * <p>
 * Ported from legend-sdlc {@code FileGenerationFactory} and {@code FileGenerator}, at legend-sdlc
 * 3e2897a8. Until the build calls this class, a change to those has to be made here too.
 */
final class ProjectFileGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFileGenerator.class);
    private static final String PACKAGE_SEPARATOR = "::";

    private final PureModel pureModel;
    private final MapIterable<String, FileGenerationSpecification> fileGenerationSpecifications;
    private final MutableListMultimap<String, GenerationExtension> extensions;

    ProjectFileGenerator(PureModelContextData pureModelContextData, PureModel pureModel)
    {
        this.pureModel = pureModel;
        this.fileGenerationSpecifications = LazyIterate.selectInstancesOf(pureModelContextData.getElements(), FileGenerationSpecification.class).groupByUniqueKey(PackageableElement::getPath, Maps.mutable.empty());
        this.extensions = Iterate.addAllTo(ServiceLoader.load(GenerationExtension.class), Lists.mutable.<GenerationExtension>empty()).groupBy(GenerationExtension::getKey);
    }

    /**
     * Generates every file generation the specification lists, in its order, adding each one's
     * result to the step as soon as it is done, so a failure leaves what was done before it.
     */
    void generate(GenerationSpecification generationSpecification, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.FileGenerationStepResult step)
    {
        if ((generationSpecification._package == null) || generationSpecification._package.isEmpty())
        {
            throw new RuntimeException("Invalid generation specifications, missing path '" + generationSpecification.name);
        }
        Set<String> generated = new HashSet<>();
        for (PackageableElementPointer fileGenerationPointer : generationSpecification.fileGenerations)
        {
            FileGenerationSpecification fileGenerationSpecification = this.fileGenerationSpecifications.get(fileGenerationPointer.path);
            if (fileGenerationSpecification == null)
            {
                throw new ProjectGenerationStepException(new RuntimeException("File generation '" + fileGenerationPointer.path + "' not found in model"), fileGenerationPointer.path);
            }
            // a file generation listed twice is written once
            if (generated.add(fileGenerationSpecification.getPath()))
            {
                try
                {
                    step.results.add(generate(fileGenerationSpecification, outputFolder, step));
                }
                catch (RuntimeException e)
                {
                    throw new ProjectGenerationStepException(e, fileGenerationSpecification.getPath());
                }
            }
        }
    }

    private ProjectGenerationResult.FileGenerationResult generate(FileGenerationSpecification fileGeneration, GeneratedFiles.OutputFolder outputFolder, ProjectGenerationResult.FileGenerationStepResult step)
    {
        LOGGER.info("Start generating file generation '{}' with type '{}'", fileGeneration.getPath(), fileGeneration.type);
        GenerationExtension extension = this.extensions.get(fileGeneration.type).getFirst();
        Assert.assertTrue(extension != null, () -> "Can't find a handler for the file generation type '" + fileGeneration.type.toLowerCase() + "'");
        List<Root_meta_pure_generation_metamodel_GenerationOutput> outputs = extension.generateFromElement(fileGeneration, this.pureModel.getContext());
        Assert.assertTrue(outputs != null, () -> "No generator found for file generation for file type '" + fileGeneration.type.toLowerCase() + "'");
        LOGGER.info("Done generating files, {} files generated", outputs.size());

        ProjectGenerationResult.FileGenerationResult result = new ProjectGenerationResult.FileGenerationResult();
        result.fileGeneration = fileGeneration.getPath();
        result.type = fileGeneration.type;
        String generationOutputPath = fileGeneration.generationOutputPath;
        result.rootFolder = ((generationOutputPath != null) && !generationOutputPath.isEmpty()) ? generationOutputPath : fileGeneration.getPath().replace(PACKAGE_SEPARATOR, "_");
        for (Root_meta_pure_generation_metamodel_GenerationOutput output : outputs)
        {
            ProjectGenerationResult.GeneratedFile file = GeneratedFiles.newFile(output._content(), output._fileName(), output._format());
            String path = result.rootFolder + "/" + file.fileName;
            if (outputFolder.add(path, file.content, "Duplicate file paths found when serializing file generations outputs : '" + path + "'"))
            {
                ProjectGenerationResult.Warning warning = new ProjectGenerationResult.Warning();
                warning.message = "Duplicate file paths found with the same content: " + path;
                warning.path = path;
                step.warnings.add(warning);
            }
            result.files.add(file);
        }
        return result;
    }
}
