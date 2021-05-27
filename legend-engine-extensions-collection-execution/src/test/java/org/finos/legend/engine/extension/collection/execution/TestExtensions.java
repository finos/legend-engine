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

package org.finos.legend.engine.extension.collection.execution;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemoryStoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.ServiceLoader;

public class TestExtensions
{
    @Test
    public void testExecutionExtensions()
    {
        MutableList<Class<? extends ExecutionExtension>> expectedExtensions = Lists.mutable.<Class<? extends ExecutionExtension>>empty()
                .with(RelationalExecutionExtension.class);
        assertHasExtensions(expectedExtensions, ExecutionExtension.class);
    }

    @Test
    public void testStoreExecutorBuilderExtensions()
    {
        MutableList<Class<? extends StoreExecutorBuilder>> expectedExtensions = Lists.mutable.<Class<? extends StoreExecutorBuilder>>empty()
                .with(InMemoryStoreExecutorBuilder.class)
                .with(RelationalStoreExecutorBuilder.class);
        assertHasExtensions(expectedExtensions, StoreExecutorBuilder.class);
    }

    private <T> void assertHasExtensions(Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass)
    {
        assertHasExtensions(expectedExtensionClasses, extensionClass, true);
    }

    private <T> void assertHasExtensions(Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass, boolean failOnAdditional)
    {
        MutableSet<Class<? extends T>> missingClasses = Sets.mutable.withAll(expectedExtensionClasses);
        MutableList<Class<?>> unexpectedClasses = Lists.mutable.empty();
        ServiceLoader.load(extensionClass).forEach(e ->
        {
            if (!missingClasses.remove(e.getClass()))
            {
                unexpectedClasses.add(e.getClass());
            }
        });
        Assert.assertEquals("Missing extensions for " + extensionClass.getName(), Collections.emptySet(), missingClasses);
        if (failOnAdditional)
        {
            Assert.assertEquals("Unexpected extensions for " + extensionClass.getName(), Collections.emptyList(), unexpectedClasses);
        }
    }
}
