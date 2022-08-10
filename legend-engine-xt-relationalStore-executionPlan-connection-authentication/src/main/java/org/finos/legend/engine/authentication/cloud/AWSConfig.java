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

package org.finos.legend.engine.authentication.cloud;

public class AWSConfig
{
    public String getRegion()
    {
        return region;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public String getRole()
    {
        return role;
    }

    public String getAwsAccessKeyIdVaultReference()
    {
        return awsAccessKeyIdVaultReference;
    }

    public String getAwsSecretAccessKeyVaultReference()
    {
        return awsSecretAccessKeyVaultReference;
    }

    public AWSConfig(String region, String accountId, String role, String awsAccessKeyIdVaultReference, String awsSecretAccessKeyVaultReference)
    {
        this.region = region;
        this.accountId = accountId;
        this.role = role;
        this.awsAccessKeyIdVaultReference = awsAccessKeyIdVaultReference;
        this.awsSecretAccessKeyVaultReference = awsSecretAccessKeyVaultReference;
    }

    private String region;
    private String accountId;
    private String role;
    private String awsAccessKeyIdVaultReference;
    private String awsSecretAccessKeyVaultReference;

    public AWSConfig()
    {
        // jackson
    }
}
