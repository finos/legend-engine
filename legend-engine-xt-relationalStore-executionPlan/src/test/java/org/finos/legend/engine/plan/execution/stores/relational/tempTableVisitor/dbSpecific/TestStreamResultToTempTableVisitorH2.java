// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.tempTableVisitor.dbSpecific;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.tempTableVisitor.TestStreamResultToTempTableVisitor;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.Test;

import java.sql.SQLException;
import java.util.function.BiConsumer;

public class TestStreamResultToTempTableVisitorH2 extends TestStreamResultToTempTableVisitor
{
    public TestStreamResultToTempTableVisitorH2()
    {
        super(null);
    }

    @Test
    public void testLocalH2TempTableCreationUsingRealizedRelationalResult()
    {
        runWithLocalH2ServerCreation((server, port) -> testTempTableCreationUsingRealizedRelationalResult(getLocalH2Specification()));
    }

    @Test
    public void testLocalH2TempTableCreationUsingRelationalResult()
    {
        runWithLocalH2ServerCreation((server, port) -> testTempTableCreationUsingRelationalResult(getLocalH2Specification()));
    }

    private static LocalH2DataSourceSpecification getLocalH2Specification()
    {
        return new LocalH2DataSourceSpecification(
                Lists.mutable.empty(),
                new H2Manager(),
                new TestDatabaseAuthenticationStrategy()
        );
    }

    private static void runWithLocalH2ServerCreation(BiConsumer<Server, Integer> toRun)
    {
        int port = DynamicPortGenerator.generatePort();
        Server server = null;
        try
        {
            server = AlloyH2Server.startServer(port);
            toRun.accept(server, port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (server != null)
            {
                server.stop();
            }
        }
    }
}
