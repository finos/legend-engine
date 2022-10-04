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

package org.finos.legend.engine.plan.execution.stores.service.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.AfterClass;

import java.util.Objects;

public abstract class ServiceStoreTestSuite
{
    private static int port;
    private static WireMockServer testServer;

    public static void setupServer(String serverStubBaseDir)
    {
        port = DynamicPortGenerator.generatePort();
        testServer = new WireMockServer(port, new SingleRootFileSource(Objects.requireNonNull(ServiceStoreTestSuite.class.getClassLoader().getResource(serverStubBaseDir)).getFile()), false);
        testServer.start();
    }

    @AfterClass
    public static void teardownServer()
    {
        if (testServer != null)
        {
            testServer.stop();
        }
    }

    protected static int getPort()
    {
        return port;
    }
}
