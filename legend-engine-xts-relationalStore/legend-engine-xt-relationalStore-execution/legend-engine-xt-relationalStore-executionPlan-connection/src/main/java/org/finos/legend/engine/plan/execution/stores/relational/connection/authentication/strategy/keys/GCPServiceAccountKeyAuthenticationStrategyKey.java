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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys;

import java.util.Objects;

public class GCPServiceAccountKeyAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    public static final String TYPE = "GCPServiceAccountKey";

    private String serviceAccountKeyVaultReference;

    public GCPServiceAccountKeyAuthenticationStrategyKey(String serviceAccountKeyVaultReference)
    {
        this.serviceAccountKeyVaultReference = serviceAccountKeyVaultReference;
    }

    public String getServiceAccountKeyVaultReference()
    {
        return serviceAccountKeyVaultReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        GCPServiceAccountKeyAuthenticationStrategyKey that = (GCPServiceAccountKeyAuthenticationStrategyKey) o;
        return Objects.equals(serviceAccountKeyVaultReference, that.serviceAccountKeyVaultReference);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(serviceAccountKeyVaultReference);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_serviceAccountKeyVaultReference:" + serviceAccountKeyVaultReference;
    }

    @Override
    public String type()
    {
        return TYPE;
    }
}
