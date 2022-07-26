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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;
import static org.junit.Assert.assertEquals;

public class TestDemoRelationalMiddleTierConnectionCredentialAuthorizer
{
    @Test
    public void testUsageContexts() throws Exception
    {
        ImmutableList<String> policyData = Lists.immutable.of(
                 "policy1,alice,reference1,INTERACTIVE_EXECUTION,resource1",
                "policy1,bob,reference1,SERVICE_EXECUTION,resource2",
                "policy2,david,reference2,INTERACTIVE_EXECUTION,resource4"
        );

        DemoRelationalMiddleTierConnectionCredentialAuthorizer authorizer = new DemoRelationalMiddleTierConnectionCredentialAuthorizer(policyData);
        Identity alice = identity("alice");
        Identity bob = identity("bob");
        Identity david = identity("david");

        assertEquals(
                RelationalMiddleTierConnectionCredentialAuthorizer.CredentialAuthorization.Status.ALLOW,
                authorizer.evaluate(alice, "reference1",  INTERACTIVE_EXECUTION, "resource1", "policy1").getStatus());

        assertEquals(
                RelationalMiddleTierConnectionCredentialAuthorizer.CredentialAuthorization.Status.DENY,
                authorizer.evaluate(alice, "reference1",  PlanExecutionAuthorizerInput.ExecutionMode.SERVICE_EXECUTION, "resource1", "policy1").getStatus());

        assertEquals(
                RelationalMiddleTierConnectionCredentialAuthorizer.CredentialAuthorization.Status.ALLOW,
                authorizer.evaluate(bob, "reference1",  PlanExecutionAuthorizerInput.ExecutionMode.SERVICE_EXECUTION, "resource2", "policy1").getStatus());

        assertEquals(
                RelationalMiddleTierConnectionCredentialAuthorizer.CredentialAuthorization.Status.ALLOW,
                authorizer.evaluate(david, "reference2",  INTERACTIVE_EXECUTION, "resource4", "policy2").getStatus());
    }

    private Identity identity(String user)
    {
        return new Identity(user);
    }
}