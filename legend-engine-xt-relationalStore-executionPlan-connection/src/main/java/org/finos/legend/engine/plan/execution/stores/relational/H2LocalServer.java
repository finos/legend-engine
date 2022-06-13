//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational;

import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import java.sql.SQLException;

public class H2LocalServer
{
    private static final H2LocalServer INSTANCE = new H2LocalServer(DynamicPortGenerator.generatePort());
    private final int port;

    private H2LocalServer(int port)
    {
        this.port = port;
        try
        {
            // We can create only one instance of the server as the databases are anyway shared in the process (VM)
            // The important part is to have an empty name in 'this.buildDataSource("127.0.0.1", localH2Port, "", null)'
            // It ensure the database creation is 'private' to the connection.
            AlloyH2Server.startServer(port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

    }

    public static H2LocalServer getInstance()
    {
        return INSTANCE;
    }

    public int getPort()
    {
        return port;
    }
}
