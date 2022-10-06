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

package org.finos.legend.engine.server;

import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.server.core.configuration.DeploymentConfiguration;
import org.finos.legend.engine.server.core.configuration.ErrorHandlingConfiguration;
import org.finos.legend.engine.server.core.configuration.OpenTracingConfiguration;
import org.finos.legend.engine.shared.core.vault.VaultConfiguration;
import org.finos.legend.server.pac4j.LegendPac4jConfiguration;

import java.util.List;
import java.util.Map;

public class ServerConfiguration extends Configuration
{
    // This can be set to avoid Jetty session cookie name collision between multiple servers running on `localhost` during development
    // See https://stackoverflow.com/questions/16789495/two-applications-on-the-same-server-use-the-same-jsessionid
    public String sessionCookie;
    public LegendPac4jConfiguration pac4j;
    public DeploymentConfiguration deployment = new DeploymentConfiguration();
    public SwaggerBundleConfiguration swagger;
    public OpenTracingConfiguration opentracing;
    public Map<String, Integer> filterPriorities;
    public MetaDataServerConfiguration metadataserver;
    public List<VaultConfiguration> vaults;
    public RelationalExecutionConfiguration relationalexecution;
    public ErrorHandlingConfiguration errorhandlingconfiguration = new ErrorHandlingConfiguration();

    /*
        This configuration has been deprecated in favor of the 'temporarytestdb' in RelationalExecutionConfiguration
     */
    @Deprecated
    public TemporaryTestDbConfiguration temporarytestdb;
}
