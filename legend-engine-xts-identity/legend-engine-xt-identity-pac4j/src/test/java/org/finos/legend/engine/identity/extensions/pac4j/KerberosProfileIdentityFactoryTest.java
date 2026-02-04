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
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendConstrainedKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KerberosProfileIdentityFactoryTest
{
    private final KerberosProfileIdentityFactory factory = new KerberosProfileIdentityFactory();

    @Test
    void shouldReturnCommonProfilesWhenIdentityHasNoCredentials()
    {
        Identity identity = new Identity("testUser");

        MutableList<CommonProfile> profiles = factory.getProfilesFromIdentity(identity);

        assertInstanceOf(CommonProfile.class, profiles.get(0));
    }

    @Test
    void shouldCreateKerberosProfileFromLegendKerberosCredential()
    {
        Subject subject = new Subject();
        LegendKerberosCredential credential = new LegendKerberosCredential(subject);
        Identity identity = new Identity("testUser", credential);

        MutableList<CommonProfile> profiles = factory.getProfilesFromIdentity(identity);

        assertEquals(1, profiles.size());
        assertInstanceOf(KerberosProfile.class, profiles.get(0));
        KerberosProfile profile = (KerberosProfile) profiles.get(0);
        assertEquals(subject, profile.getSubject());
    }

    @Test
    void shouldCreateKerberosProfileFromLegendConstrainedKerberosCredential()
    {
        Subject subject = new Subject();
        LegendConstrainedKerberosCredential credential = new LegendConstrainedKerberosCredential(subject, subject);
        Identity identity = new Identity("testUser", credential);

        MutableList<CommonProfile> profiles = factory.getProfilesFromIdentity(identity);

        assertEquals(1, profiles.size());
        assertInstanceOf(KerberosProfile.class, profiles.get(0));
        KerberosProfile profile = (KerberosProfile) profiles.get(0);
        assertEquals(subject, profile.getSubject());

    }

    @Test
    void shouldCreateMultipleProfilesFromMultipleCredentials()
    {
        Subject kerberosSub = new Subject();
        Subject constraintKerberosSub = new Subject();
        LegendKerberosCredential kerberosCredential = new LegendKerberosCredential(kerberosSub);
        LegendConstrainedKerberosCredential constrainedCredential = new LegendConstrainedKerberosCredential(constraintKerberosSub, constraintKerberosSub);
        AnonymousCredential anonymousCredential = new AnonymousCredential();

        Identity identity = new Identity("testUser", kerberosCredential, constrainedCredential, anonymousCredential);

        MutableList<CommonProfile> profiles = factory.getProfilesFromIdentity(identity);

        assertEquals(3, profiles.size());
        assertInstanceOf(KerberosProfile.class, profiles.get(0));
        assertInstanceOf(KerberosProfile.class, profiles.get(1));
        assertInstanceOf(CommonProfile.class, profiles.get(2));

    }
}
