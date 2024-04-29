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
//  limitations under the License.'

package org.finos.legend.engine.postgres.utils;

import java.util.Map;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.config.ServerConfig;

public class ErrorMessageFormatterImpl implements ErrorMessageFormatter
{
    public static final String X_B_3_TRACE_ID = "X-B3-TraceId";
    private final String traceURLEndpoint;

    public ErrorMessageFormatterImpl(ServerConfig serverConfig)
    {
        traceURLEndpoint = serverConfig.getOtelConfig().getTraceURLEndpoint();
    }

    @Override
    public String format(Throwable e)
    {
        if (!(e instanceof PostgresServerException))
        {
            return e.getMessage();
        }
        PostgresServerException postgresServerException = (PostgresServerException) e;
        StringBuilder errorMessage = new StringBuilder(postgresServerException.getMessage());
        Map<String, String> tracingDetails = postgresServerException.getTracingDetails();
        if (tracingDetails != null && tracingDetails.containsKey(X_B_3_TRACE_ID))
        {
            errorMessage.append("\n\t *** Trace : ").append(traceURLEndpoint)
                    .append(tracingDetails.get(X_B_3_TRACE_ID));
        }
        return errorMessage.toString();
    }
}
