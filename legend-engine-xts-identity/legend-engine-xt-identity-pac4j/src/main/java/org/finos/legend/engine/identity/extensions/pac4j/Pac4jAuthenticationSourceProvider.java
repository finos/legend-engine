// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.identity.extensions.pac4j;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.AuthenticationSourceProvider;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

public class Pac4jAuthenticationSourceProvider implements AuthenticationSourceProvider
{
    @Override
    public Class<?> parameterType()
    {
        return ProfileManager.class;
    }

    @Override
    public Object valueFor(Identity identity)
    {
        return Pac4jUtils.getProfilesFromIdentity(identity);
    }

    @Override
    public String clientName(Identity identity)
    {
        MutableList<CommonProfile> profiles = Pac4jUtils.getProfilesFromIdentity(identity);
        return profiles.isEmpty() ? null : profiles.get(0).getClientName();
    }
}
