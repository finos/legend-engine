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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

public class RequestContextTest
{
    @Test
    public void testRegisterCancellationHandler()
    {
        RequestContext requestContext = new RequestContext();

        AtomicBoolean handlerInvoked = new AtomicBoolean(false);

        CompletableFuture<Void> cancellationFuture = new CompletableFuture<Void>()
        {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning)
            {
                handlerInvoked.set(true);
                return super.cancel(mayInterruptIfRunning);
            }
        };
        requestContext.registerCancellationHandler(result -> cancellationFuture.cancel(true));
        Assert.assertFalse(handlerInvoked.get());
        requestContext.cancel();
        Assert.assertTrue("Cancellation handler was not invoked", handlerInvoked.get());
        Assert.assertTrue("Request is not cancelled", requestContext.isCancelled());
    }

    @Test
    public void testContextGetters()
    {

        Assert.assertNull(RequestContext.getRequestToken(null));
        Assert.assertNull(RequestContext.getReferral(null));
        Assert.assertNull(RequestContext.getSessionID(null));

        RequestContext context = new RequestContext("session", "referral", "clientReference");
        Assert.assertEquals("session", RequestContext.getSessionID(context));
        Assert.assertEquals("referral", RequestContext.getReferral(context));
        Assert.assertEquals("clientReference", RequestContext.getRequestToken(context));


    }

}