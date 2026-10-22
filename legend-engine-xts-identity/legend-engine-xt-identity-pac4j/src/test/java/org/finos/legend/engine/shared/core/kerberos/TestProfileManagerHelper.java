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

package org.finos.legend.engine.shared.core.kerberos;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Test;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestProfileManagerHelper
{
    @Test
    public void aNonCommonProfileIsNotSilentlyDiscarded()
    {
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfiles()).thenReturn(Collections.<UserProfile>singletonList(new BasicUserProfile()));

        EngineException thrown = assertThrows(EngineException.class, () -> ProfileManagerHelper.extractProfiles(pm));
        assertTrue("the message must name the offending type so the fault is diagnosable: " + thrown.getMessage(),
                thrown.getMessage().contains("BasicUserProfile"));
    }

    @Test
    public void aNonCommonProfileAlongsideARealOneStillFails()
    {
        CommonProfile real = new CommonProfile();
        real.setId("alice");
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfiles()).thenReturn(Arrays.<UserProfile>asList(new BasicUserProfile(), real));

        assertThrows(EngineException.class, () -> ProfileManagerHelper.extractProfiles(pm));
    }

    @Test
    public void commonProfilesPassThroughUnchanged()
    {
        CommonProfile first = new CommonProfile();
        first.setId("alice");
        CommonProfile second = new CommonProfile();
        second.setId("bob");
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfiles()).thenReturn(Arrays.<UserProfile>asList(first, second));

        MutableList<CommonProfile> extracted = ProfileManagerHelper.extractProfiles(pm);
        assertEquals(Arrays.asList(first, second), extracted);
    }

    @Test
    public void nullProfileManagerStillYieldsAnEmptyList()
    {
        assertTrue(ProfileManagerHelper.extractProfiles(null).isEmpty());
    }

    @Test
    public void noProfilesYieldsAnEmptyListRatherThanFailing()
    {
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfiles()).thenReturn(Collections.<UserProfile>emptyList());

        assertTrue(ProfileManagerHelper.extractProfiles(pm).isEmpty());
    }
}
