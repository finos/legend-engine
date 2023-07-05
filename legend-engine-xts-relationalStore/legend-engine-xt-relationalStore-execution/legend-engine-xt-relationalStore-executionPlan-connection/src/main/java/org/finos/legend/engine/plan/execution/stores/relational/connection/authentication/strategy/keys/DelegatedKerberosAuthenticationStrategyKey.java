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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DelegatedKerberosAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String serverPrincipal;

    public DelegatedKerberosAuthenticationStrategyKey(String serverPrincipal)
    {
        this.serverPrincipal = serverPrincipal;
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

        DelegatedKerberosAuthenticationStrategyKey that = (DelegatedKerberosAuthenticationStrategyKey) o;

        return new EqualsBuilder()
                .append(serverPrincipal, that.serverPrincipal)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(serverPrincipal)
                .toHashCode();
    }

    @Override
    public String shortId()
    {
        return "type:" + type() + "_principal:" + (serverPrincipal == null ? "none" : serverPrincipal);
    }

    @Override
    public String type()
    {
        return "DelegatedKerberos";
    }

    public String getServerPrincipal()
    {
        return serverPrincipal;
    }
}
