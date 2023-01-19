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

import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProviderBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.service.ServiceExecutor;

public class ServiceStoreState implements StoreState
{
    private final ServiceExecutor serviceExecutor;

    public ServiceStoreState()
    {
        this.serviceExecutor = new ServiceExecutor(CredentialProviderProviderBuilder.build());
    }

    public ServiceStoreState(CredentialProviderProvider credentialProviderProvider)
    {
        this.serviceExecutor = new ServiceExecutor(credentialProviderProvider);
    }

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Service;
    }

    @Override
    public Object getStoreExecutionInfo()
    {
        return null;
    }

    public ServiceExecutor getServiceExecutor()
    {
        return serviceExecutor;
    }
}
