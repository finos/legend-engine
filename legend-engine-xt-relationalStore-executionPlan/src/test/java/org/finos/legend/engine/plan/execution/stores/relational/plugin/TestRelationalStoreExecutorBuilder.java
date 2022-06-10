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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class TestRelationalStoreExecutorBuilder
{
    @Test
    public void testDefaultBuilder()
    {
        RelationalStoreExecutor executor = new RelationalStoreExecutorBuilder().build();
        RelationalStoreState state = executor.getStoreState();
        RelationalExecutorInfo info = state.getStoreExecutionInfo();
        Assert.assertNotNull(info);
    }

    @Test
    public void testGetStoreType()
    {
        Assert.assertSame(StoreType.Relational, new RelationalStoreExecutorBuilder().getStoreType());
    }

    @Test
    public void testServiceLoader()
    {
        List<StoreExecutorBuilder> builders = new ArrayList<>();
        ServiceLoader.load(StoreExecutorBuilder.class).forEach(builders::add);
        assertStoreExecutorBuilders(builders);
    }

    @Test
    public void testPlanExecutorLoader()
    {
        assertStoreExecutorBuilders(PlanExecutor.loadStoreExecutorBuilders());
    }

    private void assertStoreExecutorBuilders(List<StoreExecutorBuilder> builders)
    {
        Assert.assertEquals(1, builders.size());
        StoreExecutorBuilder builder = builders.get(0);
        Assert.assertTrue(builder instanceof RelationalStoreExecutorBuilder);
        RelationalStoreExecutor executor = ((RelationalStoreExecutorBuilder) builder).build();
        Assert.assertNotNull(executor);
    }
}
