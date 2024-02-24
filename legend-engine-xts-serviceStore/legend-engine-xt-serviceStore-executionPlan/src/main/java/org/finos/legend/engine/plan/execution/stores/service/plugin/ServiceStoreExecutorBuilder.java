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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;

public class ServiceStoreExecutorBuilder implements StoreExecutorBuilder
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Service");
    }

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Service;
    }

    @Override
    public ServiceStoreExecutor build()
    {
        return (ServiceStoreExecutor) build(ServiceStoreExecutionConfiguration.builder().build());
    }

    @Override
    public StoreExecutor build(StoreExecutorConfiguration storeExecutorConfiguration)
    {
        if (!(storeExecutorConfiguration instanceof ServiceStoreExecutionConfiguration))
        {
            throw new IllegalStateException("Incorrect store execution configuration. Please reach out to dev team");
        }
        ServiceStoreExecutionConfiguration serviceStoreExecutionConfiguration = (ServiceStoreExecutionConfiguration) storeExecutorConfiguration;
        ServiceStoreState state = new ServiceStoreState();
        return new ServiceStoreExecutor(state,serviceStoreExecutionConfiguration);
    }
}
