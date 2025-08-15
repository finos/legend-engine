// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.pure.generated.Root_meta_core_runtime_EngineRuntime;
import org.finos.legend.pure.generated.Root_meta_core_runtime_EngineRuntime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class PackageableRuntimeCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "PackageableRuntime");
    }

    @Override
    public CompilerExtension build()
    {
        return new PackageableRuntimeCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        PackageableRuntime.class,
                        Lists.fixedSize.with(Mapping.class, PackageableConnection.class),
                        this::packageableRuntimeFirstPass,
                        (PackageableRuntime packageableRuntime, CompileContext context) ->
                        {
                        },
                        this::packageableRuntimeThirdPass,
                        this::packageableRuntimePrerequisiteElementsPass
                )
        );
    }

    private PackageableElement packageableRuntimeFirstPass(PackageableRuntime packageableRuntime, CompileContext context)
    {
        Root_meta_pure_runtime_PackageableRuntime metamodel = new Root_meta_pure_runtime_PackageableRuntime_Impl(packageableRuntime.name, SourceInformationHelper.toM3SourceInformation(packageableRuntime.sourceInformation), context.pureModel.getClass("meta::pure::runtime::PackageableRuntime"));

        Optional<Root_meta_core_runtime_EngineRuntime> pureRuntimeOptional = context.getCompilerExtensions().getExtraRuntimeValueProcessors().stream()
                .map(processor -> processor.value(packageableRuntime.runtimeValue, context))
                .filter(Objects::nonNull)
                .findFirst();

        Root_meta_core_runtime_EngineRuntime compiledRuntime;
        if (pureRuntimeOptional.isPresent())
        {
            compiledRuntime = pureRuntimeOptional.get();
        }
        else
        {
            // No processor recognized or compiled the runtimeValue, assume it is an EngineRuntime in this case.
            compiledRuntime = new Root_meta_core_runtime_EngineRuntime_Impl("Root::meta::core::runtime::EngineRuntime", SourceInformationHelper.toM3SourceInformation(packageableRuntime.sourceInformation), context.pureModel.getClass("meta::core::runtime::EngineRuntime"));
        }

        GenericType packageableRuntimeGenericType = context.newGenericType(context.pureModel.getType("meta::pure::runtime::PackageableRuntime"));
        metamodel._classifierGenericType(packageableRuntimeGenericType);
        return metamodel._runtimeValue(compiledRuntime);
    }

    private void packageableRuntimeThirdPass(PackageableRuntime packageableRuntime, CompileContext context)
    {
        String fullPath = context.pureModel.buildPackageString(packageableRuntime._package, packageableRuntime.name);
        Root_meta_pure_runtime_PackageableRuntime metamodel = context.pureModel.getPackageableRuntime(fullPath, packageableRuntime.sourceInformation);
        HelperRuntimeBuilder.buildEngineRuntime(packageableRuntime.runtimeValue, metamodel._runtimeValue(), context);
    }

    private Set<PackageableElementPointer> packageableRuntimePrerequisiteElementsPass(PackageableRuntime packageableRuntime, CompileContext context)
    {
        Set<PackageableElementPointer> prerequisiteElements = Sets.mutable.empty();
        HelperRuntimeBuilder.collectPrerequisiteElementsFromEngineRuntime(prerequisiteElements, packageableRuntime.runtimeValue);
        return prerequisiteElements;
    }
}
