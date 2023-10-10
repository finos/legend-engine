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
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.authentication.vault.impl.EnvironmentCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.impl.DefaultStoreInstanceProvider;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstanceProvider;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;

import java.util.List;
import java.util.function.Function;

public class ConnectionFactoryBundle<C extends Configuration> implements ConfiguredBundle<C>
{
    private static LegendEnvironment environment;
    private static IdentityFactory identityFactory;
    private static StoreInstanceProvider storeInstanceProvider;
    private static ConnectionFactory connectionFactory;
    private final List<CredentialVault> credentialVaults;
    private final Function<C, ConnectionFactoryConfiguration> configSupplier;

    public ConnectionFactoryBundle(Function<C, ConnectionFactoryConfiguration> configSupplier, List<CredentialVault> credentialVaults)
    {
        this.configSupplier = configSupplier;
        this.credentialVaults = credentialVaults;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    @Override
    public void run(C configuration, Environment environment)
    {
        ConnectionFactoryBundle.environment = new LegendEnvironment.Builder()
                // TODO: @akphi - add a property credential vault and load its content up from the config
//                .withVault(propertiesFileCredentialVault)
                .withVault(new SystemPropertiesCredentialVault())
                .withVault(new EnvironmentCredentialVault())
                .withVaults(this.credentialVaults)
                .withStoreSupport(new RelationalDatabaseStoreSupport.Builder(DatabaseType.POSTGRES)
                        .withIdentifier("Postgres")
                        .withAuthenticationMechanismConfigurations(
                                new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class).build()
                        ).build())
                .build();

        identityFactory = new IdentityFactory.Builder(ConnectionFactoryBundle.environment)
                .build();

        storeInstanceProvider = new DefaultStoreInstanceProvider.Builder().build();

        connectionFactory = new ConnectionFactory.Builder(ConnectionFactoryBundle.environment, storeInstanceProvider)
//                .withCredentialBuilderProvider(new DefaultCredentialBuilderProvider()) // can also use service loader
//                .withConnectionBuilderProvider(new DefaultConnectionBuilderProvider()) // can also use service loader
                .build();

        // TODO: register store instances
    }

    public static LegendEnvironment getEnvironment()
    {
        if (environment == null)
        {
            throw new IllegalStateException("Environment configuration has not been set!");
        }
        return environment;
    }

    public static IdentityFactory getIdentityFactory()
    {
        if (identityFactory == null)
        {
            throw new IllegalStateException("Identity factory has not been configured properly!");
        }
        return identityFactory;
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
