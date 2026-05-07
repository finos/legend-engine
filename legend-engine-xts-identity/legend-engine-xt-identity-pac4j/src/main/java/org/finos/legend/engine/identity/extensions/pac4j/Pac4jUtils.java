// Copyright 2024 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class Pac4jUtils
{
    private static final AtomicReference<MutableList<Pac4jIdentityFactory>> CACHED_FACTORIES = new AtomicReference<>();

    private static MutableList<Pac4jIdentityFactory> loadFactories()
    {
        return CACHED_FACTORIES.updateAndGet(existing ->
        {
            if (existing == null)
            {
                return Iterate.addAllTo(ServiceLoader.load(Pac4jIdentityFactory.class), Lists.mutable.empty());
            }
            return existing;
        });
    }

    public static MutableList<CommonProfile> getProfilesFromIdentity(Identity identity)
    {
        MutableList<Pac4jIdentityFactory> factories = loadFactories();
        MutableList<CommonProfile> profiles = Lists.mutable.empty();

        for (Pac4jIdentityFactory identityFactory : factories)
        {
            profiles.addAll(identityFactory.getProfilesFromIdentity(identity));
        }

        return profiles;
    }

    public static boolean checkIfListOfPac4jProfiles(Object authenticationSource)
    {
        if (authenticationSource instanceof List)
        {
            List<?> l = (List<?>) authenticationSource;
            if (l.isEmpty())
            {
                return false;
            }
            else
            {
                if (l.get(0) instanceof CommonProfile)
                {
                    return true;
                }
            }
        }
        return false;
    }
}
