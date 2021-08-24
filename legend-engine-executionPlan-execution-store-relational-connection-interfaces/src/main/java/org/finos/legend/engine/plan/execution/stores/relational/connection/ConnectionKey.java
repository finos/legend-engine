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

package org.finos.legend.engine.plan.execution.stores.relational.connection;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class ConnectionKey
{
    private DataSourceSpecificationKey dataSourceSpecificationKey;
    private AuthenticationStrategyKey authenticationStrategyKey;

    public ConnectionKey(DataSourceSpecificationKey dataSourceSpecificationKey, AuthenticationStrategyKey authenticationStrategyKey)
    {
        this.dataSourceSpecificationKey = dataSourceSpecificationKey;
        this.authenticationStrategyKey = authenticationStrategyKey;
    }

    public DataSourceSpecificationKey getDataSourceSpecificationKey() {
        return this.dataSourceSpecificationKey;
    }

    public AuthenticationStrategyKey getAuthenticationStrategyKey() {
        return this.authenticationStrategyKey;
    }


    public String shortId() {
        return this.dataSourceSpecificationKey+ "_auth_" + this.authenticationStrategyKey;
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
        ConnectionKey that = (ConnectionKey) o;
        return Objects.equals(dataSourceSpecificationKey, that.dataSourceSpecificationKey) &&
                Objects.equals(authenticationStrategyKey, that.authenticationStrategyKey);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(dataSourceSpecificationKey, authenticationStrategyKey);
    }
}
