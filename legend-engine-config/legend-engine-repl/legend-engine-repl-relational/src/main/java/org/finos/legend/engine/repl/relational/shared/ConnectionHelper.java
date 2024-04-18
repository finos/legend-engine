// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.relational.shared;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;

public class ConnectionHelper
{
    public static java.sql.Connection getConnection(DatabaseConnection connection, PlanExecutor planExecutor)
    {
        RelationalStoreExecutor r = (RelationalStoreExecutor) planExecutor.getExecutorsOfType(StoreType.Relational).getFirst();
        ConnectionManagerSelector connectionManager = r.getStoreState().getRelationalExecutor().getConnectionManager();
        return connection == null ? connectionManager.getTestDatabaseConnection() : connectionManager.getDatabaseConnection(new Identity("X"), connection);
    }

    public static DatabaseConnection getDatabaseConnection(PureModelContextData d, String path)
    {
        PackageableConnection packageableConnection = ListIterate.select(d.getElementsOfType(PackageableConnection.class), c -> c.getPath().equals(path)).getFirst();
        if (packageableConnection == null)
        {
            throw new RuntimeException("Error, the connection '" + path + "' can't be found!");
        }
        DatabaseConnection databaseConnection = (DatabaseConnection) packageableConnection.connectionValue;
        return databaseConnection;
    }
}
