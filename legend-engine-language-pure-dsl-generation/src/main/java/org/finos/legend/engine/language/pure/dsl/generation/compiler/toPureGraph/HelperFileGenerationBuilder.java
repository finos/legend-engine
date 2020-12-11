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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration;

import java.util.Objects;
import java.util.ServiceLoader;

public class HelperFileGenerationBuilder
{
    private static GenerationCompilerExtensionImpl getGenerationCompilerExtensionInstance(CompileContext context)
    {
        return Objects.requireNonNull(ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), GenerationCompilerExtensionImpl.class).getAny(), "Generation extension is not in scope");
    }

    public static Root_meta_pure_generation_metamodel_GenerationConfiguration getFileGeneration(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_pure_generation_metamodel_GenerationConfiguration fileGeneration = getGenerationCompilerExtensionInstance(context).fileConfigurationsIndex.get(fullPath);
        Assert.assertTrue(fileGeneration != null, () -> "Can't find file generation'" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return fileGeneration;
    }

    public static Root_meta_pure_generation_metamodel_GenerationConfiguration resolveFileGeneration(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, (String path) -> getFileGeneration(path, sourceInformation, context));
    }

    public static Root_meta_pure_generation_metamodel_GenerationConfiguration processFileGeneration(FileGenerationSpecification fileGeneration, CompileContext context)
    {
        MutableListMultimap<String, GenerationExtension> extensions = Iterate.addAllTo(ServiceLoader.load(GenerationExtension.class), Lists.mutable.empty()).groupBy(x -> x.getKey().toLowerCase());
        GenerationExtension extension = extensions.get(fileGeneration.type.toLowerCase()).getFirst();
        Assert.assertTrue(extension != null, ()->"Can't find a handler for the file type '"+fileGeneration.type.toLowerCase()+"'");
        return extension.defaultConfig(context);
    }
}
