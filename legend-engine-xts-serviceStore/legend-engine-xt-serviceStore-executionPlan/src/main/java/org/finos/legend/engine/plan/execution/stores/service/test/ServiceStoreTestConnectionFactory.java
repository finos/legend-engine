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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStore;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServiceStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Service");
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, List<EmbeddedData> data)
    {
        List<ServiceStoreEmbeddedData> serviceStoreDataList = ListIterate.selectInstancesOf(data, ServiceStoreEmbeddedData.class);
        if (sourceConnection instanceof ServiceStoreConnection && serviceStoreDataList.size() == data.size() && !data.isEmpty())
        {
            String localHostUrl = "http://127.0.0.1";
            int port = DynamicPortGenerator.generatePort();

            ServiceStoreConnection testConnection = new ServiceStoreConnection();
            testConnection.element = sourceConnection.element;
            testConnection.baseUrl = localHostUrl + ":" + port;
            ServiceStoreEmbeddedData serviceStoreEmbeddedData = new ServiceStoreEmbeddedData();
            serviceStoreEmbeddedData.serviceStubMappings = ListIterate.flatCollect(serviceStoreDataList, a -> a.serviceStubMappings);
            WireMockServer testServer = new TestServerSetupHelper(serviceStoreEmbeddedData, port).setupTestServerWithData();

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

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Map<String, DataElement> dataElements, Store testStore, EmbeddedData data)
    {
        if (testStore instanceof ServiceStore)
        {
            ServiceStoreConnection testConnection = new ServiceStoreConnection();
            testConnection.element = testStore.getPath();
            return this.tryBuildTestConnection(testConnection, Lists.mutable.of(data));
        }
        return Optional.empty();
    }
}
