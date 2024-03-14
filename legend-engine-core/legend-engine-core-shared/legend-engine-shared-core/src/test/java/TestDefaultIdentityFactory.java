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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.identity.transformer.KerberosIdentityTransformer;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestDefaultIdentityFactory
{
    private DefaultIdentityFactory defaultIdentityFactory;

    @Before
    public void setup()
    {
        this.defaultIdentityFactory = (DefaultIdentityFactory) IdentityFactoryProvider.getInstance();
        assertTrue(defaultIdentityFactory instanceof DefaultIdentityFactory);
    }

    @Test
    public void testWithEmptyProfile()
    {
        MutableList<CommonProfile> emptyProfile = Lists.mutable.empty();
        Identity identity = defaultIdentityFactory.makeIdentity(emptyProfile);
        String userName = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(emptyProfile));
        assertEquals(userName, identity.getName());

        Subject subjectFromIdentity3 = KerberosIdentityTransformer.getInstance().transform(identity);
        assertEquals(ProfileManagerHelper.extractSubject(emptyProfile), subjectFromIdentity3);
        assertEquals(subjectFromIdentity3, null);
    }

    @Test
    public void testWithSingleKerberosProfile()
    {
        Subject subject = new Subject(true, Sets.mutable.with(new KerberosPrincipal("dummy@example.com")), Sets.mutable.empty(), Sets.mutable.empty());
        KerberosProfile kerberosProfile = new KerberosProfile(subject, null);
        Identity identity = defaultIdentityFactory.makeIdentity(Lists.mutable.with(kerberosProfile));

        assertTrue(identity.getCredential(LegendKerberosCredential.class).isPresent());

        String userName = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(Lists.mutable.with(kerberosProfile)));
        assertEquals(userName, identity.getName() + "@example.com");

        Subject subjectFromIdentity = KerberosIdentityTransformer.getInstance().transform(identity);
        assertEquals(ProfileManagerHelper.extractSubject(Lists.mutable.with(kerberosProfile)), subjectFromIdentity);
        assertEquals(subject, subjectFromIdentity);
    }

    @Test
    public void testWithMultipleKerberosProfile()
    {
        Subject subject1 = new Subject(true, Sets.mutable.with(new KerberosPrincipal("dummy@example.com")), Sets.mutable.empty(), Sets.mutable.empty());
        KerberosProfile kerberosProfile1 = new KerberosProfile(subject1, null);

        Subject subject2 = new Subject();
        KerberosProfile kerberosProfile2 = new KerberosProfile(subject2, null);

        MutableList<CommonProfile> profiles = Lists.mutable.with(kerberosProfile1, kerberosProfile2);
        Identity identity = defaultIdentityFactory.makeIdentity(profiles);

        String userName = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));
        assertEquals(userName, identity.getName() + "@example.com");

        Subject subjectFromIdentity = KerberosIdentityTransformer.getInstance().transform(identity);
        Subject subjectFromProfiles = ProfileManagerHelper.extractSubject(profiles);
        assertEquals(subjectFromProfiles, subjectFromIdentity);
        assertEquals(subject1, subjectFromIdentity);
        assertNotEquals(subject2, subjectFromIdentity);
    }
}
