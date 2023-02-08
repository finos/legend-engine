// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.connection;

import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.PlatformCredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.AWSSecretsManagerVault;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.engine.connection.jdbc.JdbcConnectionProvider;
import org.finos.legend.engine.connection.jdbc.JdbcConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class TestJdbcConnectionProvider
{
    private Server h2Server;

    @Before
    public void setup() throws Exception
    {
        int port = DynamicPortGenerator.generatePort();
        this.h2Server = Server.createTcpServer("-ifNotExists", "-tcpPort", String.valueOf(port)).start();
    }

    @After
    public void shutdown()
    {
        if (this.h2Server == null)
        {
            return;
        }
        this.h2Server.stop();
    }

    @Test
    public void testH2ConnectionCreation() throws Exception
    {
        Properties properties = new Properties();
        properties.put("passwordRef1", "");
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(propertiesFileCredentialVault)
                .build();

        AWSSecretsManagerVault awsSecretsManagerVault = AWSSecretsManagerVault.builder()
                .with(platformCredentialVaultProvider)
                .build();

        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                .with(awsSecretsManagerVault)
                .build();

        IntermediationRuleProvider intermediationRuleProvider = IntermediationRuleProvider.builder()
                .with(new UserPasswordFromVaultRule(credentialVaultProvider))
                .build();

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(new UserPasswordCredentialProvider())
                .with(intermediationRuleProvider)
                .build();

        JdbcConnectionProvider connectionProvider = new JdbcConnectionProvider(credentialProviderProvider);
        JdbcConnectionSpecification connectionSpecification = new JdbcConnectionSpecification("localhost", this.h2Server.getPort(), JdbcConnectionSpecification.DbType.H2);
        UserPasswordAuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification("sa", new PropertiesFileSecret("passwordRef1"));

        Identity alice = new Identity("alice", new AnonymousCredential());
        Connection connection = connectionProvider.makeConnection(connectionSpecification, authenticationSpecification, alice);

        assertNotNull(connection);
    }
}
