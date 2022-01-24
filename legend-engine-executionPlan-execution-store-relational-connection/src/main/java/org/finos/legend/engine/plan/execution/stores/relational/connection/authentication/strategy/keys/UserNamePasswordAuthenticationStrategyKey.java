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

import java.util.Objects;

public class UserNamePasswordAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String userNameVaultReference;
    private final String passwordVaultReference;

    public UserNamePasswordAuthenticationStrategyKey(String userNameVaultReference, String passwordVaultReference)
    {
        this.userNameVaultReference = userNameVaultReference;
        this.passwordVaultReference = passwordVaultReference;
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

        UserNamePasswordAuthenticationStrategyKey that = (UserNamePasswordAuthenticationStrategyKey) o;
        return Objects.equals(userNameVaultReference, that.userNameVaultReference) &&
                Objects.equals(passwordVaultReference, that.passwordVaultReference);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(userNameVaultReference, passwordVaultReference);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_username:" + userNameVaultReference +
                "_password:" + passwordVaultReference;
    }

    @Override
    public String type()
    {
        return "UserNamePassword";
    }
}
