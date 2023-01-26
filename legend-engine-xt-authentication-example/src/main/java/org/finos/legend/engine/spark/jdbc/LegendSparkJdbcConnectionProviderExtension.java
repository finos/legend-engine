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
import scala.collection.immutable.Map;

import java.sql.Connection;
import java.sql.Driver;

public class LegendSparkJdbcConnectionProviderExtension extends JdbcConnectionProvider
{
    private static LegendSparkJdbcConnectionProvider DELEGATE = null;

    public static synchronized void setDelegate(LegendSparkJdbcConnectionProvider delegate)
    {
        DELEGATE = delegate;
    }

    public static synchronized LegendSparkJdbcConnectionProvider getDelegate()
    {
        return DELEGATE;
    }

    @Override
    public String name()
    {
        return "legend-spark-jdbc";
    }

    @Override
    public boolean canHandle(Driver driver, Map<String, String> options)
    {
        return getDelegate().canHandle(driver, options);
    }

    @Override
    public Connection getConnection(Driver driver, Map<String, String> options)
    {
        return getDelegate().getConnection(driver, options);
    }

    @Override
    public boolean modifiesSecurityContext(Driver driver, Map<String, String> options)
    {
        return getDelegate().modifiesSecurityContext(driver, options);

    }
}
