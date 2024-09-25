// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.testsupport;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.data.MongoDBStoreEmbeddedData;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBDatasourceSpecification;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBURL;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;

import java.io.Closeable;
import java.util.*;

public class MongoDBStoreTestConnectionFactory implements ConnectionFactoryExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, List<EmbeddedData> data)
    {
        List<MongoDBStoreEmbeddedData> mongoDBStoreEmbeddedDataList = ListIterate.selectInstancesOf(data, MongoDBStoreEmbeddedData.class);
        if (sourceConnection instanceof MongoDBConnection && data.size() == mongoDBStoreEmbeddedDataList.size() && !data.isEmpty())
        {

            MongoDBConnection testConnection = new MongoDBConnection();
            testConnection.element = sourceConnection.element;

            InMemoryMongoDBSetupHelper inMemoryServer = new InMemoryMongoDBSetupHelper();

            MongoDBDatasourceSpecification testDataSourceSpec = ((MongoDBConnection) sourceConnection).dataSourceSpecification;

            MongoDBURL serverUrl = new MongoDBURL();
            serverUrl.baseUrl = inMemoryServer.baseUrl;
            serverUrl.port = (long)inMemoryServer.port;
            testDataSourceSpec.serverURLs = Lists.mutable.of(serverUrl);
            testConnection.dataSourceSpecification = testDataSourceSpec;

            MongoDBStoreEmbeddedData mongoDBStoreEmbeddedData = new MongoDBStoreEmbeddedData();
            mongoDBStoreEmbeddedData.testData = ListIterate.flatCollect(mongoDBStoreEmbeddedDataList, a -> a.testData);
            mongoDBStoreEmbeddedData.databaseName = mongoDBStoreEmbeddedDataList.isEmpty() ? "" : mongoDBStoreEmbeddedDataList.get(0).databaseName;
            inMemoryServer.setupData(mongoDBStoreEmbeddedData);
            Closeable closeable = new Closeable()
            {
                @Override
                public void close()
                {
                    inMemoryServer.cleanUp();
                }
            };

            return Optional.of(Tuples.pair(testConnection, Collections.singletonList(closeable)));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Map<String, DataElement> dataElements, Store testStore, EmbeddedData data)
    {
        if (testStore instanceof MongoDatabase)
        {
            MongoDBConnection testConnection = new MongoDBConnection();
            testConnection.element = testStore.getPath();
            return this.tryBuildTestConnection(testConnection, Lists.mutable.of(data));
        }
        return Optional.empty();
    }
}
