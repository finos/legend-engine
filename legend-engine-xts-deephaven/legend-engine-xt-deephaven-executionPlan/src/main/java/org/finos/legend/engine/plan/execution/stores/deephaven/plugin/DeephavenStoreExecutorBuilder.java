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
//

package org.finos.legend.engine.plan.execution.stores.deephaven.plugin;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;

public class DeephavenStoreExecutorBuilder implements StoreExecutorBuilder
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Deephaven;
    }

    @Override
    public DeephavenStoreExecutor build()
    {
        return (DeephavenStoreExecutor) build(DeephavenStoreExecutorConfiguration.newInstance().build());
    }

    @Override
    public DeephavenStoreExecutor build(StoreExecutorConfiguration storeExecutorConfiguration)
    {
        if (!(storeExecutorConfiguration instanceof DeephavenStoreExecutorConfiguration))
        {
            throw new IllegalStateException("Incorrect store execution configuration, expected DeephavenStoreExecutorConfiguration. Please reach out to dev team");
        }
        return new DeephavenStoreExecutor(new DeephavenStoreState(), (DeephavenStoreExecutorConfiguration) storeExecutorConfiguration);
    }
}
