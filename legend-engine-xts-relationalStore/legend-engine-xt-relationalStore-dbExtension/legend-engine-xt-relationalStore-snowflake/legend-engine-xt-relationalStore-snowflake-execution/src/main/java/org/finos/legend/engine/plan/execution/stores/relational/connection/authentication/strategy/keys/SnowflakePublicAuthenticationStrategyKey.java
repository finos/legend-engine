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

public class SnowflakePublicAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String privateKeyVaultReference;
    private final String passPhraseVaultReference;
    private final String publicUserName;

    public SnowflakePublicAuthenticationStrategyKey(String privateKeyVaultReference, String passPhraseVaultReference, String publicUserName)
    {
        this.privateKeyVaultReference = privateKeyVaultReference;
        this.passPhraseVaultReference = passPhraseVaultReference;
        this.publicUserName = publicUserName;
    }

    public String getPrivateKeyVaultReference()
    {
        return this.privateKeyVaultReference;
    }

    public String getPassPhraseVaultReference()
    {
        return this.passPhraseVaultReference;
    }

    public String getPublicUserName()
    {
        return this.publicUserName;
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
        SnowflakePublicAuthenticationStrategyKey that = (SnowflakePublicAuthenticationStrategyKey) o;
        return Objects.equals(privateKeyVaultReference, that.privateKeyVaultReference) &&
                Objects.equals(passPhraseVaultReference, that.passPhraseVaultReference) &&
                Objects.equals(publicUserName, that.publicUserName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(privateKeyVaultReference, passPhraseVaultReference, publicUserName);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_pk:" + privateKeyVaultReference +
                "_pp:" + passPhraseVaultReference +
                "_username:" + publicUserName;
    }

    @Override
    public String type()
    {
        return "SnowflakePublic";
    }
}
