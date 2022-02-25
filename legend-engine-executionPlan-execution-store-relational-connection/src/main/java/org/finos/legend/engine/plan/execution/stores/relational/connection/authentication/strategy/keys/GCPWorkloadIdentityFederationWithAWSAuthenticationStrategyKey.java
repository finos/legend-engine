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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;
import java.util.Objects;

public class GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey implements AuthenticationStrategyKey{
    public static final String TYPE = "GCPWorkloadIdentityFederationWithAWS";

    private String workloadProjectNumber;
    private String serviceAccountEmail;
    private List<String> additionalGcpScopes;
    private String workloadPoolId;
    private String workloadProviderId;
    private String awsAccountId;
    private String awsRegion;
    private String awsRole;
    private String awsAccessKeyIdVaultReference;
    private String awsSecretAccessKeyVaultReference;

    public GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey(String workloadProjectNumber, String serviceAccountEmail, List<String> additionalGcpScopes, String workloadPoolId, String workloadProviderId, String awsAccountId, String awsRegion, String awsRole, String awsAccessKeyIdVaultReference, String awsSecretAccessKeyVaultReference) {
        this.workloadProjectNumber = workloadProjectNumber;
        this.serviceAccountEmail = serviceAccountEmail;
        this.additionalGcpScopes = additionalGcpScopes;
        this.workloadPoolId = workloadPoolId;
        this.workloadProviderId = workloadProviderId;
        this.awsAccountId = awsAccountId;
        this.awsRegion = awsRegion;
        this.awsRole = awsRole;
        this.awsAccessKeyIdVaultReference = awsAccessKeyIdVaultReference;
        this.awsSecretAccessKeyVaultReference = awsSecretAccessKeyVaultReference;
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
        GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey that = (GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey) o;
        return Objects.equals(workloadProjectNumber, that.workloadProjectNumber) &&
                Objects.equals(serviceAccountEmail, that.serviceAccountEmail) &&
                Objects.equals(additionalGcpScopes, that.additionalGcpScopes) &&
                Objects.equals(workloadPoolId, that.workloadPoolId) &&
                Objects.equals(workloadProviderId, that.workloadProviderId) &&
                Objects.equals(awsAccountId, that.awsAccountId) &&
                Objects.equals(awsRegion, that.awsRegion) &&
                Objects.equals(awsRole, that.awsRole);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(workloadProjectNumber, serviceAccountEmail, additionalGcpScopes, workloadPoolId, workloadProviderId, awsAccountId, awsRegion, awsRole);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_wpNo:" + workloadProjectNumber +
                "_saE:" + serviceAccountEmail +
                "_gcpAS:" + (additionalGcpScopes != null ? String.join(",",additionalGcpScopes) : "null") +
                "_wPoId:" + workloadPoolId +
                "_wPrId:"+ workloadProviderId +
                "_awsAc:" + awsAccountId +
                "_awsRe:" + awsRegion +
                "_awsRo:" + awsRole +
                "_awsAKRef:" + awsAccessKeyIdVaultReference +
                "_awsSKRef:" + awsSecretAccessKeyVaultReference;
    }

    @Override
    public String type()
    {
        return TYPE;
    }
}
