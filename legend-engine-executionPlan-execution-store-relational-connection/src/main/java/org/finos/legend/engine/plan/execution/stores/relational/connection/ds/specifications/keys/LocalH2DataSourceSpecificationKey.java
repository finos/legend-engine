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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class LocalH2DataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String databaseName;
    private final int port;

    public LocalH2DataSourceSpecificationKey(int port, String databaseName)
    {
        this.port = port;
        this.databaseName = databaseName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public String shortId()
    {
        return "LocalH2_" +
                "port:" + port + "_" +
                "db:" + databaseName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        LocalH2DataSourceSpecificationKey that = (LocalH2DataSourceSpecificationKey) o;
        return port == that.port &&
                Objects.equals(databaseName, that.databaseName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(databaseName, port);
    }
}
