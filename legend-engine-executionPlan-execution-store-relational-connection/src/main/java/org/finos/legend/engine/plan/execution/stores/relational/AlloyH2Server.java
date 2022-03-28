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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.h2.tools.Server;
import org.slf4j.Logger;

import java.sql.SQLException;

public class AlloyH2Server extends Server
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AlloyH2Server.class);

    private static final String H2_ALLOWED_CLASSES_PROPERTY = "h2.allowedClasses";
    private static final String H2_ALLOWED_CLASSES_DEFAULT = "org.h2.*,org.finos.legend.engine.plan.execution.stores.relational.LegendH2Extensions";

    public static Server startServer(int port) throws SQLException
    {
        LOGGER.debug("startServer port {}", port);
        System.setProperty(H2_ALLOWED_CLASSES_PROPERTY, System.getProperty(H2_ALLOWED_CLASSES_PROPERTY, H2_ALLOWED_CLASSES_DEFAULT));
        return Server.createTcpServer("-ifNotExists", "-tcpPort", String.valueOf(port)).start();
    }

    public static void main(String[] args)
    {
        try
        {
            startServer(Integer.parseInt(args[0]));
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
