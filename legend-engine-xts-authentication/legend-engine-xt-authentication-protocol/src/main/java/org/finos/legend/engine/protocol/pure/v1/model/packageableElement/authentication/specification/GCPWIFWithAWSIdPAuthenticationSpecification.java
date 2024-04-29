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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;

import java.util.ArrayList;
import java.util.List;

public class GCPWIFWithAWSIdPAuthenticationSpecification extends AuthenticationSpecification
{
    public IdPConfiguration idPConfiguration;

    public WorkloadConfiguration workloadConfiguration;

    public String serviceAccountEmail;
    public List<String> additionalGcpScopes = new ArrayList<>();

    @Override
    public <T> T accept(AuthenticationSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public GCPWIFWithAWSIdPAuthenticationSpecification(IdPConfiguration idPConfiguration, WorkloadConfiguration workloadConfiguration)
    {
        this.idPConfiguration = idPConfiguration;
        this.workloadConfiguration = workloadConfiguration;
    }

    public GCPWIFWithAWSIdPAuthenticationSpecification()
    {
        // Jackson
    }

    public static class IdPConfiguration
    {
        public String region;
        public String accountId;
        public String role;
        public AWSCredentials awsCredentials;

        public IdPConfiguration(String region, String accountId, String role, AWSCredentials awsCredentials)
        {
            this.region = region;
            this.accountId = accountId;
            this.role = role;
            this.awsCredentials = awsCredentials;
        }

        public IdPConfiguration()
        {
            // jackson
        }
    }

    public static class WorkloadConfiguration
    {
        public String projectNumber;
        public String poolId;
        public String providerId;

        public WorkloadConfiguration(String projectNumber, String poolId, String providerId)
        {
            this.projectNumber = projectNumber;
            this.poolId = poolId;
            this.providerId = providerId;
        }

        public WorkloadConfiguration()
        {
            // jackson
        }
    }
}
