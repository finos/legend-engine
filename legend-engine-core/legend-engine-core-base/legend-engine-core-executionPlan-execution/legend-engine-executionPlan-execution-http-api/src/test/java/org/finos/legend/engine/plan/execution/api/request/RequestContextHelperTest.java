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

package org.finos.legend.engine.plan.execution.api.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;


public class RequestContextHelperTest
{
    @Test
    public void testContextHelper()
    {

        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("SESSION_ID");
        when(httpRequest.getHeader(RequestContextHelper.REFERER)).thenReturn("REFERRER");
        when(httpRequest.getHeader(RequestContextHelper.LEGEND_REQUEST_ID)).thenReturn("REQUEST_TOKEN");

        RequestContext requestContext = RequestContextHelper.RequestContext(httpRequest);

        Assert.assertEquals("SESSION_ID", RequestContext.getSessionID(requestContext));
        Assert.assertEquals("REFERRER", RequestContext.getReferral(requestContext));
        Assert.assertEquals("REQUEST_TOKEN", RequestContext.getRequestToken(requestContext));


    }

}

