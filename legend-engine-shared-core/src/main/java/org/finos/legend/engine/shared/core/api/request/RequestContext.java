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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RequestContext
{
    private String sessionID;
    private String referral;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Consumer<Boolean> cancellationHandler = null;
    private String requestToken;


    public RequestContext()
    {

    }

    public RequestContext(String sessionID, String referral, String requestToken)
    {
        this.sessionID = sessionID;
        this.referral = referral;
        this.requestToken = requestToken;
    }

    public RequestContext(String sessionID, String referral)
    {
        this.sessionID = sessionID;
        this.referral = referral;
        this.requestToken = sessionID;
    }


    public boolean isCancelled()
    {
        return cancelled.get();
    }

    public void registerCancellationHandler(Consumer<Boolean> cancellationHandler)
    {
        if (this.cancellationHandler != null)
        {
            throw new IllegalStateException("Cancellation handler already registered");
        }
        this.cancellationHandler = cancellationHandler;
        if (cancelled.get())
        {
            cancellationHandler.accept(true);
        }
    }

    public void cancel()
    {
        if (cancelled.compareAndSet(false, true) && cancellationHandler != null)
        {
            cancellationHandler.accept(true);
        }
    }

    /*
     * Safe getters to handle null RequestContext;
     */
    public static String getSessionID(RequestContext requestContext)
    {
        return requestContext != null ? requestContext.sessionID : null;
    }

    public static String getReferral(RequestContext requestContext)
    {
        return requestContext != null ? requestContext.referral : null;
    }

    public static String getRequestToken(RequestContext requestContext)
    {
        return requestContext != null ? requestContext.requestToken : null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RequestContext that = (RequestContext) o;
        return Objects.equals(sessionID, that.sessionID) && Objects.equals(referral, that.referral) && cancelled.equals(that.cancelled) && Objects.equals(cancellationHandler, that.cancellationHandler) && Objects.equals(requestToken, that.requestToken);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sessionID, referral, cancelled, cancellationHandler, requestToken);
    }
}

