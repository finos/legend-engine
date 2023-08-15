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

package org.finos.legend.engine.datapush.server;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.PlatformCredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.ConnectionSetupFlowProvider;
import org.finos.legend.connection.DefaultConnectionSetupFlowProvider;

import java.util.Properties;
import java.util.function.Function;

public class ConnectionFactoryBundle<C extends Configuration> implements ConfiguredBundle<C>
{
    private static ConnectionFactory connectionFactory;
    private final Function<C, ConnectionFactoryConfiguration> configSupplier;

    public ConnectionFactoryBundle(Function<C, ConnectionFactoryConfiguration> configSupplier)
    {
        this.configSupplier = configSupplier;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    @Override
    public void run(C configuration, Environment environment)
    {
        final ConnectionFactoryConfiguration config = this.configSupplier.apply(configuration);
        // TODO: @akphi allow, through deployment configuration to load more credential providers
        // what we have right here is just the bare minimum for setting up the credential providers

        // TEMP: for testing
        Properties properties = new Properties();
        properties.put("passwordRef", "password");
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(propertiesFileCredentialVault)
                .build();

        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                .build();

        IntermediationRuleProvider intermediationRuleProvider = IntermediationRuleProvider.builder()
                .with(new UserPasswordFromVaultRule(credentialVaultProvider))
                .build();

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(new UserPasswordCredentialProvider())
                .with(intermediationRuleProvider)
                .build();

        ConnectionSetupFlowProvider connectionSetupFlowProvider = new DefaultConnectionSetupFlowProvider();
        connectionSetupFlowProvider.configure();

        connectionFactory = new ConnectionFactory(connectionSetupFlowProvider, credentialProviderProvider);
        connectionFactory.initialize();
    }

    public static ConnectionFactory getConnectionFactory()
    {
        if (connectionFactory == null)
        {
            throw new IllegalStateException("Connection factory has not been configured properly!");
        }
        return connectionFactory;
    }
}
