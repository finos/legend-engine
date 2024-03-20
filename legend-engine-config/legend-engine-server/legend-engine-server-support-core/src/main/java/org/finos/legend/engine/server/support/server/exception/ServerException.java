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

package org.finos.legend.engine.server.support.server.exception;

import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class ServerException extends RuntimeException
{
    private static final long serialVersionUID = -427388642530259672L;
    private static final Response.Status DEFAULT_STATUS = Response.Status.INTERNAL_SERVER_ERROR;

    private final Response.Status status;

    public ServerException(String message, Response.Status httpStatus, Throwable cause)
    {
        super(message, cause);
        this.status = (httpStatus == null) ? DEFAULT_STATUS : httpStatus;
    }

    public ServerException(String message, Response.Status httpStatus)
    {
        super(message);
        this.status = (httpStatus == null) ? DEFAULT_STATUS : httpStatus;
    }

    public ServerException(String message, Throwable cause)
    {
        this(message, null, cause);
    }

    public ServerException(String message)
    {
        this(message, (Response.Status) null);
    }

    public Response.Status getStatus()
    {
        return this.status;
    }

    public static <T> T validateNonNull(T arg, String message)
    {
        return validateNonNull(arg, message, null);
    }

    public static <T> T validateNonNull(T arg, String message, Response.Status httpStatus)
    {
        return validate(arg, Objects::nonNull, message, httpStatus);
    }

    public static <T> T validate(T arg, Predicate<? super T> predicate, String message)
    {
        return validate(arg, predicate, message, null);
    }

    public static <T> T validate(T arg, Predicate<? super T> predicate, String message, Response.Status httpStatus)
    {
        if (!predicate.test(arg))
        {
            throw new ServerException(message, (httpStatus == null) ? Response.Status.BAD_REQUEST : httpStatus);
        }
        return arg;
    }

    public static <T> T validate(T arg, Predicate<? super T> predicate, Function<? super T, String> messageFn)
    {
        return validate(arg, predicate, messageFn, null);
    }

    public static <T> T validate(T arg, Predicate<? super T> predicate, Function<? super T, String> messageFn, Response.Status httpStatus)
    {
        if (!predicate.test(arg))
        {
            throw new ServerException((messageFn == null) ? null : messageFn.apply(arg), (httpStatus == null) ? Response.Status.BAD_REQUEST : httpStatus);
        }
        return arg;
    }
}