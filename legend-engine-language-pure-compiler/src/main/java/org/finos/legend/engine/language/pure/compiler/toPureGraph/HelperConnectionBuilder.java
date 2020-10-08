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

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelStringInput;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Any_Impl;
import org.finos.legend.pure.generated.core_json_jsonExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class HelperConnectionBuilder
{
    /**
     * For model connection, the store is optional; but if it is provided, we should verify that it must be "ModelStore"
     * This is to make sure in case that we have an model connection embedded in a runtime but indexed by the wrong store
     * (e.g. a flat-data store, or a database) we will throw error
     */
    public static void verifyModelConnectionStore(String store, SourceInformation storeSourceInformation)
    {
        if (store != null && !"ModelStore".equals(store))
        {
            throw new EngineException("Model connection must use 'ModelStore'", storeSourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static MutableMap<Class<?>, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<Root_meta_pure_metamodel_type_Any_Impl>> processModelInput(ModelStringInput modelInput, CompileContext context, boolean deserializeInstances, ExecutionSupport executionSupport)
    {
        Class<?> _class = context.resolveClass(modelInput._class);
        RichIterable<?> values = deserializeInstances ? ListIterate.collect(modelInput.instances, i -> core_json_jsonExtension.Root_meta_json_fromJson_String_1__Class_1__T_1_(cleanInstanceString(i), _class, executionSupport)) : Lists.immutable.withAll(modelInput.instances);
        org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List pureList = new Root_meta_pure_functions_collection_List_Impl<>("");
        pureList._values(values);
        return Maps.mutable.with(_class, pureList);
    }

    private static String cleanInstanceString(String instanceString)
    {
        return StringEscapeUtils.unescapeJava(instanceString.substring(1, instanceString.length() - 1));
    }
}
