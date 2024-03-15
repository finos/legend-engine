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

    // Usage policy context is a metadata attribute that points to a policy definition stored elsewhere. The policy by itself is not a secret and therefore not stored as part of the credential.
    private String usagePolicyContext;

    public MiddleTierUserPasswordCredential()
    {
        // jackson
    }

    public MiddleTierUserPasswordCredential(String user, String password, String usagePolicyContext)
    {
        this.user = user;
        this.password = password;
        this.usagePolicyContext = usagePolicyContext;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public String getUsagePolicyContext()
    {
        return usagePolicyContext;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setUsagePolicyContext(String usagePolicyContext)
    {
        this.usagePolicyContext = usagePolicyContext;
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
                password.equals(that.password) && usagePolicyContext.equals(that.usagePolicyContext);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(user, password, usagePolicyContext);
    }
}