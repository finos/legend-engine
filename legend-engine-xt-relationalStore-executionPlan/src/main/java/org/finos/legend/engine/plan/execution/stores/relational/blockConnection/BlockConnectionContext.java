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

package org.finos.legend.engine.plan.execution.stores.relational.blockConnection;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.pac4j.core.profile.CommonProfile;

import java.util.concurrent.CompletableFuture;

public class BlockConnectionContext
{
    private final MutableMap<ConnectionKey, BlockConnection> blockConnectionMap;

    private BlockConnectionContext(MutableMap<ConnectionKey, BlockConnection> blockConnectionMap)
    {
        this.blockConnectionMap = blockConnectionMap;
    }

    public BlockConnectionContext()
    {
        this(Maps.mutable.empty());
    }

    public BlockConnection getBlockConnection(RelationalStoreExecutionState executionState, DatabaseConnection databaseConnection, MutableList<CommonProfile> profiles)
    {
        BlockConnection requiredBlockConnection = this.blockConnectionMap.get(executionState.getRelationalExecutor().getConnectionManager().generateKeyFromDatabaseConnection(databaseConnection));
        if (requiredBlockConnection == null)
        {
            requiredBlockConnection = setBlockConnection(executionState.getRelationalExecutor().getConnectionManager(),
                    databaseConnection,
                    new BlockConnection(executionState.getRelationalExecutor().getConnectionManager().getDatabaseConnection(profiles, databaseConnection)));
        }
        if (!requiredBlockConnection.blockConnectionState.isConnectionAvailable())
        {
            Assert.fail(() -> "Multiple Connections required for " + databaseConnection.toString() + " connection( Not supported currently )");
        }

        requiredBlockConnection.blockConnectionState.hasOpenResultSet();
        return requiredBlockConnection;
    }

    public void unlockAllBlockConnections()
    {
        this.blockConnectionMap.values().forEach(conn -> conn.blockConnectionState.unlockConnection());
    }

    public void closeAllBlockConnections()
    {
        this.blockConnectionMap.values().forEach(BlockConnection::close);
    }

    public void closeAllBlockConnectionsAsync()
    {
        this.blockConnectionMap.values().forEach(blockConnection -> CompletableFuture.runAsync(blockConnection::close));
    }

    private BlockConnection setBlockConnection(ConnectionManagerSelector connectionManager, DatabaseConnection databaseConnection, BlockConnection blockConnection)
    {
        ConnectionKey key = connectionManager.generateKeyFromDatabaseConnection(databaseConnection);
        this.blockConnectionMap.put(key, blockConnection);
        return blockConnection;
    }

    public BlockConnectionContext copy()
    {
        return new BlockConnectionContext(Maps.mutable.ofMap(this.blockConnectionMap));
    }
}
