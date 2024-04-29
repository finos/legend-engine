// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.plugin;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class TestMongoDBStoreExecutorBuilder
{
    @Test
    public void testDefaultBuilder()
    {
        MongoDBStoreExecutor executor = new MongoDBStoreExecutorBuilder().build();
        MongoDBStoreState state = executor.getStoreState();
        Assert.assertNull(state.getStoreExecutionInfo());
    }

    @Test
    public void testServiceLoader()
    {
        List<StoreExecutorBuilder> builders = new ArrayList<>();
        ServiceLoader.load(StoreExecutorBuilder.class).forEach(builders::add);
        // In Memory builder as well.
        Assert.assertEquals(2, builders.size());
        StoreExecutorBuilder builder = builders.get(0);
        Assert.assertTrue(builder instanceof MongoDBStoreExecutorBuilder);
        MongoDBStoreExecutor executor = (MongoDBStoreExecutor) builder.build();
        Assert.assertNotNull(executor);
    }

    @Test
    public void testPlanExecutorLoader()
    {
        List<StoreExecutorBuilder> builders = PlanExecutor.loadStoreExecutorBuilders();
        Assert.assertEquals(2, builders.size());
        StoreExecutorBuilder builder = builders.get(0);
        Assert.assertTrue(builder instanceof MongoDBStoreExecutorBuilder);
        MongoDBStoreExecutor executor = ((MongoDBStoreExecutorBuilder) builder).build();
        Assert.assertNotNull(executor);
    }
}
