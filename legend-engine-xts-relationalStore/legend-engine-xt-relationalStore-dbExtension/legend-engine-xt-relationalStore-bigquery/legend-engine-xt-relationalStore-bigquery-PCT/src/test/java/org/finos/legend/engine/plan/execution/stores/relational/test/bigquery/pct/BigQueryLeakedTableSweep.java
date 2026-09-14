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

package org.finos.legend.engine.plan.execution.stores.relational.test.bigquery.pct;

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
 * PCT creates its fixture tables as plain tables, because BigQuery only accepts a temporary table
 * inside a script or session, so nothing reclaims them when a run ends and they accumulate in the
 * shared dataset. This drops the ones left behind by earlier runs.
 *
 * Age is read from BigQuery's own catalogue rather than inferred from the table name, and only
 * tables past the cutoff are touched, so a run happening concurrently keeps its fixtures.
 */
class BigQueryLeakedTableSweep implements TestServerResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryLeakedTableSweep.class);

    private static final String PCT_SCHEMA = "leSchema";
    private static final int MIN_AGE_DAYS = 2;
    private static final int SWEEP_LIMIT = 100;

    private final TestConnectionIntegration integration;

    BigQueryLeakedTableSweep(TestConnectionIntegration integration)
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
            LOGGER.warn("BigQuery leaked table sweep skipped", e);
        }
    }

    private void clean(Statement statement) throws SQLException
    {
        MutableList<String> leaked = Lists.mutable.empty();
        String sql = "Select table_name From " + PCT_SCHEMA + ".INFORMATION_SCHEMA.TABLES" +
                " Where creation_time < timestamp_sub(current_timestamp(), interval " + MIN_AGE_DAYS + " day)" +
                " Order by creation_time Limit " + SWEEP_LIMIT;
        try (ResultSet resultSet = statement.executeQuery(sql))
        {
            while (resultSet.next())
            {
                leaked.add(resultSet.getString(1));
            }
        }

        int swept = 0;
        for (String table : leaked)
        {
            try
            {
                statement.execute("Drop table if exists " + PCT_SCHEMA + ".`" + table + "`");
                swept++;
            }
            catch (SQLException e)
            {
                LOGGER.warn("Could not sweep leaked table [{}.{}]", PCT_SCHEMA, table, e);
            }
        }
        LOGGER.info("Swept {} leaked table(s) from [{}]", swept, PCT_SCHEMA);
    }

    @Override
    public void shutDown()
    {
    }
}
