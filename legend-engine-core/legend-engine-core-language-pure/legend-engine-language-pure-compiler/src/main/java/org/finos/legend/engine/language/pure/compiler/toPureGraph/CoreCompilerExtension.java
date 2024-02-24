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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.MappingIncludedMappingHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.core.TestAssertionCompilerHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingIncludeMapping;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_TestAssertion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CoreCompilerExtension implements CompilerExtension, EmbeddedDataCompilerExtension
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
        return Collections.singletonList(EmbeddedDataCompilerExtension::compileCoreEmbeddedDataTypes);
    }

    @Override
    public List<Function3<TestAssertion, CompileContext, ProcessingContext, Root_meta_pure_test_assertion_TestAssertion>> getExtraTestAssertionProcessors()
    {
        return Collections.singletonList(TestAssertionCompilerHelper::compileCoreTestAssertionTypes);
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
    public CompilerExtension build()
    {
        return new CoreCompilerExtension();
    }
}
