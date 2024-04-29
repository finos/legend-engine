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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

public class H2TestServerResource implements TestServerResource
{
    private org.h2.tools.Server h2Server = null;

    @Override
    public void start() throws Exception
    {
        int relationalDBPort;
        if (System.getProperty("legend.test.h2.port") == null)
        {
            relationalDBPort = DynamicPortGenerator.generatePort();
        }
        else
        {
            relationalDBPort = Integer.parseInt(System.getProperty("legend.test.h2.port"));
        }

        this.h2Server = AlloyH2Server.startServer(relationalDBPort);
        System.out.println("H2 database (Major Version:" + H2Manager.getMajorVersion() + ") started on port:" + relationalDBPort);

        // Set drop wizard vars
        System.setProperty("dw.temporarytestdb.port", String.valueOf(relationalDBPort));
        System.setProperty("dw.relationalexecution.temporarytestdb.port", String.valueOf(relationalDBPort));

        // Pure client configuration
        System.setProperty("alloy.test.h2.port", String.valueOf(relationalDBPort));
        System.setProperty("legend.test.h2.port", String.valueOf(relationalDBPort));
    }

    @Override
    public void shutDown() throws Exception
    {
        this.h2Server.shutdown();
        this.h2Server.stop();
    }
}
