// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.memsql.MemSQLManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
import org.junit.Assert;
import org.junit.Test;

public class TestMemSQLMaxPoolSize
{
    @Test
    public void testMemSQLStaticMaxPoolSizeWithSystemProperty()
    {
        System.setProperty("legend.relational.maxPoolSize", "200");
        StaticDataSourceSpecification ds1 = new StaticDataSourceSpecification(
                new StaticDataSourceSpecificationKey("0.0.0.0", 1000, "MemSQL"),
                new MemSQLManager(),
                new TestDatabaseAuthenticationStrategy());
        Assert.assertEquals("200", String.valueOf(ds1.getHikariMaxPoolSize()));
    }
}
