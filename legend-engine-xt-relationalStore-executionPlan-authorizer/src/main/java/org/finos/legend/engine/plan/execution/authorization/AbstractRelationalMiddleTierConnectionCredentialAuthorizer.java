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

import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRelationalMiddleTierConnectionCredentialAuthorizer implements RelationalMiddleTierConnectionCredentialAuthorizer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRelationalMiddleTierConnectionCredentialAuthorizer.class);

    @Override
    public CredentialAuthorization evaluate(Identity subject, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
    {
        CredentialAuthorization credentialAuthorization = this.evaluateImpl(subject, credentialVaultReference, usageContext, resourceContext, policyContext);
        return credentialAuthorization;
    }

    public abstract CredentialAuthorization evaluateImpl(Identity subject, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception;
}
