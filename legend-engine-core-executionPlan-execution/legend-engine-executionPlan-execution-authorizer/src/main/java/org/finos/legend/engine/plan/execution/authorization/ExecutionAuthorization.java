// Copyright 2020 Goldman Sachs
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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;

@JsonPropertyOrder({"status","summary","subject", "action", "resource", "policyParams", "details"})
public class ExecutionAuthorization
{
    public enum Status
    {
        ALLOW,
        DENY
    }

    private Status status;
    private String subject;
    private String action;
    private String summary;

    // objects are expected to be JSON serializable
    private ImmutableList<Object> details;

    private ImmutableMap<String, String> resource = Maps.immutable.empty();

    private ImmutableMap<String, String> policyParams = Maps.immutable.empty();

    public static ExecutionAuthorization authorize(String subject, String action, ImmutableMap<String, String> resource, ImmutableMap<String, String> policyParams, String summary, ImmutableList<Object> details)
    {
        return new ExecutionAuthorization(subject, action, resource, policyParams, Status.ALLOW, summary, details);
    }

    public static ExecutionAuthorization deny(String subject, String action, ImmutableMap<String, String> resource, ImmutableMap<String, String> policyParams, String summary, ImmutableList<Object> details)
    {
        return new ExecutionAuthorization(subject, action, resource, policyParams, Status.DENY, summary, details);
    }

    public ExecutionAuthorization()
    {
        // jackson
    }

    public ExecutionAuthorization(String subject, String action, ImmutableMap<String, String> resource, ImmutableMap<String, String> policyParams, Status status, String summary, ImmutableList<Object> details)
    {
        this.subject = subject;
        this.action = action;
        this.resource = resource;
        this.policyParams = policyParams;
        this.status = status;
        this.summary = summary;
        this.details = details;
    }

    public String getSummary()
    {
        return summary;
    }

    public ImmutableList<Object> getDetails()
    {
        return details;
    }

    public Status getStatus()
    {
        return status;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getAction()
    {
        return action;
    }

    public ImmutableMap<String, String> getResource()
    {
        return resource;
    }

    public ImmutableMap<String, String> getPolicyParams()
    {
        return policyParams;
    }

    @JsonIgnore
    public boolean isAllowed()
    {
        return Status.ALLOW.equals(this.status);
    }

    @JsonIgnore
    public boolean isDenied()
    {
        return !this.isAllowed();
    }

    public String toJSON()
    {
        try
        {
            return ObjectMapperFactory
                    .getNewStandardObjectMapperWithPureProtocolExtensionSupports()
                    .writeValueAsString(this);
        }
        catch (IOException e)
        {
            throw new RuntimeException("JSON serialization exception", e);
        }
    }

    public String toPrettyJSON()
    {
        try
        {
            return ObjectMapperFactory
                    .getNewStandardObjectMapperWithPureProtocolExtensionSupports()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this);
        }
        catch (IOException e)
        {
            throw new RuntimeException("JSON serialization exception", e);
        }
    }

    public static Builder authorize(String subject)
    {
       return new Builder(Status.ALLOW, subject);
    }

    public static Builder deny(String subject)
    {
        return new Builder(Status.DENY, subject);
    }

    public static Builder withSubject(String subject)
    {
        return new Builder(subject);
    }

    public static class Builder
    {
        private String subject;
        private Status status;
        private String action;
        private String summary;

        // objects are expected to be JSON serializable
        private ImmutableList<Object> details;

        private ImmutableMap<String, String> resource = Maps.immutable.empty();

        private ImmutableMap<String, String> policyParams = Maps.immutable.empty();

        public Builder(Status status, String subject)
        {
            this.status = status;
            this.subject = subject;
        }

        public Builder(String subject)
        {
            this.subject = subject;
        }

        public Builder withStatus(Status status)
        {
            this.status = status;
            return this;
        }

        public Builder withResource(ImmutableMap<String, String> resource)
        {
            this.resource = resource;
            return this;
        }

        public Builder withAction(String action)
        {
            this.action = action;
            return this;
        }

        public Builder withPolicyParams(ImmutableMap<String, String> policyParams)
        {
            this.policyParams = policyParams;
            return this;
        }

        public Builder withDetails(ImmutableList<Object> details)
        {
            this.details = details;
            return this;
        }

        public Builder withSummary(String summary)
        {
            this.summary = summary;
            return this;
        }

        public ExecutionAuthorization build()
        {
            return new ExecutionAuthorization(subject, action, resource, policyParams, status, summary, details);
        }
    }
}
