// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake.jdbc;

import org.finos.legend.engine.persistence.components.executor.RelationalTransactionManager;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;

import java.sql.Connection;
import java.sql.SQLException;

public class SnowflakeJdbcHelper extends JdbcHelper
{
    private SnowflakeJdbcHelper(Connection connection)
    {
        super(connection);
    }

    @Override
    protected RelationalTransactionManager intializeTransactionManager(Connection connection)
    {
        try
        {
            return new SnowflakeJdbcTransactionManager(connection);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
