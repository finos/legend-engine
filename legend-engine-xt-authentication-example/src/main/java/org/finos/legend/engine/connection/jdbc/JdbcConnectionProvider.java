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

package org.finos.legend.engine.connection.jdbc;

import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.connection.ConnectionProvider;
import org.finos.legend.engine.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class JdbcConnectionProvider extends ConnectionProvider<Connection>
{
    public JdbcConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    @Override
    public Connection makeConnection(ConnectionSpecification connectionSpec, AuthenticationSpecification authenticationSpec, Identity identity) throws Exception
    {
        assert (connectionSpec instanceof JdbcConnectionSpecification);
        JdbcConnectionSpecification jdbcConnectionSpec = (JdbcConnectionSpecification) connectionSpec;

        Credential credential = super.makeCredential(authenticationSpec, identity);

        switch (jdbcConnectionSpec.dbType)
        {
            case H2:
                return connectToH2(jdbcConnectionSpec, credential, identity);
            default:
                throw new UnsupportedOperationException("Unsupported Db Type " + jdbcConnectionSpec.dbType);
        }
    }

    private Connection connectToH2(JdbcConnectionSpecification jdbcConnectionSpecification, Credential credential, Identity identity) throws Exception
    {
        if (!(credential instanceof PlaintextUserPasswordCredential))
        {
            String message = String.format("Failed to create connected. Expected credential of type %s but found credential of type %s", PlaintextUserPasswordCredential.class, credential.getClass());
            throw new UnsupportedOperationException(message);
        }
        PlaintextUserPasswordCredential plaintextUserPasswordCredential = (PlaintextUserPasswordCredential) credential;
        Class.forName("org.h2.Driver");
        Properties properties = new Properties();
        properties.setProperty("user", plaintextUserPasswordCredential.getUser());
        properties.setProperty("password", plaintextUserPasswordCredential.getPassword());
        String url = "jdbc:h2:tcp://" + jdbcConnectionSpecification.dbHostname + ":" + jdbcConnectionSpecification.dbPort + "/mem:" + "db1";
        Connection connection = DriverManager.getConnection(url);
        return connection;
    }
}