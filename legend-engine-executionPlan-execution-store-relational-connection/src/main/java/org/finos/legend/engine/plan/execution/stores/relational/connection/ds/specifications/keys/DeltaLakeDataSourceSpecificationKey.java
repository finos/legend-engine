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

public class DeltaLakeDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String shard;
    private final String httpPath;
    private final String token;

    public DeltaLakeDataSourceSpecificationKey(String shard, String httpPath, String token)
    {
        this.shard = shard;
        this.httpPath = httpPath;
        this.token = token;
    }

    public String getShard()
    {
        return shard;
    }

    public String getHttpPath()
    {
        return httpPath;
    }

    public String getToken()
    {
        return token;
    }

    @Override
    public String toString()
    {
        return "DeltaLakeDataSourceSpecificationKey{" +
                "shard='" + shard + '\'' +
                "httpPath='" + httpPath + '\'' +
                "token='" + token + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "DeltaLake_" +
                "shard:" + shard + "_" +
                "httpPath:" + httpPath + "_" +
                "token:" + token;
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
        DeltaLakeDataSourceSpecificationKey that = (DeltaLakeDataSourceSpecificationKey) o;
        return Objects.equals(shard, that.shard) && Objects.equals(httpPath, that.httpPath) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(shard, httpPath, token);
    }
}
