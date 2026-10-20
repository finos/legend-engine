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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.pct;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

/**
 * Creates the schema fixture tables go in, since the adapter's DDL translator emits no CREATE SCHEMA.
 * Unlike the cleanup resources, a failure here stops the suite: no test can run without the schema.
 */
class DatabricksFixtureSchema implements TestServerResource
{
    private static final String PCT_SCHEMA = "leschema";

    private final TestConnectionIntegration integration;

    DatabricksFixtureSchema(TestConnectionIntegration integration)
    {
        this.integration = integration;
    }

    @Override
    public void start() throws SQLException
    {
        ConnectionManagerSelector connectionManager = new RelationalStoreExecutorBuilder().build()
                .getStoreState().getRelationalExecutor().getConnectionManager();

        try (Connection connection = connectionManager.getDatabaseConnection(new Identity("legend-pct-setup"), this.integration.getConnection());
             Statement statement = connection.createStatement())
        {
            statement.execute("Create Schema if not exists " + PCT_SCHEMA);
        }
    }

    @Override
    public void shutDown()
    {
    }
}
