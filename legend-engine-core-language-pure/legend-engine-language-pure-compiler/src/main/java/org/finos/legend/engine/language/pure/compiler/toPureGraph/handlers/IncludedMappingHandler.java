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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

public interface IncludedMappingHandler
{
    Mapping resolveMapping(MappingInclude mappingInclude, CompileContext context);

    static String parseIncludedMappingNameRecursively(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude includedMapping)
    {
        Mapping mapping = includedMapping._included();
        StringBuilder name = new StringBuilder(mapping._name());
        Package mappingPackage = mapping._package();
        while (mappingPackage != null && (!mappingPackage._name().equals("Root") || mappingPackage._package() != null))
        {
            name.insert(0, "::");
            name.insert(0, mappingPackage._name());
            mappingPackage = mappingPackage._package();
        }
        return name.toString();
    }

    org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude processMappingInclude(MappingInclude mappingInclude, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parentMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping);
}
