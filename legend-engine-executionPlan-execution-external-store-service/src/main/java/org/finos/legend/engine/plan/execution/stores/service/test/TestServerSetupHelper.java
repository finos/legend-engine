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

package org.finos.legend.engine.plan.execution.stores.service.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.service.test.contentPattern.ContentPatternToWiremockPatternGenerator;
import org.finos.legend.engine.plan.execution.stores.service.test.contentPattern.ContentPatternToWiremockPatternGeneratorExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToJsonPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.StringValuePattern;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.HttpMethod;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestServerSetupHelper
{
    private ServiceStoreEmbeddedData data;
    private WireMockServer testServer;


    public TestServerSetupHelper(ServiceStoreEmbeddedData data, int port)
    {
        this.data = data;
        this.testServer = new WireMockServer(port);
    }

    public WireMockServer setupTestServerWithData()
    {
        testServer.start();
        ListIterate.forEach(data.serviceStubMappings, this::getStubMapping);
        return testServer;
    }

    private void getStubMapping(ServiceStubMapping serviceStubMapping)
    {
        MappingBuilder requestMappingBuilder = getRequestMappingBuilder(serviceStubMapping.requestPattern);
        ResponseDefinitionBuilder responseDefinitionBuilder = getResponseDefinitionBuilder(serviceStubMapping.responseDefinition);

        testServer.stubFor(requestMappingBuilder.willReturn(responseDefinitionBuilder));
    }

    private static MappingBuilder getRequestMappingBuilder(ServiceRequestPattern requestPattern)
    {
        MappingBuilder builder;
        if (requestPattern.method == HttpMethod.GET)
        {
            if (requestPattern.url != null)
            {
                builder = WireMock.get(requestPattern.url);
            }
            else
            {
                builder = WireMock.get(urlPathEqualTo(requestPattern.urlPath));
            }
        }
        else if (requestPattern.method == HttpMethod.POST)
        {
            if (requestPattern.url != null)
            {
                builder = WireMock.post(requestPattern.url);
            }
            else
            {
                builder = WireMock.post(urlPathEqualTo(requestPattern.urlPath));
            }

            if(!requestPattern.bodyPatterns.isEmpty())
            {
                builder.withRequestBody(getWireMockStringValuePattern(requestPattern.bodyPatterns.get(0)));
            }
        }
        else
        {
            throw new UnsupportedOperationException("ServiceStore test server ");
        }

        if(requestPattern.queryParams != null)
        {
            requestPattern.queryParams.forEach((key, value) -> builder.withQueryParam(key, getWireMockStringValuePattern(value)));
        }
        if(requestPattern.headerParams != null)
        {
            requestPattern.headerParams.forEach((key, value) -> builder.withHeader(key, getWireMockStringValuePattern(value)));
        }

        return builder;
    }

    private static ResponseDefinitionBuilder getResponseDefinitionBuilder(ServiceResponseDefinition responseDefinition)
    {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", ((ExternalFormatData) responseDefinition.body).contentType)
                .withBody(((ExternalFormatData) responseDefinition.body).data);
    }

    private static com.github.tomakehurst.wiremock.matching.StringValuePattern getWireMockStringValuePattern(StringValuePattern stringValuePattern)
    {
        Optional<ContentPatternToWiremockPatternGenerator> generator = ListIterate.detectOptional(ContentPatternToWiremockPatternGeneratorExtensionLoader.extensions(), ext -> ext.supports(stringValuePattern));
        if (!generator.isPresent())
        {
            throw new RuntimeException("No generator found for content pattern for type - " + stringValuePattern.getClass().getSimpleName());
        }

        return generator.get().generate(stringValuePattern);
    }
}
