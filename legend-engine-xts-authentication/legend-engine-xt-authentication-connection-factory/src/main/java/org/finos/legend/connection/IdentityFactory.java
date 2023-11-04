// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;

import java.util.List;

public class IdentityFactory
{
    private final LegendEnvironment environment;

    private IdentityFactory(LegendEnvironment environment)
    {
        this.environment = environment;
    }

    // TODO: @akphi - this clones the logic from IdentityFactoryProvider, we should think about unifying them
    private static final DefaultIdentityFactory DEFAULT = new DefaultIdentityFactory();

    public Identity createIdentity(IdentitySpecification identitySpecification)
    {
        List<Credential> credentials = Lists.mutable.empty();
        credentials.addAll(identitySpecification.getCredentials());
        // TODO: @akphi - should we restrict here that we can only either specify the subject/profiles?
        if (identitySpecification.getSubject() != null)
        {
            credentials.addAll(DEFAULT.makeIdentity(identitySpecification.getSubject()).getCredentials().toList());
        }
        if (!identitySpecification.getProfiles().isEmpty())
        {
            credentials.addAll(DEFAULT.makeIdentity(Lists.mutable.withAll(identitySpecification.getProfiles())).getCredentials().toList());
        }
        if (credentials.isEmpty())
        {
            return identitySpecification.getName() != null ? new Identity(identitySpecification.getName(), new AnonymousCredential()) : DEFAULT.makeUnknownIdentity();
        }
        return new Identity(identitySpecification.getName(), credentials);
    }

    public static class Builder
    {
        private final LegendEnvironment environment;

        public Builder(LegendEnvironment environment)
        {
            this.environment = environment;
        }

        public IdentityFactory build()
        {
            return new IdentityFactory(this.environment);
        }
    }
}
