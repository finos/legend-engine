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

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.test.databricks.DatabricksCleanupResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops tables this suite left behind on earlier runs.
 *
 * <p>meta::relational::tests::pct::process::setupDatabase registers each table's drop as an
 * on-connection-close commit query, but those do not run when the block connection is locked, when
 * an earlier drop in the batch throws, or when the JVM dies - and the schema itself is deliberately
 * never dropped. The residue had reached 2617 tables at a steady ~165 a month since 2025-05 before
 * anything started removing it.
 *
 * <p>Selection is by age, not by run. A suite takes about half an hour, so a table older than
 * {@link #MIN_AGE} cannot belong to a run still in flight - not this one, and not a concurrent CI
 * run against the same workspace. That is what makes it safe to do from inside a test rather than
 * from a job that owns the workspace.
 */
class DatabricksLeakedTableSweep extends DatabricksCleanupResource
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabricksLeakedTableSweep.class);

    private static final String PCT_SCHEMA = "leschema";

    private static final Duration MIN_AGE = Duration.ofDays(2);

    // Bounds what any one run adds to start-up. The leak is a few tables a day and the suite runs
    // many times a day, so this drains a backlog without ever costing much on a single run.
    private static final int SWEEP_LIMIT = 100;

    // PCT names its tables <prefix><random>_<epochMillis>.
    private static final int EPOCH_MILLIS_LENGTH = 13;

    DatabricksLeakedTableSweep(TestConnectionIntegration integration)
    {
        super(integration);
    }

    @Override
    protected void clean(Statement statement) throws SQLException
    {
        long cutoff = System.currentTimeMillis() - MIN_AGE.toMillis();
        MutableList<String> leaked = query(statement, "Show tables in " + PCT_SCHEMA, 2)
                .select(table -> createdBefore(table, cutoff))
                .take(SWEEP_LIMIT);

        int swept = 0;
        for (String table : leaked)
        {
            try
            {
                statement.execute("Drop table if exists " + PCT_SCHEMA + ".`" + table + "`");
                swept++;
            }
            catch (Exception e)
            {
                LOGGER.warn("Could not sweep leaked table [{}.{}]", PCT_SCHEMA, table, e);
            }
        }
        LOGGER.info("Swept {} leaked table(s) from [{}]", swept, PCT_SCHEMA);
    }

    /**
     * A name we cannot parse is left alone rather than guessed at.
     */
    private static boolean createdBefore(String table, long cutoff)
    {
        String suffix = table.substring(table.lastIndexOf('_') + 1);
        if (suffix.length() != EPOCH_MILLIS_LENGTH || !suffix.chars().allMatch(Character::isDigit))
        {
            return false;
        }
        return Long.parseLong(suffix) < cutoff;
    }
}
