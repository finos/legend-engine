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

package org.finos.legend.engine.server.core;

import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.federecio.dropwizard.swagger.SwaggerResource;

public class ServerShared
{
    public static void registerSwagger(Environment environment, SwaggerBundleConfiguration swaggerBundleConfiguration)
    {
        environment
                .jersey()
                .register(
                        new SwaggerResource(
                                "",
                                swaggerBundleConfiguration.getSwaggerViewConfiguration(),
                                swaggerBundleConfiguration.getSwaggerOAuth2Configuration(),
                                swaggerBundleConfiguration.getContextRoot() +
                                        (swaggerBundleConfiguration.getContextRoot().endsWith("/") ? "" : "/")
                                        + "api"));
    }

}
