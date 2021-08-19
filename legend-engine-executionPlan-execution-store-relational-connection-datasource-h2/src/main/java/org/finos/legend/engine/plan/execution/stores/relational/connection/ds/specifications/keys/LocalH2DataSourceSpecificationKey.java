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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.List;
import java.util.Objects;

public class LocalH2DataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final List<String> testDataSetupSqls;

    public LocalH2DataSourceSpecificationKey(List<String> testDataSetupSqls)
    {
        this.testDataSetupSqls = testDataSetupSqls;
    }

    public List<String> getTestDataSetupSqls()
    {
        return this.testDataSetupSqls;
    }

    @Override
    public String shortId()
    {
        return "LocalH2_testDataSetupSqls:" + ListIterate.collect(testDataSetupSqls, v -> v.substring(0, Math.min(v.length(), 30))+ System.identityHashCode(v)).makeString("");
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
        return  Objects.equals(testDataSetupSqls, that.testDataSetupSqls);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(testDataSetupSqls);
    }
}
