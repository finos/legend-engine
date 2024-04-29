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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils;

import com.zaxxer.hikari.pool.HikariProxyConnection;
import org.h2.jdbc.JdbcConnection;

import java.sql.Connection;

public class H2TestUtils
{
    public static JdbcConnection unwrapWrappedH2Connection(Connection connection)
    {
        try
        {
            return (JdbcConnection) ReflectionUtils.getFieldUsingReflection(HikariProxyConnection.class, connection, "delegate");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static JdbcConnection unwrapHikariProxyConnection(Connection connection)
    {
        try
        {
            return (JdbcConnection) ReflectionUtils.getFieldUsingReflection(HikariProxyConnection.class, connection, "delegate");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void closeProperly(Connection... connections)
    {
        for (Connection connection : connections)
        {
            if (connection == null)
            {
                continue;
            }
            try
            {
                connection.close();
            }
            catch (Exception ignored)
            {

            }
        }
    }
}