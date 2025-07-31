// Copyright 2025 Goldman Sachs
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

public class OAuthAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String oauthKey;
    private final String scope;

    public OAuthAuthenticationStrategyKey(String oauthKey, String scope)
    {
        this.oauthKey = oauthKey;
        this.scope = scope;
    }

    public String getOauthKey()
    {
        return this.oauthKey;
    }

    public String getScope()
    {
        return this.scope;
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
        OAuthAuthenticationStrategyKey that = (OAuthAuthenticationStrategyKey) o;
        return Objects.equals(oauthKey, that.oauthKey) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(oauthKey, scope);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() + "_"
                + "key:" + oauthKey
                + "_scope:" + scope;
    }

    @Override
    public String type()
    {
        return "OAuth";
    }
}