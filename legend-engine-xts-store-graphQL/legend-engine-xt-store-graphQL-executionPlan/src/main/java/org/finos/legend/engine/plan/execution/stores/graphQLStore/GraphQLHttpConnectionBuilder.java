// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.graphQLStore;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class GraphQLHttpConnectionBuilder
{
    public HttpClientBuilder httpClientBuilder;
    public RequestBuilder requestBuilder;

    public GraphQLHttpConnectionBuilder(HttpClientBuilder httpClientBuilder, RequestBuilder requestBuilder)
    {
        this.httpClientBuilder = httpClientBuilder;
        this.requestBuilder = requestBuilder;
    }

    public <T> T execute(ResponseHandler<T> responseHandler) throws Exception
    {
        HttpUriRequest request = requestBuilder.build();
        try (CloseableHttpClient httpClient = httpClientBuilder.build())
        {
            return httpClient.execute(request, responseHandler);
        }
    }
}
