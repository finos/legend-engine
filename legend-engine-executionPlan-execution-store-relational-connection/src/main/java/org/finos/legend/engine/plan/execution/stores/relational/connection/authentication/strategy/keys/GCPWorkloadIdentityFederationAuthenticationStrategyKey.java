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

public class GCPWorkloadIdentityFederationAuthenticationStrategyKey implements AuthenticationStrategyKey{
    public static final String TYPE = "GCPWorkloadIdentityFederation";

    private String workloadProjectNumber;
    private String serviceAccountEmail;
    private String gcpScope;
    private String workloadPoolId;
    private String workloadProviderId;
    private String discoveryUrl;
    private String clientId;

    public String getWorkloadProjectNumber() {
        return workloadProjectNumber;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public String getGcpScope() {
        return gcpScope;
    }

    public String getWorkloadPoolId() {
        return workloadPoolId;
    }

    public String getWorkloadProviderId() {
        return workloadProviderId;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public GCPWorkloadIdentityFederationAuthenticationStrategyKey(String workloadProjectNumber, String serviceAccountEmail, String gcpScope, String workloadPoolId, String workloadProviderId, String discoveryUrl, String clientId) {
        this.workloadProjectNumber = workloadProjectNumber;
        this.serviceAccountEmail = serviceAccountEmail;
        this.gcpScope = gcpScope;
        this.workloadPoolId = workloadPoolId;
        this.workloadProviderId = workloadProviderId;
        this.discoveryUrl = discoveryUrl;
        this.clientId = clientId;
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
        GCPWorkloadIdentityFederationAuthenticationStrategyKey that = (GCPWorkloadIdentityFederationAuthenticationStrategyKey) o;
        return Objects.equals(workloadProjectNumber, that.workloadProjectNumber) &&
                Objects.equals(serviceAccountEmail, that.serviceAccountEmail) &&
                Objects.equals(gcpScope, that.gcpScope) &&
                Objects.equals(workloadPoolId, that.workloadPoolId) &&
                Objects.equals(workloadProviderId, that.workloadProviderId) &&
                Objects.equals(discoveryUrl, that.discoveryUrl) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(workloadProjectNumber, serviceAccountEmail, gcpScope, workloadPoolId, workloadProviderId, discoveryUrl, clientId);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_wpNo:" + workloadProjectNumber +
                "_saE:" + serviceAccountEmail +
                "_gcpS:" + gcpScope +
                "_wPoId:" + workloadPoolId +
                "_wPrId:"+ workloadProviderId +
                "_dUrl:" + discoveryUrl +
                "_cId:" + clientId;
    }

    @Override
    public String type()
    {
        return TYPE;
    }
}
