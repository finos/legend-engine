// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.shared.core.profiles.kerberos;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.profiles.CommonProfileExtension;
import org.pac4j.core.profile.CommonProfile;

public class KerberosProfileExtension implements CommonProfileExtension
{
    @Override
    public boolean matchesCurrentProfileType(MutableList<CommonProfile> profiles)
    {
        return ProfileManagerHelper.extractSubject(profiles) != null;
    }

    @Override
    public String getCurrentUser(MutableList<CommonProfile> profiles)
    {
        return SubjectTools.getKerberos(ProfileManagerHelper.extractSubject(profiles));
    }
}
