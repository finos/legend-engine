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

import io.dropwizard.setup.Bootstrap;
import org.finos.legend.authentication.vault.impl.EnvironmentCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.connection.AuthenticationMechanism;
import org.finos.legend.connection.Connection;
import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.DatabaseSupport;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.IdentityFactory;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.impl.CoreAuthenticationMechanismType;
import org.finos.legend.connection.impl.KerberosCredentialExtractor;
import org.finos.legend.connection.impl.KeyPairCredentialBuilder;
import org.finos.legend.connection.impl.RelationalDatabaseType;
import org.finos.legend.connection.impl.SnowflakeConnectionBuilder;
import org.finos.legend.connection.impl.StaticJDBCConnectionBuilder;
import org.finos.legend.connection.impl.UserPasswordCredentialBuilder;
import org.finos.legend.engine.datapush.DataPusher;
import org.finos.legend.engine.datapush.DataPusherProvider;
import org.finos.legend.engine.datapush.impl.SnowflakeWithS3StageDataPusher;
import org.finos.legend.engine.datapush.server.configuration.DataPushServerConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.UserPasswordAuthenticationConfiguration;
import org.finos.legend.engine.server.support.server.config.BaseServerConfiguration;
import org.finos.legend.server.pac4j.LegendPac4jBundle;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class DataPushServer extends BaseDataPushServer
{
    @Override
    protected ServerPlatformInfo newServerPlatformInfo()
    {
        return new ServerPlatformInfo(null, null, null);
    }

    @Override
    public void initialize(Bootstrap<DataPushServerConfiguration> bootstrap)
    {
        super.initialize(bootstrap);

        bootstrap.addBundle(new LegendPac4jBundle<>(BaseServerConfiguration::getPac4jConfiguration));
    }

    public static void main(String... args) throws Exception
    {
        new DataPushServer().run(args);
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
                                                .type(CoreAuthenticationMechanismType.USER_PASSWORD).authenticationConfigurationTypes(
                                                        UserPasswordAuthenticationConfiguration.class
                                                ).build()
                                )
                                .build(),
                        DatabaseSupport.builder()
                                .type(RelationalDatabaseType.SNOWFLAKE)
                                .authenticationMechanisms(
                                        AuthenticationMechanism.builder()
                                                .type(CoreAuthenticationMechanismType.KEY_PAIR).authenticationConfigurationTypes(
                                                        EncryptedPrivateKeyPairAuthenticationConfiguration.class
                                                ).build()
                                )
                                .build()
                ).build();
    }

    @Override
    public IdentityFactory buildIdentityFactory(DataPushServerConfiguration configuration, LegendEnvironment environment)
    {
        return IdentityFactory.builder()
                .environment(environment)
                .build();
    }

    @Override
    public ConnectionFactory buildConnectionFactory(DataPushServerConfiguration configuration, LegendEnvironment environment)
    {
        return ConnectionFactory.builder()
                .environment(this.environment)
                .credentialBuilders(
                        new KerberosCredentialExtractor(),
                        new UserPasswordCredentialBuilder(),
                        new KeyPairCredentialBuilder()
                )
                .connectionBuilders(
                        new StaticJDBCConnectionBuilder.WithPlaintextUsernamePassword(),
                        new SnowflakeConnectionBuilder.WithKeyPair()
                )
                .build();
    }

    @Override
    public DataPusherProvider buildDataPushProvider()
    {
        return new DataPusherProvider()
        {
            @Override
            public DataPusher getDataPusher(Connection connection)
            {
                DatabaseType databaseType = connection.getDatabaseSupport().getDatabaseType();
                if (RelationalDatabaseType.SNOWFLAKE.equals(databaseType))
                {
                    String tableName = "DEMO_DB.SCHEMA1.TABLE1";
                    String stageName = "DEMO_DB.SCHEMA1.STAGE1";
                    return new SnowflakeWithS3StageDataPusher("legend-dpsh1", null, StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    "xxxxx", // NOTE: secret - to be removed when committed
                                    "xxxxx" // NOTE: secret - to be removed when committed
                            )), tableName, stageName);
                }
                throw new UnsupportedOperationException("Unsupported database type: " + databaseType.getIdentifier());
            }
        };
    }
}
