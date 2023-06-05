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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.MappingIncludeDataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingInclude_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_SubstituteStore_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

public class DataSpaceIncludedMappingHandler implements IncludedMappingHandler
{
    @Override
    public Mapping resolveMapping(MappingInclude mappingInclude, CompileContext context)
    {
        Root_meta_pure_metamodel_dataSpace_DataSpace dataspace =
                DataSpaceCompilerExtension.dataSpacesIndex.get(mappingInclude.getFullName());
        if (dataspace == null)
        {
            throw new EngineException("Included dataspace " + mappingInclude.getFullName() + " does not exist.", mappingInclude.sourceInformation, EngineErrorType.COMPILATION);
        }
        return dataspace._defaultExecutionContext()._mapping();
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude processMappingInclude(MappingInclude mappingInclude, CompileContext context, Mapping parentMapping, Mapping includedMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude include = new Root_meta_pure_mapping_MappingInclude_Impl("", null, context.pureModel.getClass("meta::pure::mapping::MappingInclude"));
        include._owner(parentMapping);
        include._included(includedMapping);
        return include;
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude processMappingIncludeSixthPass(MappingInclude mappingInclude, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping parentMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude include = parentMapping._includes().select(i -> i._included().equals(includedMapping)).getOnly();
        MappingIncludeDataSpace mappingIncludeDataSpace = (MappingIncludeDataSpace) mappingInclude;
        if (mappingIncludeDataSpace.sourceDatabasePath != null && mappingIncludeDataSpace.targetDatabasePath != null)
        {
            if (!mappingIncludeDataSpace.sourceDatabasePath.equals(mappingIncludeDataSpace.getFullName()))
            {
                throw new EngineException("Referenced SourceDatabase " + mappingIncludeDataSpace.sourceDatabasePath + " must match the included DataSpace " + mappingIncludeDataSpace.getFullName(),
                        mappingIncludeDataSpace.sourceInformation,
                        EngineErrorType.COMPILATION);
            }
            Root_meta_pure_metamodel_dataSpace_DataSpace dataspace =
                    DataSpaceCompilerExtension.dataSpacesIndex.get(mappingInclude.getFullName());
            RichIterable<? extends Store> databases = DataSpaceCompilerExtension.getDatabasesUnderDataSpace(dataspace, context);
            for (Store database : databases)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SubstituteStore substituteStore = new Root_meta_pure_mapping_SubstituteStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::SubstituteStore"));
                substituteStore._owner(include);
                substituteStore._original(database);
                substituteStore._substitute(context.resolveStore(mappingIncludeDataSpace.targetDatabasePath));
                include._storeSubstitutionsAdd(substituteStore);
            }
        }

        return include;
    }
}
