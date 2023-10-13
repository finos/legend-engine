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

package org.finos.legend.engine.datapush.server.impl;

import org.finos.legend.connection.ConnectionFactory;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.datapush.server.Data;
import org.finos.legend.engine.datapush.server.DataPusher;
import org.finos.legend.engine.datapush.server.SQLData;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCDataPusher extends DataPusher
{
    public JDBCDataPusher(ConnectionFactory connectionFactory)
    {
        super(connectionFactory);
    }

    @Override
    public void write(Identity identity, StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration, Data data) throws Exception
    {
        RelationalDatabaseStoreSupport.cast(storeInstance.getStoreSupport());
        Connection connection = this.connectionFactory.getConnection(identity, storeInstance, authenticationConfiguration);
        if (data instanceof SQLData)
        {
            SQLData sqlData = (SQLData) data;
            Statement stmt = connection.createStatement();
            sqlData.statements.forEach(statement ->
            {
                try
                {
                    stmt.executeUpdate(statement);
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e);
                }
            });
            connection.close();
            return;
        }
        // TODO: support CSV data - we need to do some parsing, or we need to have the schema provided somehow
        throw new RuntimeException(String.format("Can't push data of unsupported type '%s'", data.getClass().getSimpleName()));
    }
}
