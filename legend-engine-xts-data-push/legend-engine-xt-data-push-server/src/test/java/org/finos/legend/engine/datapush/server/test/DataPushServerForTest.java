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

package org.finos.legend.engine.datapush.server.test;

import org.finos.legend.authentication.vault.impl.EnvironmentCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.connection.AuthenticationConfigurationProvider;
import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstanceProvider;
import org.finos.legend.connection.impl.InstrumentedAuthenticationConfigurationProvider;
import org.finos.legend.connection.impl.InstrumentedStoreInstanceProvider;
import org.finos.legend.connection.impl.KerberosCredentialExtractor;
import org.finos.legend.connection.impl.StaticJDBCConnectionBuilder;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;
import org.finos.legend.connection.impl.UserPasswordCredentialBuilder;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.engine.datapush.server.DataPushServer;
import org.finos.legend.engine.datapush.DataPusher;
import org.finos.legend.engine.datapush.DataStager;
import org.finos.legend.engine.datapush.server.configuration.DataPushServerConfiguration;
import org.finos.legend.engine.server.support.server.BaseServer;

public class DataPushServerForTest extends DataPushServer
{
    public DataPushServerForTest()
    {
    }

    @Override
    protected BaseServer.ServerPlatformInfo newServerPlatformInfo()
    {
        return new ServerPlatformInfo(null, null, null);
    }

    public static void main(String... args) throws Exception
    {
        new DataPushServerForTest().run(args);
    }

    @Override
    public LegendEnvironment buildLegendEnvironment(DataPushServerConfiguration configuration)
    {
        return new LegendEnvironment.Builder()
                .withVaults(
                        new SystemPropertiesCredentialVault(),
                        new EnvironmentCredentialVault()
                )
                .withStoreSupports(
                        new RelationalDatabaseStoreSupport.Builder(DatabaseType.POSTGRES)
                                .withIdentifier("Postgres")
                                .withAuthenticationMechanismConfigurations(
                                        new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).withAuthenticationConfigurationTypes(
                                                UserPasswordAuthenticationConfiguration.class
                                        ).build()
                                )
                                .build()
                ).build();
    }

    @Override
    public IdentityFactory buildIdentityFactory(DataPushServerConfiguration configuration, LegendEnvironment environment)
    {
        return new IdentityFactory.Builder(environment)
                .build();
    }

    @Override
    public StoreInstanceProvider buildStoreInstanceProvider(DataPushServerConfiguration configuration, LegendEnvironment environment)
    {
        return new InstrumentedStoreInstanceProvider();
    }

    @Override
    public AuthenticationConfigurationProvider buildAuthenticationConfigurationProvider(DataPushServerConfiguration configuration, StoreInstanceProvider storeInstanceProvider, LegendEnvironment environment)
    {
        return new InstrumentedAuthenticationConfigurationProvider(this.storeInstanceProvider, this.environment);
    }

    @Override
    public ConnectionFactory buildConnectionFactory(DataPushServerConfiguration configuration, StoreInstanceProvider storeInstanceProvider, LegendEnvironment environment)
    {
        return new ConnectionFactory.Builder(environment, storeInstanceProvider)
                .withCredentialBuilders(
                        new KerberosCredentialExtractor(),
                        new UserPasswordCredentialBuilder()
                )
                .withConnectionBuilders(
                        new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword()
                )
                .build();
    }

    @Override
    public DataStager buildDataStager(DataPushServerConfiguration configuration)
    {
        // TODO
        return null;
    }

    @Override
    public DataPusher buildDataPusher(DataPushServerConfiguration configuration)
    {
        // TODO
        return null;
    }

    public LegendEnvironment getEnvironment()
    {
        return environment;
    }

    public IdentityFactory getIdentityFactory()
    {
        return identityFactory;
    }

    public InstrumentedStoreInstanceProvider getStoreInstanceProvider()
    {
        return (InstrumentedStoreInstanceProvider) storeInstanceProvider;
    }

    public InstrumentedAuthenticationConfigurationProvider getAuthenticationConfigurationProvider()
    {
        return (InstrumentedAuthenticationConfigurationProvider) authenticationConfigurationProvider;
    }
}