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

package org.finos.legend.engine.protocol.pure.v1.extension;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.shared.core.extension.LegendConnectionExtension;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Used to build connections using test data. Main goal is to leverage connection for execution
 */
public interface ConnectionFactoryExtension extends LegendConnectionExtension
{
    @Override
    default String type()
    {
        return "Connection_Factory";
    }

    /**
     * Deprecated as part of deprecated of Legacy Mapping Test
     */
    @Deprecated
    default Optional<Connection> tryBuildFromInputData(InputData inputData)
    {
        return Optional.empty();
    }

    @Deprecated
    /**
     * Deprecated as part of deprecated of Legacy Service Test
     */
    default Optional<Connection> tryBuildFromConnection(Connection connection, String testData, String element)
    {
        return Optional.empty();
    }

    default Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnection(Connection sourceConnection, List<EmbeddedData> data)
    {
        return Optional.empty();
    }

    default Optional<Pair<Connection, List<Closeable>>> tryBuildTestConnectionsForStore(Map<String, DataElement> dataElements, Store store, EmbeddedData data)
    {
        return Optional.empty();
    }

    default Optional<Pair<Connection, List<Closeable>>> tryBuildConnectionForStoreData(Map<String, DataElement> dataElements, Map<Store, EmbeddedData> storeTestData)
    {
        if (storeTestData.size() == 1)
        {
            Store store = storeTestData.keySet().stream().findFirst().get();
            EmbeddedData embeddedData = storeTestData.values().stream().findFirst().get();
            return tryBuildTestConnectionsForStore(dataElements, store, embeddedData);
        }
        return Optional.empty();
    }

}
