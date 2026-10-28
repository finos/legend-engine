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

import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.ProfileManager;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TestPersistencePlatformActionsProfileType
{
    @Test
    public void aNonCommonProfileIsRejectedAsACredentialsErrorBeforeTheModelLoads()
    {
        ProfileManager pm = mock(ProfileManager.class);
        when(pm.getProfile()).thenReturn(Optional.of(new BasicUserProfile()));
        ModelManager modelManager = mock(ModelManager.class);
        PersistencePlatformActionPayload payload = new PersistencePlatformActionPayload();
        payload.clientVersion = "vX_X_X";
        payload.model = new PureModelContextPointer();
        payload.persistenceContextPath = "test::Context";

        EngineException thrown = Assertions.assertThrows(EngineException.class, () -> new PersistencePlatformActions(() -> null, modelManager).validate(payload, pm));

        Assertions.assertEquals(ExceptionCategory.USER_CREDENTIALS_ERROR, thrown.getErrorCategory());
        verifyNoInteractions(modelManager);
    }
}
