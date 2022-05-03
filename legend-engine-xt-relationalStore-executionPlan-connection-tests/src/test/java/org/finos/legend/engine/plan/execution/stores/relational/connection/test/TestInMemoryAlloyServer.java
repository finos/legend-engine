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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.Assert.*;

public class TestInMemoryAlloyServer
{
    @Test
    public void connectionsOnDifferentPortsSameDatabaseNotUnique() throws Exception
    {
        int port1 = DynamicPortGenerator.generatePort();
        Server server1 = AlloyH2Server.startServer(port1);
        ConnectionManagerSelector selector1 = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(port1), Collections.emptyList());

        // the first table creation is successful
        Connection connection1 = selector1.getTestDatabaseConnection();
        Statement statement1 = connection1.createStatement();
        statement1.execute("create table test(a varchar(100))");

        int port2 = DynamicPortGenerator.generatePort();
        Server server2 = AlloyH2Server.startServer(port2);
        ConnectionManagerSelector selector2 = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(port2), Collections.emptyList());

        // the second table creation fails, even though it uses a unique port, as the database names are the same.
        // with the memory mode, the port number does not provide for unqiue databases
        try
        {
            Connection connection2 = selector2.getTestDatabaseConnection();
            Statement statement2 = connection2.createStatement();
            statement2.execute("create table test(a varchar(100))");
            fail("Failed to get exception");
        }
        catch (SQLException e)
        {
            assertTrue(e.getMessage().contains("Table \"TEST\" already exists; SQL statement:"));
        }

        finally
        {
            close(server1);
            close(server2);
        }
    }

    private void close(Server server)
    {
        if (server == null)
        {
            return;
        }
        server.shutdown();
        server.stop();
    }
}