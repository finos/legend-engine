// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.shared.core.api.request;

import javax.servlet.http.HttpServletRequest;

public class RequestContext
{
    private String sessionID;
    private String referral;

    public String getSessionID()
    {
        return sessionID;
    }

    public String getReferral()
    {
        return referral;
    }


    public RequestContext(String sessionID, String referral)
    {
        this.sessionID = sessionID;
        this.referral = referral;
    }

    public RequestContext(HttpServletRequest request)

    {
        this.sessionID = request.getSession().getId();
        this.referral = request.getHeader("Referer");
    }


}

