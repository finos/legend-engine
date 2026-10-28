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

import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.AnonymousProfile;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestProfileManagerHelperPrimaryProfile
{
    @Test
    public void aCommonProfileIsReturned()
    {
        CommonProfile real = new CommonProfile();
        real.setId("alice");

        Assertions.assertEquals(Optional.of(real), ProfileManagerHelper.extractPrimaryProfile(managerWith(real)));
    }

    @Test
    public void anAnonymousProfileIsReturnedAsIs()
    {
        AnonymousProfile anonymous = new AnonymousProfile();

        Assertions.assertEquals(Optional.of(anonymous), ProfileManagerHelper.extractPrimaryProfile(managerWith(anonymous)));
    }

    @Test
    public void aNonCommonProfileIsRejectedAsACredentialsError()
    {
        EngineException thrown = Assertions.assertThrows(EngineException.class, () -> ProfileManagerHelper.extractPrimaryProfile(managerWith(new BasicUserProfile())));

        Assertions.assertEquals(ExceptionCategory.USER_CREDENTIALS_ERROR, thrown.getErrorCategory());
        Assertions.assertTrue(thrown.getMessage().contains(BasicUserProfile.class.getName()), thrown.getMessage());
    }

    @Test
    public void noProfileYieldsEmpty()
    {
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfile()).thenReturn(Optional.empty());

        Assertions.assertEquals(Optional.empty(), ProfileManagerHelper.extractPrimaryProfile(pm));
    }

    @Test
    public void nullProfileManagerYieldsEmpty()
    {
        Assertions.assertEquals(Optional.empty(), ProfileManagerHelper.extractPrimaryProfile(null));
    }

    private static ProfileManager managerWith(UserProfile profile)
    {
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfile()).thenReturn(Optional.of(profile));
        return pm;
    }
}
