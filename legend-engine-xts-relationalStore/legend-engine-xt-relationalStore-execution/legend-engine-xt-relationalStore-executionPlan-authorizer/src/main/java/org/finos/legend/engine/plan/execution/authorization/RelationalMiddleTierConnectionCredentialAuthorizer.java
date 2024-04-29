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

package org.finos.legend.engine.plan.execution.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.shared.core.identity.Identity;

public interface RelationalMiddleTierConnectionCredentialAuthorizer
{
    /*
        Is 'Alice' allowed to use 'credref1' for interactive development ?
        Is 'Alice' allowed to use 'credref1' during a service execution ?
     */
    CredentialAuthorization evaluate(Identity currentUser, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception;

    @JsonPropertyOrder({"status","summary","subject","vaultReference","details"})
    class CredentialAuthorization
    {
        public enum Status
        {
            ALLOW,
            DENY
        }

        private Status status;

        private String subject;

        private String vaultReference;

        // details are expected to be JSON serializable
        private ImmutableList<Object> details;

        public CredentialAuthorization(String subject, String vaultReference, Status status, ImmutableList<Object> details)
        {
            this.subject = subject;
            this.vaultReference = vaultReference;
            this.status = status;
            this.details = details;
        }

        public static CredentialAuthorization allow(String subject, String vaultReference, ImmutableList<Object> details)
        {
            return new CredentialAuthorization(subject, vaultReference, Status.ALLOW, details);
        }

        public static CredentialAuthorization deny(String subject, String vaultReference, ImmutableList<Object> details)
        {
            return new CredentialAuthorization(subject, vaultReference, Status.DENY, details);
        }

        public Status getStatus()
        {
            return status;
        }

        @JsonIgnore
        public boolean isAllowed()
        {
            return this.status.equals(Status.ALLOW);
        }

        @JsonIgnore
        public boolean isDenied()
        {
            return !this.isAllowed();
        }

        public ImmutableList<Object> getDetails()
        {
            return details;
        }

        public String getSubject()
        {
            return subject;
        }

        public String getVaultReference()
        {
            return vaultReference;
        }

        public Object toJSON() throws Exception
        {
            return new ObjectMapper().writeValueAsString(this);
        }

        public Object toPrettyJSON() throws Exception
        {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        }
    }
}
