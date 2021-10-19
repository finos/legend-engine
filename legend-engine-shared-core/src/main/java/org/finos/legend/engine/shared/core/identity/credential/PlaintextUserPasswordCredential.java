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

package org.finos.legend.engine.shared.core.identity.credential;

import java.util.Objects;

import org.finos.legend.engine.shared.core.identity.Credential;

public class PlaintextUserPasswordCredential implements Credential
{
    private final String user;
    private final String password;

    public PlaintextUserPasswordCredential(String user, String password)
    {
        this.user = user;
        this.password = password;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
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
        PlaintextUserPasswordCredential that = (PlaintextUserPasswordCredential) o;
        return user.equals(that.user) &&
                password.equals(that.password);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(user, password);
    }
}