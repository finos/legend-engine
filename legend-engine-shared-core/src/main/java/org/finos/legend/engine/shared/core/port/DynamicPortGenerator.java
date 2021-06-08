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

package org.finos.legend.engine.shared.core.port;

import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicPortGenerator
{
    public static final int MIN_TEST_PORT = 49152;
    public static final int MAX_TEST_PORT = 65535;

    private static final int NUMBER_OF_TRIES = 10;
    private static final AtomicInteger testPort = new AtomicInteger(MIN_TEST_PORT);

    public static int generatePort() {
        for (int i = 0; i < NUMBER_OF_TRIES; i++) {
            int port = testPort.updateAndGet(current -> current == MAX_TEST_PORT ? MIN_TEST_PORT : current + 1);

            if (isAvailable(port)) {
                return port;
            }
        }

        throw new EngineException("Unable to obtain dynamic port");
    }

    private static boolean isAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
