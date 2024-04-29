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

import org.pac4j.core.profile.CommonProfile;

import java.util.Date;

public class SessionInfo
{
    public String creationTime;
    public String lastAccessTime;
    public String principal;
    public Integer requests;
    public CommonProfile profile;
    public String sessionID;


    public SessionInfo(long creationTime, long lastAccessedTime, String principal, Integer requests, CommonProfile profile, String sessionID)
    {
        this.creationTime = new Date(creationTime).toString();
        this.lastAccessTime = new Date(lastAccessedTime).toString();
        this.principal = principal;
        this.requests = requests;
        this.profile = profile;
        this.sessionID = sessionID;
        if (this.profile != null)
        {
            this.profile.clearSensitiveData();
        }
    }
}
