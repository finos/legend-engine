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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.pac4j.core.profile.CommonProfile;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionTracker implements HttpSessionListener
{
    public static final String ATTR_USER_ID = "UserId";
    public static final String ATTR_USER_PROFILE = "UserProfile";
    public static final String ATTR_CALLS = "Calls";
    private MutableSet<HttpSession> sessions = Sets.mutable.empty();

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent)
    {
        sessions.add(httpSessionEvent.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent)
    {
        sessions.removeAll(sessions.select(s -> s.getId().equals(httpSessionEvent.getSession().getId())));
    }

    public SetIterable<SessionInfo> getSessionsInfo(boolean includeProfile, String userID)
    {
        return sessions.collectIf(s -> s.getAttribute(ATTR_USER_ID) != null && userID != null && s.getAttribute(ATTR_USER_ID).equals(userID) || (s.getAttribute(ATTR_USER_ID) != null && userID == null),
                s -> new SessionInfo(s.getCreationTime(), s.getLastAccessedTime(), (String) s.getAttribute(ATTR_USER_ID), (Integer) s.getAttribute(ATTR_CALLS), includeProfile ? (CommonProfile) s.getAttribute(ATTR_USER_PROFILE) : null, s.getId()));
    }

    public String toJSON(boolean includeProfile, String userID)
    {
        try
        {
            return new ObjectMapper().writeValueAsString(getSessionsInfo(includeProfile, userID).toList());
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
