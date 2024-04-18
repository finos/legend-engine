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

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;

/*
    A plan execution authorizer is used to determine whether a plan can be executed or not.

    Given an input (identity, plan, authorization context), an authorizer can produce one of the following outputs :

    1/ "Authorize the plan"

    The output contains one/more ExecutionAuthorization's each with a status of ALLOW. As an example, an interactive query might require authorizations to multiple connections/credentials.
    The output also echoes the input plan.

    2/ "Authorize and rewrite the plan"

    This is same as above except that output contains a plan that is different from the input.
    This is reserved for use cases where we might re-write the plan based on the authorization criteria.

    3/ "Deny"

    The output contains one/more ExecutionAuthorization's with a status of DENY. As an example, an interactive query might require authorizations to two databases and only one of which was successful.
    In this case the output plan is irrelevant.

 */
public interface PlanExecutionAuthorizer
{
    PlanExecutionAuthorizerOutput evaluate(Identity identity, ExecutionPlan executionPlan, PlanExecutionAuthorizerInput authorizationInput) throws Exception;

    boolean isMiddleTierPlan(ExecutionPlan executionPlan);
}
