// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationPrerequisiteElementsPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface EmbeddedDataCompilerHelper
{
    static Root_meta_pure_data_EmbeddedData compileCoreEmbeddedDataTypes(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        SourceInformation m3SourceInformation = SourceInformationHelper.toM3SourceInformation(embeddedData.sourceInformation);

        if (embeddedData instanceof ExternalFormatData)
        {
            //TODO: Have extension mechanism to validate data with respect to contentType
            ExternalFormatData externalFormatData = (ExternalFormatData) embeddedData;
            return new Root_meta_external_format_shared_metamodel_data_ExternalFormatData_Impl("", m3SourceInformation, context.pureModel.getClass("meta::external::format::shared::metamodel::data::ExternalFormatData"))
                    ._contentType(externalFormatData.contentType)
                    ._data(externalFormatData.data);
        }
        else if (embeddedData instanceof ModelStoreData)
        {
            ModelStoreData modelStoreData = (ModelStoreData) embeddedData;
            ValueSpecificationBuilder builder = new ValueSpecificationBuilder(context, Lists.mutable.empty(), processingContext);
            Root_meta_pure_data_ModelStoreData metamodelModelStoreData =  new Root_meta_pure_data_ModelStoreData_Impl("", m3SourceInformation, context.pureModel.getClass("meta::pure::data::ModelStoreData"));
            if (modelStoreData.modelData == null || modelStoreData.modelData.isEmpty())
            {
                throw new EngineException("No data provided for Model Store", modelStoreData.sourceInformation, EngineErrorType.COMPILATION);
            }
            for (ModelTestData modelTestData : modelStoreData.modelData)
            {
                SourceInformation modelTestDataSourceInformation = SourceInformationHelper.toM3SourceInformation(modelTestData.sourceInformation);

                if (modelTestData instanceof ModelEmbeddedTestData)
                {
                    Root_meta_pure_data_ModelEmbeddedData mmModelData = new Root_meta_pure_data_ModelEmbeddedData_Impl("", modelTestDataSourceInformation, context.pureModel.getClass("meta::pure::data::ModelEmbeddedData"));
                    mmModelData._model(context.resolveClass(modelTestData.model, modelTestData.sourceInformation));
                    mmModelData._data(
                        context.getCompilerExtensions().getExtraEmbeddedDataProcessors().stream().map(processor -> processor.value(((ModelEmbeddedTestData) modelTestData).data, context, processingContext)).filter(Objects::nonNull)
                            .findFirst().orElseThrow(() -> new EngineException("Unsupported Embedded Data for Model Store", modelTestData.sourceInformation, EngineErrorType.COMPILATION)));
                    metamodelModelStoreData._modelTestDataAdd(mmModelData);
                }
                else if (modelTestData instanceof ModelInstanceTestData)
                {
                    ModelInstanceTestData modelInstanceData = (ModelInstanceTestData) modelTestData;
                    Root_meta_pure_data_ModelInstanceData mmModelInstanceData = new Root_meta_pure_data_ModelInstanceData_Impl("", modelTestDataSourceInformation, context.pureModel.getClass("meta::pure::data::ModelInstanceData"));
                    Class<?> c = context.resolveClass(modelInstanceData.model, modelInstanceData.sourceInformation);
                    InstanceValue collection = (InstanceValue) modelInstanceData.instances.accept(builder);

                    if (!(collection._genericType()._rawType().equals(c) || collection._genericType()._rawType()._generalizations().contains(c) || (modelInstanceData.instances instanceof PackageableElementPtr
                        && validatePairForModelStoreData((PackageableElementPtr) modelInstanceData.instances, context))))
                    {
                        throw new EngineException("Instance types does not align with associated type '" + modelInstanceData.model + "'", modelInstanceData.instances.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    mmModelInstanceData._instances(collection)._model(c);
                    metamodelModelStoreData._modelTestDataAdd(mmModelInstanceData);
                }
            }
            return metamodelModelStoreData;
        }
        else if (embeddedData instanceof DataElementReference
                && ((DataElementReference) embeddedData).dataElement.type.equals(PackageableElementType.DATA)
        )
        {
            DataElementReference dataElementReference = (DataElementReference) embeddedData;
            PackageableElement element = context.pureModel.getPackageableElement(dataElementReference.dataElement.path, dataElementReference.sourceInformation);
            if (!(element instanceof Root_meta_pure_data_DataElement))
            {
                throw new EngineException("Can only reference a Data element", dataElementReference.sourceInformation, EngineErrorType.COMPILATION);
            }

            return new Root_meta_pure_data_DataElementReference_Impl("", null, context.pureModel.getClass("meta::pure::data::DataElementReference"))
                    ._dataElement((Root_meta_pure_data_DataElement) element);
        }
        else
        {
            return null;
        }
    }

    static void collectPrerequisiteElementsFromCoreEmbeddedDataTypes(Set<PackageableElementPointer> prerequisiteElements, EmbeddedData embeddedData, CompileContext context)
    {
        if (embeddedData instanceof ModelStoreData)
        {
            ModelStoreData modelStoreData = (ModelStoreData) embeddedData;
            ValueSpecificationPrerequisiteElementsPassBuilder valueSpecificationPrerequisiteElementsPassBuilder = new ValueSpecificationPrerequisiteElementsPassBuilder(context, prerequisiteElements);
            for (ModelTestData modelTestData : modelStoreData.modelData)
            {
                prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.CLASS, modelTestData.model, modelTestData.sourceInformation));
                if (modelTestData instanceof ModelEmbeddedTestData)
                {
                    context.getCompilerExtensions().getExtraEmbeddedDataPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(prerequisiteElements, ((ModelEmbeddedTestData) modelTestData).data, context));
                }
                else if (modelTestData instanceof ModelInstanceTestData)
                {
                    ModelInstanceTestData modelInstanceData = (ModelInstanceTestData) modelTestData;
                    modelInstanceData.instances.accept(valueSpecificationPrerequisiteElementsPassBuilder);
                }
            }
        }
        else if (embeddedData instanceof DataElementReference
                && ((DataElementReference) embeddedData).dataElement.type.equals(PackageableElementType.DATA)
        )
        {
            DataElementReference dataElementReference = (DataElementReference) embeddedData;
            prerequisiteElements.add(new PackageableElementPointer(PackageableElementType.DATAELEMENT, dataElementReference.dataElement.path, dataElementReference.sourceInformation));
        }
    }

    static boolean validatePairForModelStoreData(PackageableElementPtr value, CompileContext context)
    {
        return (context.pureModel.getPackageableElement(value.fullPath) instanceof Root_meta_pure_data_DataElement &&
                ((Root_meta_pure_data_DataElement) context.pureModel.getPackageableElement(value.fullPath))._data() instanceof Root_meta_external_format_shared_metamodel_data_ExternalFormatData
        );
    }
    
    default Iterable<? extends Function2<DataElementReference, PureModelContextData, List<EmbeddedData>>> getExtraDataElementReferencePMCDTraversers()
    {
        return Collections.emptyList();
    }
    
    static EmbeddedData getEmbeddedDataFromDataElement(DataElementReference dataElementReference, PureModelContextData pureModelContextData)
    {
        List<EmbeddedData> dataList = ListIterate
                .selectInstancesOf(CompilerExtensions.fromAvailableExtensions().getExtensions(), EmbeddedDataCompilerHelper.class)
                .flatCollect(EmbeddedDataCompilerHelper::getExtraDataElementReferencePMCDTraversers)
                .flatCollect(f -> f.apply(dataElementReference, pureModelContextData))
                .select(Objects::nonNull);
        if (dataList.size() > 1)
        {
            throw new EngineException("More than one data element found at the address " + dataElementReference.dataElement.path, dataElementReference.sourceInformation, EngineErrorType.COMPILATION);
        }
        else if (dataList.isEmpty())
        {
            throw new EngineException("No data element found at the address " + dataElementReference.dataElement.path, dataElementReference.sourceInformation, EngineErrorType.COMPILATION);
        }
        else
        {
            return dataList.get(0);
        }
    }
}