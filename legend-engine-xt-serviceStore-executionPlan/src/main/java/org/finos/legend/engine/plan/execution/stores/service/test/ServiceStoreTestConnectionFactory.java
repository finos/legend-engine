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

package org.finos.legend.engine.plan.execution.stores.service.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ServiceStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, EmbeddedData data)
    {
        if (sourceConnection instanceof ServiceStoreConnection && data instanceof ServiceStoreEmbeddedData)
        {
            String localHostUrl = "http://127.0.0.1";
            int port = DynamicPortGenerator.generatePort();

            ServiceStoreConnection testConnection = new ServiceStoreConnection();
            testConnection.element = sourceConnection.element;
            testConnection.baseUrl = localHostUrl + ":" + port;

            WireMockServer testServer = new TestServerSetupHelper((ServiceStoreEmbeddedData) data, port).setupTestServerWithData();

            Closeable closeable = new Closeable()
            {
                @Override
                public void close() throws IOException
                {
                    testServer.stop();
                }
            };

            return Optional.of(Tuples.pair(testConnection, Collections.singletonList(closeable)));
        }
        return Optional.empty();
    }
}
