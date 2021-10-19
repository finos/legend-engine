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

package org.finos.legend.engine.plan.execution.stores.service.plugin;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class TestServiceStoreExecutorBuilder
{
    @Test
    public void testDefaultBuilder()
    {
        ServiceStoreExecutor executor = new ServiceStoreExecutorBuilder().build();
        ServiceStoreState state = executor.getStoreState();
        Assert.assertNull(state.getStoreExecutionInfo());
    }

    @Test
    public void testGetStoreType()
    {
        Assert.assertSame(StoreType.Service, new ServiceStoreExecutorBuilder().getStoreType());
    }

    @Test
    public void testServiceLoader()
    {
        List<StoreExecutorBuilder> builders = new ArrayList<>();
        ServiceLoader.load(StoreExecutorBuilder.class).forEach(builders::add);
        Assert.assertEquals(2, builders.size());
        StoreExecutorBuilder builder = builders.get(0);
        Assert.assertTrue(builder instanceof ServiceStoreExecutorBuilder);
        ServiceStoreExecutor executor = ((ServiceStoreExecutorBuilder)builder).build();
        Assert.assertNotNull(executor);
    }

    @Test
    public void testPlanExecutorLoader()
    {
        List<StoreExecutorBuilder> builders = PlanExecutor.loadStoreExecutorBuilders();
        Assert.assertEquals(2, builders.size());
        StoreExecutorBuilder builder = builders.get(0);
        Assert.assertTrue(builder instanceof ServiceStoreExecutorBuilder);
        ServiceStoreExecutor executor = ((ServiceStoreExecutorBuilder)builder).build();
        Assert.assertNotNull(executor);
    }
}
