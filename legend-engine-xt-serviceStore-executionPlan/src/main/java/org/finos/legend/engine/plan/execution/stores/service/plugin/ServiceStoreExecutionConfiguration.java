// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.service.ServiceExecutor;

public class ServiceStoreExecutionConfiguration implements StoreExecutorConfiguration
{
    private CredentialProviderProvider credentialProviderProvider;

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Service;
    }

    public CredentialProviderProvider getCredentialProviderProvider()
    {
        return credentialProviderProvider;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder().build();

        public Builder withCredentialProviderProvider(CredentialProviderProvider credentialProviderProvider)
        {
            this.credentialProviderProvider = credentialProviderProvider;
            return this;
        }

        public ServiceStoreExecutionConfiguration build()
        {
            ServiceStoreExecutionConfiguration serviceStoreExecutionConfiguration = new ServiceStoreExecutionConfiguration();
            serviceStoreExecutionConfiguration.credentialProviderProvider = credentialProviderProvider;
            return serviceStoreExecutionConfiguration;
        }
    }
}