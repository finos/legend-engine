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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

import java.util.Objects;

public class EmbeddedDataCompilerHelper
{
    public static Root_meta_data_Data compileEmbeddedData(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        return context.getCompilerExtensions().getExtraEmbeddedDataProcessors().stream()
                .map(processor -> processor.value(embeddedData, context, processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new EngineException("Unsupported embedded data type '" + embeddedData._type + "'", embeddedData.sourceInformation, EngineErrorType.COMPILATION));
    }

    public static Root_meta_data_Data compileCoreEmbeddedDataTypes(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        if (embeddedData instanceof BinaryData)
        {
            BinaryData binaryData = (BinaryData) embeddedData;
            return new Root_meta_data_BinaryData_Impl("")
                    ._contentType(binaryData.contentType)
                    ._hexData(binaryData.hexData);
        }
        else if (embeddedData instanceof PureCollectionData)
        {
            PureCollectionData pureCollectionData = (PureCollectionData) embeddedData;
            ValueSpecificationBuilder builder = new ValueSpecificationBuilder(context, Lists.mutable.empty(), processingContext);
            InstanceValue collection = (InstanceValue) pureCollectionData.data.accept(builder);

            return new Root_meta_data_PureCollectionData_Impl("")
                    ._collection(collection);
        }
        else if (embeddedData instanceof DataElementReference)
        {
            DataElementReference dataElementReference = (DataElementReference) embeddedData;
            PackageableElement element = context.pureModel.getPackageableElement(dataElementReference.dataElement, dataElementReference.sourceInformation);
            if (!(element instanceof Root_meta_data_DataElement))
            {
                throw new EngineException("Can only reference a Data element", dataElementReference.sourceInformation, EngineErrorType.COMPILATION);
            }

            return new Root_meta_data_DataElementReference_Impl("")
                    ._dataElement((Root_meta_data_DataElement) element);
            }
        else if (embeddedData instanceof TextData)
        {
            TextData textData = (TextData) embeddedData;
            return new Root_meta_data_TextData_Impl("")
                    ._contentType(textData.contentType)
                    ._data(textData.data);
        }
        else
        {
            return null;
        }
    }
}
