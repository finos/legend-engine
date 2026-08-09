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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a suite's own tidy-up against the shared Databricks workspace before its tests start.
 *
 * <p>Add one after the connection integration in the {@code wrapSuite} resource list -
 * {@code ServersState.start()} runs resources in order, and this needs the connection the
 * integration builds in its own {@code start()}.
 *
 * <p>Each suite cleans up only what it creates. Nothing here may touch another suite's fixtures:
 * the pct-cloud-test profile sets {@code forkCount=2}, so suites run in parallel JVMs that cannot
 * coordinate with each other.
 */
public abstract class DatabricksCleanupResource implements TestServerResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabricksCleanupResource.class);

    private final TestConnectionIntegration integration;

    protected DatabricksCleanupResource(TestConnectionIntegration integration)
    {
        this.integration = integration;
    }

    @Override
    public void start()
    {
        try
        {
            ConnectionManagerSelector connectionManager = new RelationalStoreExecutorBuilder().build()
                    .getStoreState().getRelationalExecutor().getConnectionManager();

            try (Connection connection = connectionManager.getDatabaseConnection(new Identity("legend-pct-cleanup"), this.integration.getConnection());
                 Statement statement = connection.createStatement())
            {
                this.clean(statement);
            }
        }
        catch (Exception e)
        {
            // Never fatal - a tidy-up that throws would turn a recoverable workspace into a hard suite failure.
            LOGGER.warn("Databricks cleanup [{}] skipped", this.getClass().getSimpleName(), e);
        }
    }

    protected abstract void clean(Statement statement) throws SQLException;

    protected static MutableList<String> query(Statement statement, String sql, int column) throws SQLException
    {
        MutableList<String> values = Lists.mutable.empty();
        try (ResultSet resultSet = statement.executeQuery(sql))
        {
            while (resultSet.next())
            {
                values.add(resultSet.getString(column));
            }
        }
        return values;
    }

    @Override
    public void shutDown()
    {
    }
}
