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

package org.finos.legend.engine.server.support.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.finos.legend.server.pac4j.LegendPac4jConfiguration;

import java.util.Map;

public class BaseServerConfiguration extends Configuration
{
    @JsonProperty("applicationName")
    private String applicationName;

    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @JsonProperty("pac4j")
    private LegendPac4jConfiguration pac4jConfiguration;

    @JsonProperty("cors")
    private CORSConfiguration corsConfiguration;

    @JsonProperty("filterPriorities")
    private Map<String, Integer> filterPriorities;

    @JsonProperty("errors")
    private ErrorHandlingConfiguration errorConfig;

    // This can be set to avoid Jetty session cookie name collision between multiple servers running on `localhost` during development
    // See https://stackoverflow.com/questions/16789495/two-applications-on-the-same-server-use-the-same-jsessionid
    @JsonProperty("sessionCookie")
    private String sessionCookie;

    public String getApplicationName()
    {
        return this.applicationName;
    }

    public SwaggerBundleConfiguration getSwaggerBundleConfiguration()
    {
        return this.swaggerBundleConfiguration;
    }

    public LegendPac4jConfiguration getPac4jConfiguration()
    {
        return this.pac4jConfiguration;
    }

    public CORSConfiguration getCORSConfiguration()
    {
        return this.corsConfiguration;
    }

    public Map<String, Integer> getFilterPriorities()
    {
        return this.filterPriorities;
    }

    public ErrorHandlingConfiguration getErrorHandlingConfiguration()
    {
        return this.errorConfig;
    }

    public String getSessionCookie()
    {
        return this.sessionCookie;
    }
}
