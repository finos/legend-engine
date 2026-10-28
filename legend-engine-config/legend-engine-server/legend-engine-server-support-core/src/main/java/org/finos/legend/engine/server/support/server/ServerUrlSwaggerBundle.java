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

package org.finos.legend.engine.server.support.server;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.federecio.dropwizard.swagger.ConfigurationHelper;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.servers.Server;

import java.util.Collections;

/**
 * {@code SwaggerBundleConfiguration.build()} sets no OpenAPI {@code servers}, so Swagger UI sends
 * "Try it out" requests to {@code /} and they 404 under a non-root mount such as {@code /api}.
 *
 * <p>The context must be registered before {@code super.run}, which then reuses it. Bundles that
 * build their own OpenAPI configuration can call {@link #withServerUrl} instead.
 */
public abstract class ServerUrlSwaggerBundle<T extends Configuration> extends SwaggerBundle<T>
{
    @Override
    public void run(T configuration, Environment environment) throws Exception
    {
        SwaggerBundleConfiguration swaggerBundleConfiguration = getSwaggerBundleConfiguration(configuration);
        if (swaggerBundleConfiguration != null && swaggerBundleConfiguration.isEnabled())
        {
            new JaxrsOpenApiContextBuilder()
                    .openApiConfiguration(buildOpenApiConfiguration(configuration, swaggerBundleConfiguration))
                    .buildContext(true);
        }
        super.run(configuration, environment);
    }

    static SwaggerConfiguration buildOpenApiConfiguration(Configuration configuration, SwaggerBundleConfiguration swaggerBundleConfiguration)
    {
        return withServerUrl(swaggerBundleConfiguration.build(), configuration, swaggerBundleConfiguration);
    }

    /**
     * Names the Jersey URL pattern as the only server of {@code oasConfiguration}'s OpenAPI, unless
     * Jersey is mounted at the root, where Swagger UI's default server is already right.
     */
    public static SwaggerConfiguration withServerUrl(SwaggerConfiguration oasConfiguration, Configuration configuration, SwaggerBundleConfiguration swaggerBundleConfiguration)
    {
        String urlPattern = new ConfigurationHelper(configuration, swaggerBundleConfiguration).getUrlPattern();
        // ConfigurationHelper reports a root mount as "" or "/"
        if (!urlPattern.isEmpty() && !"/".equals(urlPattern))
        {
            oasConfiguration.getOpenAPI().setServers(Collections.singletonList(new Server().url(urlPattern)));
        }
        return oasConfiguration;
    }
}
