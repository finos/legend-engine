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

package org.finos.legend.engine.shared.core.kerberos;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.security.auth.Subject;
import java.util.Objects;
import java.util.Optional;

public class ProfileManagerHelper
{
    public static Subject extractSubject(ProfileManager<?> pm)
    {
        return (pm == null) ? null : extractSubject(pm.getAll(true));
    }

    public static Subject extractSubject(Iterable<? extends CommonProfile> profiles)
    {
        if (profiles == null)
        {
            return null;
        }
        return LazyIterate.selectInstancesOf(profiles, KerberosProfile.class)
                .collect(KerberosProfile::getSubject)
                .select(Objects::nonNull)
                .getFirst();
    }

    public static <T extends CommonProfile> MutableList<T> extractProfiles(ProfileManager<T> pm)
    {
        if (pm != null)
        {
            Optional<T> profile = pm.get(true);
            if (profile.isPresent())
            {
                return Lists.fixedSize.with(profile.get());
            }
        }
        return Lists.fixedSize.empty();
    }
}
