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
import org.finos.legend.connection.AuthenticationMechanism;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseSupport;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.impl.CoreAuthenticationMechanismType;
import org.finos.legend.connection.impl.KerberosCredentialExtractor;
import org.finos.legend.connection.impl.RelationalDatabaseType;
import org.finos.legend.connection.impl.StaticJDBCConnectionBuilder;
import org.finos.legend.connection.impl.UserPasswordCredentialBuilder;
import org.finos.legend.engine.datapush.server.DataPushServer;
import org.finos.legend.engine.datapush.server.configuration.DataPushServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.UserPasswordAuthenticationConfiguration;

public class DataPushServerForTest extends DataPushServer
{
    public DataPushServerForTest()
    {
    }

    public static void main(String... args) throws Exception
    {
        new DataPushServerForTest().run(args);
    }

    @Override
    public LegendEnvironment buildLegendEnvironment(DataPushServerConfiguration configuration)
    {
        return LegendEnvironment.builder()
                .vaults(
                        new SystemPropertiesCredentialVault(),
                        new EnvironmentCredentialVault()
                )
                .databaseSupports(
                        DatabaseSupport.builder()
                                .type(RelationalDatabaseType.POSTGRES)
                                .authenticationMechanisms(
                                        AuthenticationMechanism.builder()
                                                .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                                .authenticationConfigurationTypes(
                                                        UserPasswordAuthenticationConfiguration.class
                                                ).build()
                                )
                                .build()
                ).build();
    }

    @Override
    public ConnectionFactory buildConnectionFactory(DataPushServerConfiguration configuration, LegendEnvironment environment)
    {
        return ConnectionFactory.builder()
                .environment(this.environment)
                .credentialBuilders(
                        new KerberosCredentialExtractor(),
                        new UserPasswordCredentialBuilder()
                )
                .connectionBuilders(
                        new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword()
                )
                .build();
    }

    public LegendEnvironment getEnvironment()
    {
        return environment;
    }
}