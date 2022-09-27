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

package org.finos.legend.engine.plan.execution.stores.service.config;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;

import java.util.List;

public class ServiceStoreExecutionConfiguration implements StoreExecutorConfiguration
{
    // Feature flag that indicates if the JVM's ssl key and trust store should be used when making ServiceStore connections
    public boolean propagateJVMSSLContext = false;

    /*
        List of services URIs that use mTLS.
        Note : The elements in this list are prefixes and a URI must fully match a prefix (including the leading https://)
     */
    public List<String> mtlsServiceUriPrefixes = FastList.newList();

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Service;
    }


    public ServiceStoreExecutionConfiguration()
    {
    }

    public boolean isPropagateJVMSSLContext()
    {
        return propagateJVMSSLContext;
    }

    public void setPropagateJVMSSLContext(boolean propagateJVMSSLContext)
    {
        this.propagateJVMSSLContext = propagateJVMSSLContext;
    }

    public List<String> getMtlsServiceUriPrefixes()
    {
        return mtlsServiceUriPrefixes;
    }

    public void setMtlsServiceUriPrefixes(List<String> mtlsServiceUriPrefixes)
    {
        this.mtlsServiceUriPrefixes = mtlsServiceUriPrefixes;
    }
}
