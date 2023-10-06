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

import org.finos.legend.connection.ConnectionBuilder;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.util.Properties;

public class StaticJDBCConnectionBuilder
{
    public static class WithPlaintextUsernamePassword extends ConnectionBuilder<Connection, PlaintextUserPasswordCredential, StaticJDBCConnectionSpecification>
    {
        public Connection getConnection(StoreInstance storeInstance, PlaintextUserPasswordCredential credential, AuthenticationConfiguration authenticationConfiguration, Identity identity) throws Exception
        {
            RelationalDatabaseStoreSupport storeSupport = RelationalDatabaseStoreSupport.cast(storeInstance.getStoreSupport());
            StaticJDBCConnectionSpecification connectionSpecification = this.getCompatibleConnectionSpecification(storeInstance);
            Properties properties = new Properties();
            properties.put("user", credential.getUser());
            properties.put("password", credential.getPassword());
            return JDBCConnectionManager.getConnection(storeSupport.getDatabase(), connectionSpecification.host, connectionSpecification.port, connectionSpecification.databaseName, identity, connectionSpecification, authenticationConfiguration, properties);
        }
    }
}
