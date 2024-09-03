// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.authentication.strategy.key;

import java.util.Objects;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;

public class DuckDBS3AuthenticationStrategyKey implements AuthenticationStrategyKey
{
    public static final String TYPE = "S3";
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKeyVaultReference;
    private final String endpoint;

    public DuckDBS3AuthenticationStrategyKey(String region, String accessKeyId, String secretAccessKeyVaultReference, String endpoint)
    {
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretAccessKeyVaultReference = secretAccessKeyVaultReference;
        this.endpoint = endpoint;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof DuckDBS3AuthenticationStrategyKey))
        {
            return false;
        }
        DuckDBS3AuthenticationStrategyKey that = (DuckDBS3AuthenticationStrategyKey) o;
        return Objects.equals(region, that.region) && Objects.equals(accessKeyId, that.accessKeyId) && Objects.equals(secretAccessKeyVaultReference, that.secretAccessKeyVaultReference) && Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(region, accessKeyId, secretAccessKeyVaultReference, endpoint);
    }

    @Override
    public String toString()
    {
        return TYPE + " {" +
                "region='" + region + '\'' +
                ", accessKeyId='" + accessKeyId + '\'' +
                ", secretAccessKeyVaultReference='" + secretAccessKeyVaultReference + '\'' +
                ", endpoint='" + endpoint + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return this.toString();
    }

    @Override
    public String type()
    {
        return TYPE;
    }

    public String getRegion()
    {
        return region;
    }

    public String getAccessKeyId()
    {
        return accessKeyId;
    }

    public String getSecretAccessKeyVaultReference()
    {
        return secretAccessKeyVaultReference;
    }

    public String getEndpoint()
    {
        return endpoint;
    }
}
