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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.data;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;

import java.util.Set;

public class ServiceStoreEmbeddedDataCompiler
{
    public static Root_meta_pure_data_EmbeddedData compileServiceStoreEmbeddedDataCompiler(EmbeddedData embeddedData, CompileContext context, ProcessingContext processingContext)
    {
        if (embeddedData instanceof ServiceStoreEmbeddedData)
        {
            ServiceStoreEmbeddedData serviceStoreEmbeddedData = (ServiceStoreEmbeddedData) embeddedData;
            return new HelperServiceStoreEmbeddedDataCompiler(context, processingContext).compileServiceStoreEmbeddedData(serviceStoreEmbeddedData);
        }
        else
        {
            return null;
        }
    }

    public static void collectPrerequisiteElementsFromServiceStoreEmbeddedDataCompiler(Set<PackageableElementPointer> prerequisiteElements, EmbeddedData embeddedData, CompileContext context)
    {
        if (embeddedData instanceof ServiceStoreEmbeddedData)
        {
            ServiceStoreEmbeddedData serviceStoreEmbeddedData = (ServiceStoreEmbeddedData) embeddedData;
            new HelperServiceStoreEmbeddedDataCompiler(context).collectPrerequisiteElementsFromServiceStoreEmbeddedData(prerequisiteElements, serviceStoreEmbeddedData, context);
        }
    }
}
