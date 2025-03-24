// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.test.shared;

import org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;

public class DeephavenCommands
{
    private static final int DEEPHAVEN_PORT = 10000;
    public static final String START_SERVER_FUNCTION = "startDeephaven_String_1__URL_1_";
    public static final String STOP_SERVER_FUNCTION = "stopDeephaven_String_1__Nil_0_";

    public static Root_meta_pure_functions_io_http_URL startServer(String imageTag)
    {
        if (!DeephavenTestContainer.startDeephaven(imageTag))
        {
            throw new RuntimeException("Failed to start Deephaven container");
        }

        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("deephavenUrl");
        String host = DeephavenTestContainer.deephavenContainer.getHost();
        int mappedPort = DeephavenTestContainer.deephavenContainer.getMappedPort(DEEPHAVEN_PORT);
        url._host(host);
        url._port(mappedPort);
        url._path("/");
        return url;
    }

    public static void stopServer(String imageTag)
    {
        DeephavenTestContainer.stopDeephaven();
    }
}
