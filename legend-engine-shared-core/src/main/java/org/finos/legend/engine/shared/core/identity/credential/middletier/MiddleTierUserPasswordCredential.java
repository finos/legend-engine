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

package org.finos.legend.engine.shared.core.identity.credential.middletier;

import org.finos.legend.engine.shared.core.identity.Credential;

import java.util.Objects;

public class MiddleTierUserPasswordCredential implements Credential
{
    private String user;
    private String password;
    private String[] usageContexts;

    public MiddleTierUserPasswordCredential()
    {
        // jackson
    }

    public MiddleTierUserPasswordCredential(String user, String password, String[] usageContexts)
    {
        this.user = user;
        this.password = password;
        this.usageContexts = usageContexts;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public String[] getUsageContexts()
    {
        return usageContexts;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setUsageContexts(String[] usageContexts)
    {
        this.usageContexts = usageContexts;
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
        MiddleTierUserPasswordCredential that = (MiddleTierUserPasswordCredential) o;
        return user.equals(that.user) &&
                password.equals(that.password) && usageContexts.equals(that.usageContexts);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(user, password, usageContexts);
    }
}