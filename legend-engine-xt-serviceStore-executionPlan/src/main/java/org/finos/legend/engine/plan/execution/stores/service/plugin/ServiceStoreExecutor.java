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

import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;

public class ServiceStoreExecutor implements StoreExecutor
{
    private final ServiceStoreState state;
    private final ServiceStoreExecutionConfiguration serviceStoreExecutionConfiguration;

    public ServiceStoreExecutor(ServiceStoreState state,ServiceStoreExecutionConfiguration serviceStoreExecutionConfiguration)
    {
        this.state = state;
        this.serviceStoreExecutionConfiguration = serviceStoreExecutionConfiguration;
    }

    @Override
    public StoreExecutionState buildStoreExecutionState()
    {
        return new ServiceStoreExecutionState(this.state, this.serviceStoreExecutionConfiguration.getCredentialProviderProvider());
    }

    @Override
    public ServiceStoreState getStoreState()
    {
        return this.state;
    }
}
