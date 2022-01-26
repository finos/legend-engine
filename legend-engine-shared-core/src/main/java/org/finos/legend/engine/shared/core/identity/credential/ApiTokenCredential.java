// Copyright 2022 Databricks
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

import org.finos.legend.engine.shared.core.identity.Credential;

import java.util.Objects;

public class ApiTokenCredential implements Credential
{
    private final String apiToken;

    public ApiTokenCredential(String apiToken)
    {
        this.apiToken = apiToken;
    }

    public String getApiToken()
    {
        return apiToken;
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
        ApiTokenCredential that = (ApiTokenCredential) o;
        return apiToken.equals(that.apiToken);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(apiToken);
    }
}