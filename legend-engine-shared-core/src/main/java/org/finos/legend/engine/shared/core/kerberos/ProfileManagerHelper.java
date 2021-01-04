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
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.security.auth.Subject;

public class ProfileManagerHelper
{
    public static Subject extractSubject(ProfileManager<?> pm)
    {
        if (pm != null)
        {
            CommonProfile profile = pm.get(true).orElse(null);
            if (profile instanceof KerberosProfile)
            {
                return ((KerberosProfile) profile).getSubject();
            }
        }
        return null;
    }

    public static KerberosProfile extractKerberosProfile(MutableList<CommonProfile> profiles)
    {
        if (profiles!=null && profiles.size() > 0)
        {
            profiles.select(p-> p instanceof KerberosProfile).getFirst();

        }
        return null;
    }

    public static KerberosProfile extractKerberosProfile(ProfileManager<?> pm)
    {
        if (pm != null)
        {
            CommonProfile profile = pm.get(true).orElse(null);
            if (profile instanceof KerberosProfile)
            {
                return ((KerberosProfile) profile);
            }
        }
        return null;
    }

    public static Subject extractSubject(MutableList<CommonProfile> profiles)
    {
        if (profiles!=null && (profiles.size() > 0))
        {
           CommonProfile k =  profiles.select(p-> p instanceof KerberosProfile).getFirst();
           if(k != null)
           {
               return ((KerberosProfile)k).getSubject();
           }

        }
        return null;
    }

    public static MutableList<CommonProfile> extractProfile(ProfileManager<?> pm)
    {
        MutableList availableProfiles = Lists.mutable.empty();
        if (pm != null)
        {
            CommonProfile profile = pm.get(true).orElse(null);
            availableProfiles.add(profile);
        }
        return availableProfiles;
    }
}
