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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingIncludeMapping;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingInclude_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_SubstituteStore_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

public class MappingIncludedMappingHandler implements IncludedMappingHandler
{
    @Override
    public Mapping resolveMapping(MappingInclude mappingInclude, CompileContext context)
    {
        return context.resolveMapping(mappingInclude.getFullName(), mappingInclude.sourceInformation);
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude processMappingInclude(MappingInclude mappingInclude, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parentMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping)
    {
        MappingIncludeMapping mappingIncludeMapping = (MappingIncludeMapping) mappingInclude;
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude include = new Root_meta_pure_mapping_MappingInclude_Impl("", SourceInformationHelper.toM3SourceInformation(mappingInclude.sourceInformation), context.pureModel.getClass("meta::pure::mapping::MappingInclude"));
        include._owner(parentMapping);
        if (mappingIncludeMapping.sourceDatabasePath != null && mappingIncludeMapping.targetDatabasePath != null)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore substituteStore = new Root_meta_pure_mapping_SubstituteStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::SubstituteStore"));
            substituteStore._owner(include);
            substituteStore._original(mappingIncludeMapping.sourceDatabasePath != null ? context.resolveStore(mappingIncludeMapping.sourceDatabasePath) : null);
            substituteStore._substitute(mappingIncludeMapping.targetDatabasePath != null ? context.resolveStore(mappingIncludeMapping.targetDatabasePath) : null);
            include._storeSubstitutionsAdd(substituteStore);
        }
        include._included(includedMapping);
        return include;
    }
}
