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

import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.service.config.ServiceStoreExecutionConfiguration;

public class ServiceStoreExecutorBuilder implements StoreExecutorBuilder
{
    @Override
    public StoreType getStoreType()
    {
        return StoreType.Service;
    }

    @Override
    public ServiceStoreExecutor build()
    {
        return ServiceStoreExecutor.buildInstance();
    }

    @Override
    public StoreExecutor build(StoreExecutorConfiguration storeExecutorConfiguration)
    {
        return ServiceStoreExecutor.buildInstance((ServiceStoreExecutionConfiguration) storeExecutorConfiguration);
    }
}
