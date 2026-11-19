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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.sqlOptimizer;

import java.sql.SQLException;
import java.sql.Statement;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.test.databricks.DatabricksCleanupResource;

/**
 * Drops the SQL optimizer suite's fixture schema before the suite runs.
 *
 * <p>The suite's tables have fixed names in a single shared workspace, so overlapping CI runs can leave a table whose Delta
 * log cannot be reconstructed (see DatabricksFixtureSchemaReset). Dropping the schema is the only repair over JDBC. The
 * fixture setup recreates the schema before each table, and the semistructured suite's reset spares it, since that suite
 * runs in the other fork.
 */
class DatabricksSqlOptimizerSchemaReset extends DatabricksCleanupResource
{
    private static final String SCHEMA = "SQLOPT";

    DatabricksSqlOptimizerSchemaReset(TestConnectionIntegration integration)
    {
        super(integration);
    }

    @Override
    protected void clean(Statement statement) throws SQLException
    {
        statement.execute("Drop schema if exists " + SCHEMA + " cascade");
    }
}
