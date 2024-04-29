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

package org.finos.legend.engine.plan.execution.api.request;

import org.finos.legend.engine.shared.core.api.request.RequestContext;

import javax.servlet.http.HttpServletRequest;

public class RequestContextHelper
{
    public static String REFERER = "REFERER"; //spelling is correct
    public static String LEGEND_REQUEST_ID = "x-legend-request-id";
    public static String LEGEND_USE_PLAN_CACHE = "x-legend-use-plan-cache";

    public static RequestContext RequestContext(HttpServletRequest httpRequest)
    {
        String clientRef = httpRequest.getHeader(LEGEND_REQUEST_ID);
        String sessionID = httpRequest.getSession().getId();
        return new RequestContext(sessionID, httpRequest.getHeader(REFERER),  clientRef == null ? sessionID : clientRef); //default to the sessionID if no ClientReference was provided
    }

}
