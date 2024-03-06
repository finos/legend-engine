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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;
import org.finos.legend.pure.generated.Root_meta_external_store_model_ModelStore_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Map;

public interface StoreProviderCompilerHelper
{
    default Map<PackageableElementType, Function2<StoreProviderPointer, CompileContext, Store>> getExtraStoreProviderHandlers()
    {
        return Maps.mutable.empty();
    }

    static Store getStoreFromStoreProviderPointers(StoreProviderPointer storeProviderPointer, CompileContext context)
    {
        if (storeProviderPointer.path.equals("ModelStore"))
        {
            return new Root_meta_external_store_model_ModelStore_Impl("", null, context.pureModel.getClass("meta::external::store::model::ModelStore"));
        }
        Map<PackageableElementType, Function2<StoreProviderPointer, CompileContext, Store>> extraStoreProviderPointerHandlers = Maps.mutable.empty();
        ListIterate
                .selectInstancesOf(CompilerExtensions.fromAvailableExtensions().getExtensions(), StoreProviderCompilerHelper.class)
                .forEach(e -> extraStoreProviderPointerHandlers.putAll(e.getExtraStoreProviderHandlers()));
        return extraStoreProviderPointerHandlers.get(storeProviderPointer.type).apply(storeProviderPointer, context);
    }
}
