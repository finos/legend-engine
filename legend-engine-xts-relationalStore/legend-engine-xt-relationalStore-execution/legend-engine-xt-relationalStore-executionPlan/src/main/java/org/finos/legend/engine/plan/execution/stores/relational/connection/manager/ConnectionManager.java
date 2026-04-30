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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager;

import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.util.Map;

public interface ConnectionManager
{
    DataSourceSpecification getDataSourceSpecification(DatabaseConnection databaseConnection);

    ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection databaseConnection);

    Connection getTestDatabaseConnection();

    /**
     * Preprocesses a DatabaseConnection before it enters key generation and DataSourceSpecification
     * construction. Extensions may enrich the connection using the executing user's identity
     * and/or allocation variables from the execution state.
     * <p>
     * The default implementation returns the connection unchanged.
     *
     * @param connection       the database connection to preprocess
     * @param identity         the identity of the executing user
     * @param allocationResults  allocation variables (name &rarr; Result) from the current execution state
     * @return the (possibly enriched) DatabaseConnection
     */
    default DatabaseConnection preprocessConnection(DatabaseConnection connection, Identity identity, Map<String, Result> allocationResults)
    {
        return connection;
    }
}
