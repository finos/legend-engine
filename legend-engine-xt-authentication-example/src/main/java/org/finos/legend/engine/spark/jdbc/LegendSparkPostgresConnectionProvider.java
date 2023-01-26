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

package org.finos.legend.engine.spark.jdbc;

import org.apache.spark.sql.jdbc.JdbcConnectionProvider;
import org.finos.legend.engine.connection.jdbc.LegendPostgresConnectionProvider;
import scala.collection.immutable.Map;

import java.sql.Connection;
import java.sql.Driver;

public class LegendSparkPostgresConnectionProvider extends JdbcConnectionProvider
{
    private LegendPostgresConnectionProvider legendPostgresConnectionProvider;

    public LegendSparkPostgresConnectionProvider(LegendPostgresConnectionProvider legendPostgresConnectionProvider)
    {
        this.legendPostgresConnectionProvider = legendPostgresConnectionProvider;
    }

    @Override
    public String name()
    {
        return "legend-postgres";
    }

    @Override
    public boolean canHandle(Driver driver, Map<String, String> options)
    {
        return this.legendPostgresConnectionProvider.canHandle(driver, options);
    }

    @Override
    public Connection getConnection(Driver driver, Map<String, String> options)
    {
        try
        {
            return this.legendPostgresConnectionProvider.getConnection(driver, options);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean modifiesSecurityContext(Driver driver, Map<String, String> options)
    {
        return false;
    }
}
