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
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import javax.security.auth.Subject;
import java.util.List;
import java.util.Objects;

public class ProfileManagerHelper
{
    public static Subject extractSubject(ProfileManager pm)
    {
        return (pm == null) ? null : extractSubject(pm.getProfiles());
    }

    public static Subject extractSubject(Iterable<? extends UserProfile> profiles)
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

    public static MutableList<CommonProfile> extractProfiles(ProfileManager pm)
    {
        if (pm == null)
        {
            return Lists.fixedSize.empty();
        }
        List<UserProfile> profiles = pm.getProfiles();
        MutableList<CommonProfile> narrowed = Lists.mutable.ofAll(LazyIterate.selectInstancesOf(profiles, CommonProfile.class));
        if (narrowed.size() != profiles.size())
        {
            // Throw rather than drop: dropping can leave an empty list, and Identity.makeIdentity
            // then treats an authenticated request as the unknown user. Class names only, never contents.
            MutableList<String> discarded = LazyIterate.reject(profiles, CommonProfile.class::isInstance)
                    .collect(p -> p.getClass().getName()).toSortedList().distinct();
            throw new EngineException("Authenticated profile(s) of an unsupported type: " + discarded,
                    ExceptionCategory.USER_CREDENTIALS_ERROR);
        }
        return narrowed;
    }
}
