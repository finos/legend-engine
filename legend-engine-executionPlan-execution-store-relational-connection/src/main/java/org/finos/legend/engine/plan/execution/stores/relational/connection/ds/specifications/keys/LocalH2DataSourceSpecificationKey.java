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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

public class LocalH2DataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final List<String> testDataSetupSqls;
    private final long currentTime;
    private final String uuidString;
    private final String id;

    public LocalH2DataSourceSpecificationKey(List<String> testDataSetupSqls)
    {
        this.currentTime = System.currentTimeMillis();
        this.uuidString = UUID.randomUUID().toString();
        this.testDataSetupSqls = testDataSetupSqls;
        this.id = String.format("LocalH2:%d:%s", this.currentTime, this.uuidString);
    }

    public List<String> getTestDataSetupSqls()
    {
        return this.testDataSetupSqls;
    }

    @Override
    public String shortId()
    {
        /*
            //return "LocalH2_testDataSetupSqls:" + ListIterate.collect(testDataSetupSqls, v -> v.substring(0, Math.min(v.length(), 30))+ System.identityHashCode(v)).makeString("");

            Note : We intentionally changed how we identify H2 data sources.
            Previously : The data source id was a best effort attempt to produce a globally unique id. Not only did this not guarantee uniqueness, it was also very long and spamming the logs.
            Now : We simply use the current time plus a random UUID.
         */
        return this.id;
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
        return Objects.equals(testDataSetupSqls, that.testDataSetupSqls) && id.equals(that.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(testDataSetupSqls, id);
    }
}
