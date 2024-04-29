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

package org.finos.legend.authentication.credentialprovider;

import java.util.Optional;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;

public class CredentialBuilder
{
    public static Credential makeCredential(CredentialProviderProvider credentialProviderProvider, AuthenticationSpecification authenticationSpecification, Identity identity)
    {
        try
        {
            ImmutableSet<? extends Class<? extends Credential>> inputCredentialTypes = identity.getCredentials().collect(c -> c.getClass()).toSet().toImmutable();
            Optional<CredentialProvider> matchingCredentialProvider = credentialProviderProvider.findMatchingCredentialProvider(authenticationSpecification.getClass(), inputCredentialTypes);

            String message = String.format("Did not find a credential provider for specification type=%s, input credential types=%s.", authenticationSpecification.getClass(), inputCredentialTypes);
            CredentialProvider credentialProvider = matchingCredentialProvider.orElseThrow(() -> new EngineException(message, ExceptionCategory.USER_CREDENTIALS_ERROR));
            return credentialProvider.makeCredential(authenticationSpecification, identity);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new EngineException(e.getMessage(), e, ExceptionCategory.USER_CREDENTIALS_ERROR);
        }
    }
}
