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

public class MiddleTierKeytabAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String keytabVaultReference;
    private final String principal;
    private String keytabMetadataVaultReference;

    public MiddleTierKeytabAuthenticationStrategyKey(String principal, String keytabVaultReference, String keytabMetadataVaultReference)
    {
        this.principal = principal;
        this.keytabVaultReference = keytabVaultReference;
        this.keytabMetadataVaultReference = keytabMetadataVaultReference;
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

        MiddleTierKeytabAuthenticationStrategyKey that = (MiddleTierKeytabAuthenticationStrategyKey) o;

        return new EqualsBuilder()
                .append(principal, that.principal)
                .append(keytabVaultReference, that.keytabVaultReference)
                .append(keytabMetadataVaultReference, that.keytabMetadataVaultReference)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(principal)
                .append(keytabVaultReference)
                .append(keytabMetadataVaultReference)
                .toHashCode();
    }

    @Override
    public String shortId()
    {
        return "type:" + type()
                + "_principal:" + (principal == null ? "none" : principal)
                + "_keytabVaultReference:" + (keytabVaultReference == null ? "none" : keytabVaultReference)
                + "_keytabMetadataVaultReference:" + (keytabMetadataVaultReference == null ? "none" : keytabMetadataVaultReference);

    }

    @Override
    public String type()
    {
        return "MiddleTierKeytab";
    }

    public String getKeytabVaultReference()
    {
        return keytabVaultReference;
    }

    public String getPrincipal() { return principal; }

    public String getkeytabMetadataVaultReference() {
        return keytabMetadataVaultReference;
    }
}
