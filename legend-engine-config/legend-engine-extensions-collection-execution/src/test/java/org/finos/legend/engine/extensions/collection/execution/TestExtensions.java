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

package org.finos.legend.engine.extensions.collection.execution;

import java.util.Collections;
import java.util.ServiceLoader;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.external.format.flatdata.FlatDataRuntimeExtension;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.json.JsonSchemaRuntimeExtension;
import org.finos.legend.engine.external.format.xml.XsdRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatExecutionExtension;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.language.pure.dsl.service.execution.AbstractServicePlanExecutor;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemoryStoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.service.ServiceStoreExecutionExtension;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStoreExecutorBuilder;
import org.junit.Assert;
import org.junit.Test;

public class TestExtensions
{
    @Test
    public void testExecutionExtensions()
    {
        assertHasExtensions(expectedExecutionExtensions(), ExecutionExtension.class);
    }

    protected MutableList<Class<? extends ExecutionExtension>> expectedExecutionExtensions()
    {
        return Lists.mutable.<Class<? extends ExecutionExtension>>empty()
                .with(RelationalExecutionExtension.class)
                .with(ExternalFormatExecutionExtension.class)
                .with(ServiceStoreExecutionExtension.class)
                .with(org.finos.legend.engine.plan.execution.stores.mongodb.MongoDBStoreExecutionExtension.class)
                .with(org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.Elasticsearch7ExecutionExtension.class);
    }

    @Test
    public void testExternalFormatRuntimeExtensions()
    {
        assertHasExtensions(expectedExternalFormatRuntimeExtensions(), ExternalFormatRuntimeExtension.class);
    }

    protected MutableList<Class<? extends ExternalFormatRuntimeExtension>> expectedExternalFormatRuntimeExtensions()
    {
        return Lists.mutable.<Class<? extends ExternalFormatRuntimeExtension>>empty()
                .with(FlatDataRuntimeExtension.class)
                .with(JsonSchemaRuntimeExtension.class)
                .with(XsdRuntimeExtension.class);
    }

    @Test
    public void testFlatDataDriverDescription()
    {
        assertHasExtensions(getExpectedFlatDataDriverDescriptionExtensions(), FlatDataDriverDescription.class);
    }

    protected Iterable<? extends Class<? extends FlatDataDriverDescription>> getExpectedFlatDataDriverDescriptionExtensions()
    {
        // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
        return Lists.mutable.<Class<? extends FlatDataDriverDescription>>empty()
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.DelimitedWithHeadingsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.DelimitedWithoutHeadingsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.FixedWidthDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.core.ImmaterialLinesDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergActionsDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergDataDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergExtendActionDriverDescription.class)
                .with(org.finos.legend.engine.external.format.flatdata.driver.bloomberg.BloombergMetadataDriverDescription.class);
    }

    @Test
    public void testStoreExecutorBuilderExtensions()
    {
        assertHasExtensions(expectedStoreExecutorBuilderExtensions(), StoreExecutorBuilder.class);
    }

    protected MutableList<Class<? extends StoreExecutorBuilder>> expectedStoreExecutorBuilderExtensions()
    {
        return Lists.mutable.<Class<? extends StoreExecutorBuilder>>empty()
                .with(InMemoryStoreExecutorBuilder.class)
                .with(RelationalStoreExecutorBuilder.class)
                .with(ServiceStoreExecutorBuilder.class)
                .with(org.finos.legend.engine.plan.execution.stores.mongodb.plugin.MongoDBStoreExecutorBuilder.class)
                .with(org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.plugin.ElasticsearchV7StoreExecutorBuilder.class);
    }

    @Test
    public void testAbstractServicePlanExecutor()
    {
        Assert.assertNotNull(AbstractServicePlanExecutor.class);
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
