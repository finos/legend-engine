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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys;

import java.util.List;
import java.util.Objects;

public class GCPWorkloadIdentityFederationAuthenticationStrategyKey implements AuthenticationStrategyKey{
    public static final String TYPE = "GCPWorkloadIdentityFederation";

    private String serviceAccountEmail;
    private List<String> additionalGcpScopes;

    public GCPWorkloadIdentityFederationAuthenticationStrategyKey(String serviceAccountEmail, List<String> additionalGcpScopes) {
        this.serviceAccountEmail = serviceAccountEmail;
        this.additionalGcpScopes = additionalGcpScopes;
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
        GCPWorkloadIdentityFederationAuthenticationStrategyKey that = (GCPWorkloadIdentityFederationAuthenticationStrategyKey) o;
        return  Objects.equals(serviceAccountEmail, that.serviceAccountEmail) &&
                Objects.equals(additionalGcpScopes, that.additionalGcpScopes);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(serviceAccountEmail, additionalGcpScopes);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_serviceAccountEmail:" + serviceAccountEmail +
                "_gcpAdditionalScopes:" + (additionalGcpScopes != null ? String.join(",",additionalGcpScopes) : "null");
    }

    @Override
    public String type()
    {
        return TYPE;
    }
}
