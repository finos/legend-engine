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
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;

@JsonPropertyOrder({"authorizer", "authorized", "summary", "authorizationInput", "authorizations"})
public class PlanExecutionAuthorizerOutput
{
    private PlanExecutionAuthorizerInput authorizationInput;
    private String authorizer;
    private String summary;

    private ExecutionPlan transformedPlan;

    private ImmutableList<ExecutionAuthorization> authorizations;

    private boolean authorized;

    public PlanExecutionAuthorizerOutput()
    {
        // jackson
    }

    public PlanExecutionAuthorizerOutput(String authorizer, String summary, PlanExecutionAuthorizerInput authorizationInput, ExecutionPlan transformedPlan, ImmutableList<ExecutionAuthorization> authorizations)
    {
        this.authorizationInput = authorizationInput;
        this.authorizer = authorizer;
        this.summary = summary;
        this.transformedPlan = transformedPlan;
        this.authorizations = authorizations;
        this.authorized = authorizations.select(authorization -> authorization.isDenied()).isEmpty();
    }

    public String getAuthorizer()
    {
        return authorizer;
    }

    public String getSummary()
    {
        return summary;
    }

    @JsonIgnore
    public ExecutionPlan getTransformedPlan()
    {
        return transformedPlan;
    }

    public ImmutableList<ExecutionAuthorization> getAuthorizations()
    {
        return authorizations;
    }

    public boolean isAuthorized()
    {
        return authorized;
    }

    public PlanExecutionAuthorizerInput getAuthorizationInput()
    {
        return authorizationInput;
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
            return
                    ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(this);
        }
        catch (IOException e)
        {
            throw new RuntimeException("JSON serialization exception", e);
        }
    }
}
