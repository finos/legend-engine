// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.finos.legend.engine.postgres.utils.OpenTelemetryUtil;

public class PostgresServerException extends RuntimeException
{
    private static final TextMapSetter<Map<String, String>> TEXT_MAP_SETTER = (map, key, value) -> Objects.requireNonNull(map).put(key, value);

    private final Map<String, String> tracingDetails = new HashMap<>();

    public PostgresServerException(Throwable cause)
    {
        super(cause);
        addTracingDetails();
    }

    public PostgresServerException(String message)
    {
        super(message);
        addTracingDetails();
    }

    public PostgresServerException(String message, Throwable cause)
    {
        super(message, cause);
        addTracingDetails();
    }

    public static PostgresServerException wrapException(Throwable e)
    {
        Throwable toThrow;
        if (e instanceof ExecutionException)
        {
            toThrow = e.getCause();
        }
        else
        {
            toThrow = e;
        }
        if (!(toThrow instanceof PostgresServerException))
        {
            return new PostgresServerException(toThrow);
        }
        return (PostgresServerException) toThrow;

    }

    private void addTracingDetails()
    {
        OpenTelemetryUtil.getPropagators().inject(Context.current(), tracingDetails, TEXT_MAP_SETTER);
    }

    public Map<String, String> getTracingDetails()
    {
        return Collections.unmodifiableMap(tracingDetails);
    }
}
