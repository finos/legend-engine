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

package org.finos.legend.connection.jdbc;

import org.finos.legend.connection.ConnectionSetupFlow;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class StaticJDBCConnectionSetupFlow
{
    public static class WithPlaintextUsernamePassword implements ConnectionSetupFlow<Connection, StaticJDBCConnectionSetupSpecification, PlaintextUserPasswordCredential>
    {
        @Override
        public Class<StaticJDBCConnectionSetupSpecification> getConnectionSetupSpecificationClass()
        {
            return StaticJDBCConnectionSetupSpecification.class;
        }

        @Override
        public Class<PlaintextUserPasswordCredential> getCredentialClass()
        {
            return PlaintextUserPasswordCredential.class;
        }

        @Override
        public Connection setupConnection(StaticJDBCConnectionSetupSpecification connectionSetupSpecification, PlaintextUserPasswordCredential credential) throws Exception
        {
            JDBCConnectionManager connectionManager = JDBCConnectionManager.getManagerForDatabaseType(connectionSetupSpecification.databaseType.name());
            return DriverManager.getConnection(
                    connectionManager.buildURL(connectionSetupSpecification.host, connectionSetupSpecification.port, connectionSetupSpecification.databaseName, new Properties()),
                    credential.getUser(), credential.getPassword()
            );
        }
    }
}
