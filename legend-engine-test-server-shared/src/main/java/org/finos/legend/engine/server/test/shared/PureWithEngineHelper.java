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

package org.finos.legend.engine.server.test.shared;

import io.dropwizard.Application;
import org.eclipse.collections.api.block.function.Function0;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import java.util.Objects;

public class PureWithEngineHelper
{
    public static boolean initClientVersionIfNotAlreadySet(String defaultClientVersion)
    {
        boolean isNotSet = System.getProperty("alloy.test.clientVersion") == null && System.getProperty("legend.test.clientVersion") == null;
        if (isNotSet)
        {
            System.setProperty("alloy.test.clientVersion", defaultClientVersion);
            System.setProperty("legend.test.clientVersion", defaultClientVersion);
            System.setProperty("alloy.test.serverVersion", "v1");
            System.setProperty("legend.test.serverVersion", "v1");
        }
        return isNotSet;
    }

    public static void cleanUp()
    {
        System.clearProperty("alloy.test.clientVersion");
        System.clearProperty("legend.test.clientVersion");
    }

    public static <T extends Application> T initEngineServer(String serverConfigFilePath, Function0<T> engineServerCreator) throws Exception
    {
        int engineServerPort = DynamicPortGenerator.generatePort();

        System.setProperty("dw.server.connector.port", String.valueOf(engineServerPort));
        System.out.println("Found Config file: " + Objects.requireNonNull(PureWithEngineHelper.class.getClassLoader().getResource(serverConfigFilePath)).getFile());
        T server = engineServerCreator.get();
        server.run("server", Objects.requireNonNull(PureWithEngineHelper.class.getClassLoader().getResource(serverConfigFilePath)).getFile());
        System.out.println("Alloy server started on port:" + engineServerPort);

        System.setProperty("alloy.test.server.host", "127.0.0.1");
        System.setProperty("alloy.test.server.port", String.valueOf(engineServerPort));
        System.setProperty("legend.test.server.host", "127.0.0.1");
        System.setProperty("legend.test.server.port", String.valueOf(engineServerPort));
        System.out.println("Pure client configured to reach engine server");

        return server;
    }
}
