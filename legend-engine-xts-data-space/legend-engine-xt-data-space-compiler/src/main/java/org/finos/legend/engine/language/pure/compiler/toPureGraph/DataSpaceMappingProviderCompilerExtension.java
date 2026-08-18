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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceMappingProvider;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface DataSpaceMappingProviderCompilerExtension
{
    Optional<Mapping> resolveMapping(PackageableElement element, DataSpaceMappingProvider provider, CompileContext context);

    default List<PackageableElement> extractMappingProviderElementsFromQuery(FunctionDefinition<?> query, CompileContext context)
    {
        return Collections.emptyList();
    }
}


