// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.graphQLStore.plugin;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class TestGraphQLStoreExecutorBuilder
{
    @Test
    public void testDefaultBuilder()
    {
        GraphQLStoreExecutor executor = new GraphQLStoreExecutorBuilder().build();
        GraphQLStoreState state = executor.getStoreState();
        Assert.assertNull(state.getStoreExecutionInfo());
    }

    @Test
    public void testGetStoreType()
    {
        Assert.assertSame(StoreType.GraphQL, new GraphQLStoreExecutorBuilder().getStoreType());
    }

    @Test
    public void testServiceLoader()
    {
        List<StoreExecutorBuilder> builders = new ArrayList<>();
        ServiceLoader.load(StoreExecutorBuilder.class).forEach(builders::add);
        Assert.assertTrue(builders.stream().anyMatch(b -> b instanceof GraphQLStoreExecutorBuilder));
        StoreExecutorBuilder builder = builders.stream().filter(b -> b instanceof GraphQLStoreExecutorBuilder).findFirst().orElseThrow(() -> new AssertionError("GraphQLStoreExecutorBuilder not found via ServiceLoader"));
        GraphQLStoreExecutor executor = ((GraphQLStoreExecutorBuilder) builder).build();
        Assert.assertNotNull(executor);
    }

    @Test
    public void testPlanExecutorLoader()
    {
        List<StoreExecutorBuilder> builders = PlanExecutor.loadStoreExecutorBuilders();
        Assert.assertTrue(builders.stream().anyMatch(b -> b instanceof GraphQLStoreExecutorBuilder));
    }
}
