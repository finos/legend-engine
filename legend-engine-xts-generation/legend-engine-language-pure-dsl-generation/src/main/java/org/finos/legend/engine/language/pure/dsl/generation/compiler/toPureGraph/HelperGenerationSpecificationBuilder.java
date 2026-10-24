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

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNodeHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HelperGenerationSpecificationBuilder
{
    public static List<GenerationCompilerExtension> getGenerationCompilerExtensions(CompileContext context)
    {
        return ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), GenerationCompilerExtension.class);
    }

    public static void processGenerationSpecification(GenerationSpecification generationSpecification, CompileContext context)
    {
        // TODO? maybe we should think of a way to inform the users when the element is found but not supported by any of the plugins
        List<Function3<String, SourceInformation, CompileContext, PackageableElement>> extraModelGenerationSpecificationResolvers = ListIterate.flatCollect(getGenerationCompilerExtensions(context), GenerationCompilerExtension::getExtraModelGenerationSpecificationResolvers);
        Set<String> ids = Sets.mutable.empty();
        generationSpecification.generationNodes.forEach(node ->
        {
            if (!ids.add(node.id))
            {
                throw new EngineException("Duplicate generation node id '" + node.id + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            if ((node.generationElement != null) && !node.generationElements.isEmpty())
            {
                throw new EngineException("Generation node must specify either 'generationElement' or 'generationElements', not both", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            List<String> generationElements = GenerationTreeNodeHelper.generationElements(node);
            if (generationElements.isEmpty())
            {
                throw new EngineException("Generation node must specify at least one generation element", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            Set<String> nodeGenerationElements = Sets.mutable.empty();
            generationElements.forEach(generationElement ->
            {
                if (!nodeGenerationElements.add(generationElement))
                {
                    throw new EngineException("Duplicate generation element '" + generationElement + "' in generation node '" + node.id + "'", node.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
            generationElements.forEach(generationElement ->
            {
                if (extraModelGenerationSpecificationResolvers.stream().map(resolver -> resolver.value(generationElement, node.sourceInformation, context)).noneMatch(Objects::nonNull))
                {
                    throw new EngineException("Can't find generation element '" + generationElement + "'", node.sourceInformation, EngineErrorType.COMPILATION);
                }
            });
        });
        Set<String> fileGenerations = Sets.mutable.empty();
        generationSpecification.fileGenerations.forEach(fileGeneration ->
        {
            if (!fileGenerations.add(fileGeneration.path))
            {
                throw new EngineException("Duplicate file generation '" + fileGeneration.path + "'", fileGeneration.sourceInformation, EngineErrorType.COMPILATION);
            }
            HelperFileGenerationBuilder.resolveFileGeneration(fileGeneration.path, fileGeneration.sourceInformation, context);
        });
    }
}
