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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_data_ExternalFormatData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElement;
import org.finos.legend.pure.generated.Root_meta_pure_data_DataElementReference_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_data_ModelStoreData_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Map;

public class EmbeddedDataCompilerHelper
{
    public static Root_meta_pure_data_EmbeddedData compileCoreEmbeddedDataTypes(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        if (embeddedData instanceof ExternalFormatData)
        {
            //TODO: Have extension mechanism to validate data with respect to contentType
            ExternalFormatData externalFormatData = (ExternalFormatData) embeddedData;
            return new Root_meta_external_shared_format_metamodel_data_ExternalFormatData_Impl("", null, context.pureModel.getClass("meta::external::shared::format::metamodel::data::ExternalFormatData"))
                    ._contentType(externalFormatData.contentType)
                    ._data(externalFormatData.data);
        }
        else if (embeddedData instanceof ModelStoreData)
        {
            ModelStoreData modelStoreData = (ModelStoreData) embeddedData;
            ValueSpecificationBuilder builder = new ValueSpecificationBuilder(context, Lists.mutable.empty(), processingContext);

            Map<Class<?>, InstanceValue> typeIndexedInstances = Maps.mutable.empty();

            for (Map.Entry<String, ValueSpecification> entry : modelStoreData.instances.entrySet())
            {
                Class<?> c = context.resolveClass(entry.getKey(), entry.getValue().sourceInformation);
                InstanceValue collection = (InstanceValue) entry.getValue().accept(builder);

                if (!(collection._genericType()._rawType().equals(c) || collection._genericType()._rawType()._generalizations().contains(c)))
                {
                    throw new EngineException("Instance types does not align with associated type", entry.getValue().sourceInformation, EngineErrorType.COMPILATION);
                }

                typeIndexedInstances.put(c, collection);
            }

            return new Root_meta_pure_data_ModelStoreData_Impl("", null, context.pureModel.getClass("meta::pure::data::ModelStoreData"))
                    ._instances(new PureMap(typeIndexedInstances));
        }
        else if (embeddedData instanceof DataElementReference)
        {
            DataElementReference dataElementReference = (DataElementReference) embeddedData;
            PackageableElement element = context.pureModel.getPackageableElement(dataElementReference.dataElement, dataElementReference.sourceInformation);
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
}
