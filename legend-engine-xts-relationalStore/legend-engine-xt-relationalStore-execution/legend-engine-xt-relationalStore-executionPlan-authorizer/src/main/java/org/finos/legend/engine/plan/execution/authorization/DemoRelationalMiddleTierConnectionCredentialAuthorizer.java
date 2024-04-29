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

public class DemoRelationalMiddleTierConnectionCredentialAuthorizer extends AbstractRelationalMiddleTierConnectionCredentialAuthorizer
{
    private ImmutableList<String> policyData;

    public DemoRelationalMiddleTierConnectionCredentialAuthorizer(ImmutableList<String> policyData)
    {
        this.policyData = policyData;
    }

    @Override
    public CredentialAuthorization evaluateImpl(Identity subject, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
    {
        String policyRule = String.format("%s,%s,%s,%s,%s", policyContext, subject.getName(), credentialVaultReference, usageContext.name(), resourceContext);

        boolean allowed = this.policyData.contains(policyRule);
        CredentialAuthorization.Status status  = allowed ? CredentialAuthorization.Status.ALLOW : CredentialAuthorization.Status.DENY;
        return new CredentialAuthorization(subject.getName(), credentialVaultReference, status, Lists.immutable.empty());
    }
}
