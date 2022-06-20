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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HelperDataSpaceBuilder
{
    private static DataSpaceCompilerExtension getDataSpaceCompilerExtensionInstance(CompileContext context)
    {
        return Objects.requireNonNull(ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), DataSpaceCompilerExtension.class).getAny(), "Data space extension is not in scope");
    }

    public static Root_meta_pure_metamodel_dataSpace_DataSpace getDataSpace(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_pure_metamodel_dataSpace_DataSpace diagram = getDataSpaceCompilerExtensionInstance(context).dataSpacesIndex.get(fullPath);
        Assert.assertTrue(diagram != null, () -> "Can't find data space '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return diagram;
    }

    public static Root_meta_pure_metamodel_dataSpace_DataSpace resolveDataSpace(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, path -> getDataSpace(path, sourceInformation, context));
    }


    public static List<Root_meta_pure_runtime_PackageableRuntime> getExecutionContextCompatibleRuntimes(
            Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext executionContext, List<PackageableRuntime> runtimes, PureModel pureModel)
    {
        Set<Mapping> mappings = new HashSet<>();
        mappings.add(executionContext._mapping());
        mappings.addAll(HelperMappingBuilder.getAllIncludedMappings(executionContext._mapping()).toSet());
        return ListIterate
                .select(runtimes, runtime -> ListIterate.collect(runtime.runtimeValue.mappings, mappingPtr -> pureModel.getMapping(mappingPtr.path, mappingPtr.sourceInformation)).anySatisfy(mappings::contains))
                .collect(runtime -> pureModel.getPackageableRuntime(runtime.getPath(), null)).distinct();
    }
}

