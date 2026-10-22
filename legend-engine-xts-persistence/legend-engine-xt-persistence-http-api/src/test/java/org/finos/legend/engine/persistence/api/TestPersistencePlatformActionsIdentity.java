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

package org.finos.legend.engine.persistence.api;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;
import org.pac4j.core.profile.AnonymousProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Asserts the ProfileManager accessor rather than the resolved principal: the identity factories
 * registered here select by profile type, so an anonymous profile does not visibly shadow a real
 * one in this repository, but a downstream IdentityFactory that selects by position would see it.
 */
public class TestPersistencePlatformActionsIdentity
{
    @Test
    public void theIdentityComesFromTheFlattenedProfileNotEveryProfile()
    {
        ProfileManager pm = mock(ProfileManager.class);
        CommonProfile real = new CommonProfile();
        real.setId("alice");
        when(pm.getProfile()).thenReturn(Optional.of(real));
        when(pm.getProfiles()).thenReturn(Arrays.<UserProfile>asList(new AnonymousProfile(), real));

        PersistencePlatformActionPayload payload = new PersistencePlatformActionPayload();
        payload.clientVersion = "vX_X_X";
        payload.model = new PureModelContextPointer();
        payload.persistenceContextPath = "test::Context";

        try
        {
            new PersistencePlatformActions(() -> null, new StopsAfterIdentityResolution())
                    .validate(payload, pm);
        }
        catch (IdentityResolved expected)
        {
        }

        verify(pm).getProfile();
        verify(pm, never()).getProfiles();
    }

    @Test
    public void aNullProfileManagerStillResolvesAnIdentity()
    {
        PersistencePlatformActionPayload payload = new PersistencePlatformActionPayload();
        payload.clientVersion = "vX_X_X";
        payload.model = new PureModelContextPointer();
        payload.persistenceContextPath = "test::Context";

        try
        {
            new PersistencePlatformActions(() -> null, new StopsAfterIdentityResolution())
                    .validate(payload, null);
        }
        catch (IdentityResolved expected)
        {
            assertTrue("a null ProfileManager must not blow up before the identity is built",
                    expected.identity != null);
        }
    }

    private static final class IdentityResolved extends RuntimeException
    {
        private final transient Identity identity;

        private IdentityResolved(Identity identity)
        {
            this.identity = identity;
        }
    }

    private static final class StopsAfterIdentityResolution extends ModelManager
    {
        private StopsAfterIdentityResolution()
        {
            super(DeploymentMode.TEST);
        }

        @Override
        public Pair<PureModelContextData, PureModel> loadModelAndData(PureModelContext context, String clientVersion, Identity identity, String packageOffset)
        {
            throw new IdentityResolved(identity);
        }
    }
}
