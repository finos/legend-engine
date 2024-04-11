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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;

/*
    A root plan execution authorizer composes many authorizers and uses them to authorize a plan execution.
 */
public class RootMiddleTierPlanExecutionAuthorizer implements PlanExecutionAuthorizer
{
    private ImmutableList<PlanExecutionAuthorizer> authorizerImplementations;

    public RootMiddleTierPlanExecutionAuthorizer(ImmutableList<PlanExecutionAuthorizer> authorizerImplementations)
    {
        if (authorizerImplementations == null || authorizerImplementations.isEmpty())
        {
            throw new IllegalArgumentException("Plan execution authorizer has to be instantiated with one authorizer implementation");
        }
        if (authorizerImplementations.size() > 1)
        {
            throw new IllegalArgumentException(String.format("Plan execution authorizer cannot be instantiated with %d (more than one) authorizer implementation. This is reserved for future use.", authorizerImplementations.size()));
        }
        this.authorizerImplementations = authorizerImplementations;
    }

    @Override
    public PlanExecutionAuthorizerOutput evaluate(Identity user, ExecutionPlan executionPlan, PlanExecutionAuthorizerInput authorizationInput) throws Exception
    {
        MutableList<PlanExecutionAuthorizerOutput> authorizationResults = Lists.mutable.empty();

        for (PlanExecutionAuthorizer authorizerImplementation : this.authorizerImplementations)
        {
            PlanExecutionAuthorizerOutput authorizationResult = authorizerImplementation.evaluate(user, executionPlan, authorizationInput);
            authorizationResults.add(authorizationResult);
        }
        return this.merge(authorizationResults);
    }

    private PlanExecutionAuthorizerOutput merge(MutableList<PlanExecutionAuthorizerOutput> authorizationResults)
    {
        /*
            When we support multiple authorizer implementations, we have to implement a more sophisticated merge; especially because each authorizer can produce its own transformed plan.
            For now, it is safe to return just the first element as we restrict the number of authorizer implementations to one.
         */
        return authorizationResults.get(0);
    }

    public ImmutableList<PlanExecutionAuthorizer> getAuthorizerImplementations()
    {
        return authorizerImplementations;
    }

    @Override
    public boolean isMiddleTierPlan(ExecutionPlan executionPlan)
    {
        /*
            When we support multiple authorizer implementations, we have to check with each authorizer.
            For now, it is safe to check with just the first element as we restrict the number of authorizer implementations to one.
         */

        return authorizerImplementations.get(0).isMiddleTierPlan(executionPlan);
    }
}
