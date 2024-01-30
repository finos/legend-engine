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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedStoreHandler;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.IncludedStoreCarrier;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.List;
import java.util.Optional;

public class DataSpaceIncludedStoreHandler implements IncludedStoreHandler
{
    @Override
    public List<Store> resolveStore(IncludedStoreCarrier includedStoreCarrier, CompileContext context)
    {
        String packageAddress = includedStoreCarrier.path;
        if (!DataSpaceCompilerExtension.dataSpacesIndex.containsKey(packageAddress))
        {
            throw new EngineException("Dataspace " + packageAddress + " cannot be found.", includedStoreCarrier.sourceInformation, EngineErrorType.COMPILATION);
        }
        Root_meta_pure_metamodel_dataSpace_DataSpace dataspace =
                DataSpaceCompilerExtension.dataSpacesIndex.get(packageAddress);
        return Lists.mutable.withAll(Optional
                .ofNullable(dataspace._defaultExecutionContext()._stores())
                .orElseThrow(() -> new EngineException("Dataspace " + packageAddress + " does not have stores defined in its default execution context.", includedStoreCarrier.sourceInformation, EngineErrorType.COMPILATION)));

    }
}
