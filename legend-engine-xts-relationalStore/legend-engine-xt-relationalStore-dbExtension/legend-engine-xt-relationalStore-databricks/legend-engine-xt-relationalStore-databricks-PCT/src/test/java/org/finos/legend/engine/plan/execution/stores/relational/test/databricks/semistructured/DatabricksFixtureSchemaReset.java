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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.semistructured;

import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.test.databricks.DatabricksCleanupResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops the fixture schemas of meta::relational::tests::semistructured before the suite runs.
 *
 * <p>Unlike the PCT suite, which randomises its table names, these tests use hardcoded schema/table
 * names against a single shared Databricks workspace. When two CI runs overlap, one run's
 * `Drop table if exists` can delete `_delta_log/0.json` after the other has written later versions.
 * That leaves a managed table directory whose Delta log cannot be reconstructed - and because the
 * metastore entry is gone with it, the table is invisible to `Show tables`, so no `Drop table` can
 * reach it and every later `Create Table` fails with DELTA_TRUNCATED_TRANSACTION_LOG. Dropping the
 * schema deletes its warehouse directory, orphan included, which is the only repair available over
 * JDBC.
 *
 * <p>The set is derived from `Show schemas` rather than listed, so a new fixture schema needs no
 * change here. See {@link #PROTECTED_SCHEMAS} for what is spared.
 */
class DatabricksFixtureSchemaReset extends DatabricksCleanupResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabricksFixtureSchemaReset.class);

    // Everything Show schemas returns is dropped except these.
    //
    //   default            - the database the JDBC URL connects to
    //   leschema           - the PCT suite's, and it sweeps its own leaked tables by age. Dropping
    //                        it here would race the tables that suite creates in a parallel fork.
    //   information_schema - metastore-owned
    //   inflows/lcr/legend - unknown provenance, no reference anywhere in this repo. Quarantined
    //                        until someone claims them; delete these three entries once they are
    //                        confirmed to be junk.
    private static final MutableList<String> PROTECTED_SCHEMAS = Lists.mutable.with(
            "default",
            "leschema",
            "information_schema",
            "inflows",
            "lcr",
            "legend"
    );

    DatabricksFixtureSchemaReset(TestConnectionIntegration integration)
    {
        super(integration);
    }

    @Override
    protected void clean(Statement statement) throws SQLException
    {
        for (String schema : query(statement, "Show schemas", 1))
        {
            if (PROTECTED_SCHEMAS.contains(schema.toLowerCase()))
            {
                continue;
            }
            try
            {
                statement.execute("Drop schema if exists " + schema + " cascade");
                LOGGER.info("Reset Databricks schema [{}]", schema);
            }
            catch (Exception e)
            {
                LOGGER.warn("Could not reset Databricks schema [{}]", schema, e);
            }
        }
    }
}
