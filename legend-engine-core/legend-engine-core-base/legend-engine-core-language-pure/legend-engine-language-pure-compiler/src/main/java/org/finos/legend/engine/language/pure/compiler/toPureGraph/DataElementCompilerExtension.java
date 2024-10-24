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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElementReference;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;

public class DataElementCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataElement");
    }

    @Override
    public CompilerExtension build()
    {
        return new DataElementCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        DataElement.class,
                        this::dataElementFirstPass,
                        this::dataElementSecondPass
                )
        );
    }

    private PackageableElement dataElementFirstPass(DataElement dataElement, CompileContext context)
    {
        Root_meta_pure_data_DataElement compiled = new Root_meta_pure_data_DataElement_Impl(dataElement.name, SourceInformationHelper.toM3SourceInformation(dataElement.sourceInformation), null);
        GenericType mappingGenericType = context.newGenericType(context.pureModel.getType("meta::pure::data::DataElement"));
        return compiled._classifierGenericType(mappingGenericType)
                ._stereotypes(ListIterate.collect(dataElement.stereotypes, context::resolveStereotype))
                ._taggedValues(ListIterate.collect(dataElement.taggedValues, context::newTaggedValue));
    }

    private void dataElementSecondPass(DataElement dataElement, CompileContext context)
    {
        String fullPath = context.pureModel.buildPackageString(dataElement._package, dataElement.name);
        Root_meta_pure_data_DataElement compiled = (Root_meta_pure_data_DataElement) context.pureModel.getPackageableElement(fullPath);

        ProcessingContext processingContext = new ProcessingContext("Data '" + fullPath + "' Second Pass");
        Root_meta_pure_data_EmbeddedData compiledData = dataElement.data.accept(new EmbeddedDataFirstPassBuilder(context, processingContext));
        if (compiledData instanceof Root_meta_pure_data_DataElementReference)
        {
            throw new EngineException("Cannot use Data element reference in a Data element", dataElement.data.sourceInformation, EngineErrorType.COMPILATION);
        }
        compiled._data(compiledData);
    }
}
