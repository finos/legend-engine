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

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderPointerHandler;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class DataspaceStoreProviderPointerHandler extends StoreProviderPointerHandler
{
    @Override
    public Store resolveStore(StoreProviderPointer storeProviderPointer, CompileContext context)
    {
        String packageAddress = storeProviderPointer.path;
        if (!DataSpaceCompilerExtension.dataSpacesIndex.containsKey(packageAddress))
        {
            throw new EngineException("Dataspace " + packageAddress + " cannot be found.", storeProviderPointer.sourceInformation, EngineErrorType.COMPILATION);
        }
        Root_meta_pure_metamodel_dataSpace_DataSpace dataspace =
                DataSpaceCompilerExtension.dataSpacesIndex.get(packageAddress);
        ImmutableList<Store> stores = HelperMappingBuilder.getStoresFromMappingIgnoringIncludedMappings(dataspace._defaultExecutionContext()._mapping(),context);
        String dataspacePath = HelperModelBuilder.getElementFullPath(dataspace, context.pureModel.getExecutionSupport());
        String mappingPath = HelperModelBuilder.getElementFullPath(dataspace._defaultExecutionContext()._mapping(), context.pureModel.getExecutionSupport());
        if (stores.isEmpty())
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataspacePath + ") is not mapped to a store type supported by the ExtraSetImplementationSourceScanners.", storeProviderPointer.sourceInformation, EngineErrorType.COMPILATION);
        }
        else if (stores.size() > 1)
        {
            throw new EngineException("Default mapping (" + mappingPath + ") in dataspace (" + dataspacePath
                    + ") cannot be resolved to a single store. Stores found : ["
                    + stores.collect(s -> getElementFullPath(s, context.pureModel.getExecutionSupport())).makeString(",")
                    + "]. Please notify dataspace owners that their dataspace cannot be used as a store interface.",
                    storeProviderPointer.sourceInformation,
                    EngineErrorType.COMPILATION
            );
        }
        return stores.get(0);
    }
}
