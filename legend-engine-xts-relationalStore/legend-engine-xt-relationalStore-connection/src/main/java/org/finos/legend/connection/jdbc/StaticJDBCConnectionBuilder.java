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
import org.finos.legend.connection.StoreSupport;
import org.finos.legend.connection.jdbc.driver.JDBCConnectionDriver;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class StaticJDBCConnectionBuilder
{
    public static class WithPlaintextUsernamePassword extends ConnectionBuilder<Connection, PlaintextUserPasswordCredential, StaticJDBCConnectionSpecification>
    {
        public Connection getConnection(PlaintextUserPasswordCredential credential, StaticJDBCConnectionSpecification connectionSpecification, StoreInstance storeInstance) throws Exception
        {
            StoreSupport storeSupport = storeInstance.getStoreSupport();
            if (!(storeSupport instanceof RelationalDatabaseStoreSupport))
            {
                throw new RuntimeException("Can't get connection: only support relational database stores");
            }
            JDBCConnectionDriver driver = JDBCConnectionManager.getDriverForDatabaseType(((RelationalDatabaseStoreSupport) storeSupport).getDatabaseType());
            return DriverManager.getConnection(
                    driver.buildURL(connectionSpecification.host, connectionSpecification.port, connectionSpecification.databaseName, new Properties()),
                    credential.getUser(), credential.getPassword()
            );
        }
    }
}
