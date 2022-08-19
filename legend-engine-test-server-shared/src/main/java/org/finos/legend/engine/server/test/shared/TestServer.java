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

package org.finos.legend.engine.server.test.shared;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultConfiguration;
import org.finos.legend.engine.shared.core.vault.VaultFactory;
import org.finos.legend.server.pac4j.LegendPac4jBundle;
import java.util.List;

public class TestServer<T extends TestServerConfiguration> extends Application<T>
{
    private Environment environment;

    public static void main(String[] args) throws Exception
    {
        EngineUrlStreamHandlerFactory.initialize();
        new TestServer().run(args);
    }

    @Override
    public void initialize(Bootstrap<T> bootstrap)
    {
        bootstrap.addBundle(new LegendPac4jBundle<>(serverConfiguration -> serverConfiguration.pac4j));
        PureProtocolObjectMapperFactory.withPureProtocolExtensions(bootstrap.getObjectMapper());
        VaultFactory.withVaultConfigurationExtensions(bootstrap.getObjectMapper());
        ObjectMapperFactory.withStandardConfigurations(bootstrap.getObjectMapper());
    }

    @Override
    public void run(T serverConfiguration, Environment environment)
    {
        loadVaults(serverConfiguration.vaults);
        this.environment = environment;
        DeploymentStateAndVersions.DEPLOYMENT_MODE = DeploymentMode.DEV;
    }

    private void loadVaults(List<VaultConfiguration> vaultConfigurations)
    {
        if (vaultConfigurations != null)
        {
            ListIterate.forEach(vaultConfigurations, v -> Vault.INSTANCE.registerImplementation(VaultFactory.generateVaultImplementationFromConfiguration(v)));
        }
    }

    public void shutDown() throws Exception
    {
        this.environment.getApplicationContext().getServer().stop();
        CollectorRegistry.defaultRegistry.clear();
    }
}
