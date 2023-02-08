// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.AbstractGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_PackageableElement_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;

public class GenerationCompilerExtensionImpl implements GenerationCompilerExtension
{
    final MutableMap<String, Root_meta_pure_generation_metamodel_GenerationSpecification> generationSpecificationsIndex = Maps.mutable.empty();
    final MutableMap<String, Root_meta_pure_generation_metamodel_GenerationConfiguration> fileConfigurationsIndex = Maps.mutable.empty();

    @Override
    public CompilerExtension build()
    {
        return new GenerationCompilerExtensionImpl();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(
                Processor.newProcessor(
                        FileGenerationSpecification.class,
                        (fileGeneration, context) ->
                        {
                            // NOTE: we stub out since this element doesn't have an equivalent packageable element form in PURE metamodel
                            PackageableElement stub = new Root_meta_pure_metamodel_PackageableElement_Impl(fileGeneration.name, null, context.pureModel.getClass("meta::pure::metamodel::PackageableElement"))._name(fileGeneration.name);
                            Root_meta_pure_generation_metamodel_GenerationConfiguration configuration = HelperFileGenerationBuilder.processFileGeneration(fileGeneration, context);
                            this.fileConfigurationsIndex.put(context.pureModel.buildPackageString(fileGeneration._package, fileGeneration.name), configuration);
                            return stub;
                        }),
                Processor.newProcessor(
                        GenerationSpecification.class,
                        Collections.singletonList(AbstractGenerationSpecification.class),
                        (generationSpecification, context) ->
                        {
                            Root_meta_pure_generation_metamodel_GenerationSpecification genTree = new Root_meta_pure_generation_metamodel_GenerationSpecification_Impl(generationSpecification.name, null, context.pureModel.getClass("meta::pure::generation::metamodel::GenerationSpecification"))._name(generationSpecification.name);
                            HelperGenerationSpecificationBuilder.processGenerationSpecification(generationSpecification, context);
                            this.generationSpecificationsIndex.put(context.pureModel.buildPackageString(generationSpecification._package, generationSpecification.name), genTree);
                            return genTree;
                        })
        );
    }
}
