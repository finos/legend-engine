// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.plugin;

import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.connection.ElasticsearchHttpContextProvider;

public class ElasticsearchV7StoreState implements StoreState
{
    private final List<ElasticsearchHttpContextProvider> providers = ElasticsearchHttpContextProvider.providers();

    private final HttpClient client = TracingHttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(1000).setSocketTimeout(30000).build())
            .disableRedirectHandling()
            .setConnectionTimeToLive(5, TimeUnit.MINUTES)
            .build();

    @Override
    public StoreType getStoreType()
    {
        return StoreType.ESv7;
    }

    @Override
    public Object getStoreExecutionInfo()
    {
        return null;
    }

    public List<ElasticsearchHttpContextProvider> getProviders()
    {
        return this.providers;
    }

    public HttpClient getClient()
    {
        return this.client;
    }
}
