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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.finos.legend.engine.store.core.LegendStoreCommand;
import org.finos.legend.engine.store.core.LegendStoreConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LegendPostgresCurrentUserCommand implements LegendStoreCommand<Connection, String>
{
    private LegendStoreConnectionProvider<Connection> connectionProvider;

    @Override
    public void initialize(LegendStoreConnectionProvider<Connection> connectionProvider) throws Exception
    {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public String run() throws Exception
    {
        try (Connection connection = this.connectionProvider.getConnection())
        {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select current_user;");
            resultSet.next();
            return  resultSet.getString(1);
        }
    }
}
