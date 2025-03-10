// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.jarService.compiler.toPureGraph;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph.ServiceCompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionParameters_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionParameters_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.List;

public class HelperJarServiceBuilder
{
    public static List<ServiceCompilerExtension> getServiceCompilerExtensions(CompileContext context)
    {
        return ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), ServiceCompilerExtension.class);
    }

    private static void inferEmbeddedRuntimeMapping(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime runtime, String mappingPath)
    {
        // If the runtime is embedded and no mapping is specified, we will take the mapping of the execution as the mapping for the runtime
        if (runtime instanceof EngineRuntime)
        {
            EngineRuntime engineRuntime = (EngineRuntime) runtime;
            if (engineRuntime.mappings.isEmpty())
            {
                PackageableElementPointer mappingPointer = new PackageableElementPointer();
                mappingPointer.sourceInformation = runtime.sourceInformation;
                mappingPointer.type = PackageableElementType.MAPPING;
                mappingPointer.path = mappingPath;
                engineRuntime.mappings.add(mappingPointer);
            }
        }
    }

    public static Root_meta_legend_service_metamodel_ExecutionParameters processExecutionParameters(ExecutionParameters params, CompileContext context)
    {
        if (params instanceof SingleExecutionParameters)
        {
            SingleExecutionParameters execParams = (SingleExecutionParameters) params;
            Mapping mapping = context.resolveMapping(execParams.mapping, execParams.mappingSourceInformation);
            inferEmbeddedRuntimeMapping(execParams.runtime, execParams.mapping);
            Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(execParams.runtime, context);
            HelperRuntimeBuilder.checkRuntimeMappingCoverage(runtime, Lists.fixedSize.of(mapping), context, execParams.runtime.sourceInformation);
            return new Root_meta_legend_service_metamodel_SingleExecutionParameters_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::SingleExecutionParameters"))
                    ._key(execParams.key)
                    ._mapping(mapping)
                    ._runtime(runtime);
        }
        else if (params instanceof MultiExecutionParameters)
        {
            MultiExecutionParameters execParams = (MultiExecutionParameters) params;
            return new Root_meta_legend_service_metamodel_MultiExecutionParameters_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::MultiExecutionParameters"))
                    ._masterKey(execParams.masterKey)
                    ._singleExecutionParameters(ListIterate.collect(execParams.singleExecutionParameters,
                            param -> (Root_meta_legend_service_metamodel_SingleExecutionParameters) processExecutionParameters(param, context)));
        }
        throw new UnsupportedOperationException("Unsupported service execution type '" + params.getClass().getSimpleName() + "'");
    }
}
