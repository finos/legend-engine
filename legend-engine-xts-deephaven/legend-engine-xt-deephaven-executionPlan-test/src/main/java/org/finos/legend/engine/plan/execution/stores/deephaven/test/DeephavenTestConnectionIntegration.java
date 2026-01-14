// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.test;

import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.DeephavenCommands;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;

import java.net.URI;

public class DeephavenTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final String IMAGE_TAG = "0.37.4";
    private final DeephavenConnection connection = new DeephavenConnection();

    @Override
    public void setup() throws Exception
    {
        Root_meta_pure_functions_io_http_URL url = DeephavenCommands.startServerForPCT(IMAGE_TAG);
        URI uri = new URI("http", null, url._host(), (int) url._port(), url._path(), null, null);
        this.connection._sourceSpec(new org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification()._url(uri))
                ._authSpec(new PSKAuthenticationSpecification("myStaticPSK"));
    }

    @Override
    public DeephavenConnection getConnection() throws Exception
    {
        if (this.connection.sourceSpec == null)
        {
            this.setup();
        }
        return this.connection;
    }

    @Override
    public void cleanup() throws Exception
    {
        DeephavenCommands.stopServer(IMAGE_TAG);
    }

    @Override
    public void shutDown() throws Exception
    {
//        this.cleanup();
    }

    @Override
    public void start() throws Exception
    {
//        this.setup();
    }
}

