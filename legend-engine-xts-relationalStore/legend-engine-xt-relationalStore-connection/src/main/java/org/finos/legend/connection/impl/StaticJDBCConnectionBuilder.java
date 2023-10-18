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

package org.finos.legend.connection.impl;

import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.JDBCConnectionBuilder;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.protocol.StaticJDBCConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.util.Properties;
import java.util.function.Function;

public class StaticJDBCConnectionBuilder
{
    public static class WithPlaintextUsernamePassword extends JDBCConnectionBuilder<PlaintextUserPasswordCredential, StaticJDBCConnectionSpecification>
    {
        public Connection getConnection(StaticJDBCConnectionSpecification connectionSpecification, Authenticator<PlaintextUserPasswordCredential> authenticator, Identity identity) throws Exception
        {
            StoreInstance storeInstance = authenticator.getStoreInstance();
            RelationalDatabaseStoreSupport storeSupport = RelationalDatabaseStoreSupport.cast(storeInstance.getStoreSupport());

            Properties connectionProperties = new Properties();
            Function<Credential, Properties> authenticationPropertiesSupplier = cred ->
            {
                PlaintextUserPasswordCredential credential = (PlaintextUserPasswordCredential) cred;
                Properties properties = new Properties();
                properties.put("user", credential.getUser());
                properties.put("password", credential.getPassword());
                return properties;
            };

            return this.getConnectionManager().getConnection(storeSupport.getDatabase(), connectionSpecification.host, connectionSpecification.port, connectionSpecification.databaseName, connectionProperties, this.getConnectionPoolConfig(), authenticationPropertiesSupplier, authenticator, identity);
        }
    }
}
