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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.contentPattern.ContentPatternFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceRequestPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceResponseDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStubMapping;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.StringValuePattern;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpMethod;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_data_ExternalFormatData;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceResponseDefinition;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceResponseDefinition_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceRequestPattern;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceRequestPattern_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceStoreEmbeddedData;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceStoreEmbeddedData_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceStubMapping;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_ServiceStubMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_service_metamodel_data_StringValuePattern;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Map;

public class HelperServiceStoreEmbeddedDataCompiler
{
    private CompileContext context;
    private ProcessingContext processingContext;

    public HelperServiceStoreEmbeddedDataCompiler(CompileContext context, ProcessingContext processingContext)
    {
        this.context = context;
        this.processingContext = processingContext;
    }

    public Root_meta_external_store_service_metamodel_data_ServiceStoreEmbeddedData compileServiceStoreEmbeddedData(ServiceStoreEmbeddedData serviceStoreEmbeddedData)
    {
        Root_meta_external_store_service_metamodel_data_ServiceStoreEmbeddedData pureServiceStoreEmbeddedData = new Root_meta_external_store_service_metamodel_data_ServiceStoreEmbeddedData_Impl("");
        pureServiceStoreEmbeddedData._serviceStubMappings(ListIterate.collect(serviceStoreEmbeddedData.serviceStubMappings, this::compileServiceStubMapping));

        return pureServiceStoreEmbeddedData;
    }

    private Root_meta_external_store_service_metamodel_data_ServiceStubMapping compileServiceStubMapping(ServiceStubMapping serviceStubMapping)
    {
        Root_meta_external_store_service_metamodel_data_ServiceStubMapping pureServiceStubMapping = new Root_meta_external_store_service_metamodel_data_ServiceStubMapping_Impl("");
        pureServiceStubMapping._requestPattern(this.compileServiceRequestPattern(serviceStubMapping.requestPattern));
        pureServiceStubMapping._responseDefinition(this.compileServiceResponseDefinition(serviceStubMapping.responseDefinition));

        return pureServiceStubMapping;
    }

    private Root_meta_external_store_service_metamodel_data_ServiceRequestPattern compileServiceRequestPattern(ServiceRequestPattern serviceRequestPattern)
    {
        validateServiceRequestPattern(serviceRequestPattern);

        Root_meta_external_store_service_metamodel_data_ServiceRequestPattern pureServiceRequestPattern = new Root_meta_external_store_service_metamodel_data_ServiceRequestPattern_Impl("");
        pureServiceRequestPattern._method(context.pureModel.getEnumValue("meta::pure::functions::io::http::HTTPMethod", serviceRequestPattern.method.name()));

        if (serviceRequestPattern.url != null)
        {
            pureServiceRequestPattern._url(serviceRequestPattern.url);
        }
        if (serviceRequestPattern.urlPath != null)
        {
            pureServiceRequestPattern._urlPath(serviceRequestPattern.urlPath);
        }

        if (serviceRequestPattern.queryParams != null && serviceRequestPattern.queryParams.size() > 0)
        {
            Map<String, Root_meta_external_store_service_metamodel_data_StringValuePattern> map = Maps.mutable.empty();
            serviceRequestPattern.queryParams.forEach((key, value) -> map.put(key, this.compileStringValuePattern(value)));

            pureServiceRequestPattern._headerParams(new PureMap(map));
        }

        if (serviceRequestPattern.headerParams != null && serviceRequestPattern.headerParams.size() > 0)
        {
            Map<String, Root_meta_external_store_service_metamodel_data_StringValuePattern> map = Maps.mutable.empty();
            serviceRequestPattern.headerParams.forEach((key, value) -> map.put(key, this.compileStringValuePattern(value)));

            pureServiceRequestPattern._headerParams(new PureMap(map));
        }

        if (serviceRequestPattern.bodyPatterns != null && serviceRequestPattern.bodyPatterns.size() > 0)
        {
            pureServiceRequestPattern._bodyPatterns(ListIterate.collect(serviceRequestPattern.bodyPatterns, this::compileStringValuePattern));
        }

        return pureServiceRequestPattern;
    }

    private void validateServiceRequestPattern(ServiceRequestPattern serviceRequestPattern)
    {
        if (serviceRequestPattern.method == HttpMethod.GET && (serviceRequestPattern.bodyPatterns != null && !serviceRequestPattern.bodyPatterns.isEmpty()))
        {
            throw new EngineException("Request Body pattern should not be provided for GET requests", serviceRequestPattern.sourceInformation, EngineErrorType.COMPILATION);
        }

        if (serviceRequestPattern.url == null && serviceRequestPattern.urlPath == null)
        {
            throw new EngineException("Either url or urlPath must be provided with each request", serviceRequestPattern.sourceInformation, EngineErrorType.COMPILATION);
        }

        if (serviceRequestPattern.url != null && serviceRequestPattern.urlPath != null)
        {
            throw new EngineException("Both url and urlPath must not be provided with any request", serviceRequestPattern.sourceInformation, EngineErrorType.COMPILATION);
        }

        if (serviceRequestPattern.url != null && (serviceRequestPattern.queryParams != null && !serviceRequestPattern.queryParams.isEmpty()))
        {
            throw new EngineException("urlPath (in place of url) should be used with query parameters", serviceRequestPattern.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private Root_meta_external_store_service_metamodel_data_StringValuePattern compileStringValuePattern(StringValuePattern stringValuePattern)
    {
        return (Root_meta_external_store_service_metamodel_data_StringValuePattern) stringValuePattern.accept(new ContentPatternFirstPassBuilder(this.context, this.processingContext));
    }

    private Root_meta_external_store_service_metamodel_data_ServiceResponseDefinition compileServiceResponseDefinition(ServiceResponseDefinition serviceResponseDefinition)
    {
        Root_meta_external_store_service_metamodel_data_ServiceResponseDefinition pureServiceResponseDefinition = new Root_meta_external_store_service_metamodel_data_ServiceResponseDefinition_Impl("");
        pureServiceResponseDefinition._body((Root_meta_external_shared_format_metamodel_data_ExternalFormatData) serviceResponseDefinition.body.accept(new EmbeddedDataFirstPassBuilder(this.context, this.processingContext)));

        return pureServiceResponseDefinition;
    }
}
