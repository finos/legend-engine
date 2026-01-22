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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.MappingIncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.core.TestAssertionCompilerHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingIncludeMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_TestAssertion;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CoreCompilerExtension extends FunctionDispatchExtension implements CompilerExtension, EmbeddedDataCompilerHelper, StoreProviderCompilerHelper
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Core");
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Function3<EmbeddedData, CompileContext, ProcessingContext, Root_meta_pure_data_EmbeddedData>> getExtraEmbeddedDataProcessors()
    {
        return Collections.singletonList(EmbeddedDataCompilerHelper::compileCoreEmbeddedDataTypes);
    }

    @Override
    public List<Procedure3<Set<PackageableElementPointer>, EmbeddedData, CompileContext>> getExtraEmbeddedDataPrerequisiteElementsPassProcessors()
    {
        return Collections.singletonList(EmbeddedDataCompilerHelper::collectPrerequisiteElementsFromCoreEmbeddedDataTypes);
    }

    @Override
    public List<Function3<TestAssertion, CompileContext, ProcessingContext, Root_meta_pure_test_assertion_TestAssertion>> getExtraTestAssertionProcessors()
    {
        return Collections.singletonList(TestAssertionCompilerHelper::compileCoreTestAssertionTypes);
    }

    @Override
    public List<Procedure3<Set<PackageableElementPointer>, TestAssertion, CompileContext>> getExtraTestAssertionPrerequisiteElementsPassProcessors()
    {
        return Collections.singletonList(TestAssertionCompilerHelper::collectPrerequisiteElementsFromCoreTestAssertionTypes);
    }

    @Override
    public Map<String, IncludedMappingHandler> getExtraIncludedMappingHandlers()
    {
        return Maps.mutable.of(
                MappingIncludeMapping.class.getName(), new MappingIncludedMappingHandler()
        );
    }

    @Override
    public Iterable<? extends Function2<DataElementReference, PureModelContextData, List<EmbeddedData>>> getExtraDataElementReferencePMCDTraversers()
    {
        return Collections.singletonList(CoreCompilerExtension::getDataFromDataReferencePMCD);
    }

    private static List<EmbeddedData> getDataFromDataReferencePMCD(DataElementReference dataElementReference, PureModelContextData pureModelContextData)
    {
        return ListIterate
                .select(pureModelContextData.getElementsOfType(DataElement.class), e -> dataElementReference.dataElement.path.equals(e.getPath()))
                .collect(d -> d.data);
    }

    @Override
    public Map<PackageableElementType, Function2<StoreProviderPointer, CompileContext, Store>> getExtraStoreProviderHandlers()
    {
        return Maps.mutable.of(PackageableElementType.STORE, CoreCompilerExtension::resolveStore);
    }

    private static Store resolveStore(StoreProviderPointer storeProviderPointer, CompileContext context)
    {
        return context.resolveStore(storeProviderPointer.path, storeProviderPointer.sourceInformation);
    }

    @Override
    public CompilerExtension build()
    {
        return new CoreCompilerExtension();
    }

    @Override
    public List<Procedure3<SetImplementation, Set<String>, CompileContext>> getExtraSetImplementationSourceScanners()
    {
        return Collections.singletonList((setImplementation, scannedSources, context) ->
        {
            if (setImplementation instanceof PureInstanceSetImplementation && ((PureInstanceSetImplementation) setImplementation)._srcClass() != null)
            {
                scannedSources.add("ModelStore");
            }
        });
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return Lists.mutable.withAll(super.getExtraFunctionHandlerDispatchBuilderInfoCollectors())
                .with((handlers) -> Lists.mutable.with(
                        // TODO this function are been use incorrectly, hence we cannot apply the proper compiler constrains that the auto-generated code will enforce
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::annualized_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::cme_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::cw_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::cw_fm_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::CYMinus2_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::CYMinus3_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::mtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p12wa_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p12mtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p12wtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p4wa_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p4wtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p52wtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::p52wa_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pma_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pmtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pqtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::priorDay_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::priorYear_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pw_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pw_fm_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pwa_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pwtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pymtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pyqtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pytd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pywa_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::pywtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::qtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::reportEndDay_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::wtd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true),
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::functions::date::calendar::ytd_Date_$0_1$__String_1__Date_1__Number_$0_1$__Number_$0_1$_", (List<ValueSpecification> ps) -> true)
                ));
    }
}
