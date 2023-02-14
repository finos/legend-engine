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

package org.finos.legend.engine.server.core.session;

import org.finos.legend.engine.protocol.analytics.model.MappedProperty;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public class TestSessionTracker
{
    @Test
    public void testSessionInfoForUser()
    {

        SessionTracker tracker = new SessionTracker();
        HttpSession mockSession1 = Mockito.mock(HttpSession.class);
        Mockito.when(mockSession1.getAttribute(SessionTracker.ATTR_USER_ID)).thenReturn("JohnSmith");
        HttpSession mockSession2 = Mockito.mock(HttpSession.class);
        Mockito.when(mockSession2.getAttribute(SessionTracker.ATTR_USER_ID)).thenReturn("Bob");
        HttpSessionEvent event1 = new HttpSessionEvent(mockSession1);
        HttpSessionEvent event2 = new HttpSessionEvent(mockSession2);
        tracker.sessionCreated(event1);
        tracker.sessionCreated(event2);

        assert (tracker.getSessionsInfo(false, "JohnSmith").size() == 1);
        assert (tracker.getSessionsInfo(false, null).size() == 2);
        assert (tracker.getSessionsInfo(false, "Someone").size() == 0);


    }

}
