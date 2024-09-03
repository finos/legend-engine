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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.duckdb;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

import java.util.Properties;

public class DuckDBDriver extends DriverWrapper
{
    public static String DRIVER_CLASSNAME = "org.duckdb.DuckDBDriver";

    @Override
    protected String getClassName()
    {
        return DRIVER_CLASSNAME;
    }

    @Override
    protected Properties handlePropertiesPriorToJDBCDriverConnection(Properties properties)
    {
        Properties duckDBProperties = new Properties();
        duckDBProperties.putAll(properties);
        duckDBProperties.remove("prepStmtCacheSize");
        duckDBProperties.remove("useServerPrepStmts");
        duckDBProperties.remove("cachePrepStmts");
        duckDBProperties.remove("prepStmtCacheSqlLimit");
        duckDBProperties.remove("POOL_NAME_KEY");
        duckDBProperties.remove("AUTHENTICATION_STRATEGY_KEY");
        duckDBProperties.remove("legend_duckdb_path");
        duckDBProperties.remove("connectionInitSql");
        return duckDBProperties;
    }
}
