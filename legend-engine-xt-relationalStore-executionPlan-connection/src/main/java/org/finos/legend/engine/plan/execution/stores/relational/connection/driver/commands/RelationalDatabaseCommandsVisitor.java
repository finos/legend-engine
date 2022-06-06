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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery.BigQueryCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Commands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.sqlserver.SqlServerCommands;

public interface RelationalDatabaseCommandsVisitor<T>
{
    T visit(SnowflakeCommands snowflakeCommands);

    T visit(DatabricksCommands databricksCommands);

    T visit(H2Commands h2Commands);

    T visit(SqlServerCommands sqlServerCommands);

    T visit(BigQueryCommands bigQueryCommands);

    T visit(RedshiftCommands redshiftCommands);

}
