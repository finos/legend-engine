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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.identity.extension.kerberos.KerberosSubjectIdentityFactory;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.Optional;

public class KerberosProfileIdentityFactory implements Pac4jIdentityFactory
{
    @Override
    public Optional<Identity> makeIdentity(Object authenticationSource)
    {
        if (!Pac4jUtils.checkIfListOfPac4jProfiles(authenticationSource))
        {
            return Optional.empty();
        }

        MutableList<CommonProfile> profiles = (MutableList<CommonProfile>) authenticationSource;

        Optional<KerberosProfile> kerberosProfileHolder = Optional.ofNullable(LazyIterate.selectInstancesOf(profiles, KerberosProfile.class).getFirst());
        if (kerberosProfileHolder.isPresent())
        {
            return new KerberosSubjectIdentityFactory().makeIdentity(kerberosProfileHolder.get().getSubject());
        }

        return Optional.empty();
    }

    @Override
    public MutableList<CommonProfile> getProfilesFromIdentity(Identity identity)
    {
        MutableList<CommonProfile> profiles = Lists.mutable.empty();
        ImmutableList<Credential> credentials = identity.getCredentials();
        for (Credential credential : credentials)
        {
            if (credential instanceof LegendKerberosCredential)
            {
                LegendKerberosCredential kerberosCredential = (LegendKerberosCredential) credential;
                profiles.add(new KerberosProfile(kerberosCredential.getSubject(), null));
            }
            else if (credential instanceof AnonymousCredential)
            {
                CommonProfile profile = new CommonProfile();
                profile.setId(identity.getName());
                profiles.add(profile);
            }
        }
        return profiles;
    }
}
