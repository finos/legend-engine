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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.HashSet;
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
        Set<String> ids = new HashSet<>();
        generationSpecification.generationNodes.forEach(node ->
        {
            if (!ids.add(node.id))
            {
                throw new EngineException("Duplicate generation node id '" + node.id + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }
            // TODO? maybe we should think of a way to inform the users when the element is found but not supported by any of the plugins
            List<Function3<String, SourceInformation, CompileContext, PackageableElement>> extraModelGenerationSpecificationResolvers = ListIterate.flatCollect(getGenerationCompilerExtensions(context), GenerationCompilerExtension::getExtraModelGenerationSpecificationResolvers);
            if (extraModelGenerationSpecificationResolvers.stream().map(resolver -> resolver.value(node.generationElement, node.sourceInformation, context)).noneMatch(Objects::nonNull))
            {
                throw new EngineException("Can't find generation element '" + node.generationElement + "'", node.sourceInformation, EngineErrorType.COMPILATION);
            }
        });
        Set<String> fileGenerations = new HashSet<>();
        generationSpecification.fileGenerations.forEach(fileGeneration ->
        {
            if (!fileGenerations.add(fileGeneration.path))
            {
                throw new EngineException("Duplicate file generation'" + fileGeneration.path + "'", fileGeneration.sourceInformation, EngineErrorType.COMPILATION);
            }
            HelperFileGenerationBuilder.resolveFileGeneration(fileGeneration.path, fileGeneration.sourceInformation, context);
        });
    }
}
