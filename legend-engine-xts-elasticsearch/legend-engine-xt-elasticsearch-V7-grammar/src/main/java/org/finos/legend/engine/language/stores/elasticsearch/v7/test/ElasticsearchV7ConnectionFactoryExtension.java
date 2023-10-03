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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.test;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7EmbeddedData;

public class ElasticsearchV7ConnectionFactoryExtension implements ConnectionFactoryExtension
{

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Store store, Connection sourceConnection, EmbeddedData data)
    {
        return Optional.of(sourceConnection)
                .filter(Elasticsearch7StoreConnection.class::isInstance)
                .flatMap(x -> this.tryBuildTestConnectionsForStore(null, store, data));
    }

    @Override
    public Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Map<String, DataElement> dataElements, Store store, EmbeddedData data)
    {
        return Optional.of(store)
                .filter(Elasticsearch7Store.class::isInstance)
                .filter(x -> data instanceof ElasticsearchV7EmbeddedData)
                .map(x -> ElasticsearchV7ContainerManager.INSTANCE.setupServer((Elasticsearch7Store) store, (ElasticsearchV7EmbeddedData) data));
    }
}
