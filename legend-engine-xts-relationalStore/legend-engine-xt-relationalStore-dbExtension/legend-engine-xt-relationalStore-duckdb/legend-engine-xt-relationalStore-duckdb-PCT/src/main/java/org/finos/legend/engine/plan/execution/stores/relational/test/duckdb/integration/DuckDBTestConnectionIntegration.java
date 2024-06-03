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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

public class DuckDBTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "DuckDB");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.DuckDB;
    }

    @Override
    public void setup() throws Exception
    {
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        DuckDBDatasourceSpecification duckDBDataSourceSpecification = new DuckDBDatasourceSpecification();
        duckDBDataSourceSpecification.path = "";

        TestDatabaseAuthenticationStrategy authSpec = new TestDatabaseAuthenticationStrategy();

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(duckDBDataSourceSpecification, authSpec, DatabaseType.DuckDB);

        conn.type = DatabaseType.DuckDB;           // for compatibility with legacy DatabaseConnection
        conn.element = null;

        return conn;
    }

    @Override
    public void cleanup() throws Exception
    {
    }


    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }
}
