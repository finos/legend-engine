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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.util.GlobalTracer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ExceptionTool
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapper();

    public static Response exceptionManager(Exception exception, LoggingEventType eventType, Iterable<? extends CommonProfile> pm)
    {
        return manage(eventType, pm, new ExceptionError(-1, exception), Response.Status.INTERNAL_SERVER_ERROR);
    }

    public static Response exceptionManager(String message, LoggingEventType eventType, Iterable<? extends CommonProfile> pm)
    {
        return manage(eventType, pm, new ExceptionError(-1, message), Response.Status.INTERNAL_SERVER_ERROR);
    }

    public static Response exceptionManager(Exception exception, LoggingEventType eventType, Response.Status status, Iterable<? extends CommonProfile> pm)
    {
        return manage(eventType, pm, new ExceptionError(-1, exception), status);
    }

    public static Response exceptionManager(String message, LoggingEventType eventType, Response.Status status, Iterable<? extends CommonProfile> pm)
    {
        return manage(eventType, pm, new ExceptionError(-1, message), status);
    }

    private static Response manage(LoggingEventType eventType, Iterable<? extends CommonProfile> pm, ExceptionError error, Response.Status status)
    {
        LOGGER.error(new LogInfo(pm, eventType, error).toString());
        String text;
        try
        {
            text = "{\"status\":\"error\", \"message\":\"" + objectMapper.writeValueAsString(error) + "\"}";
        }
        catch (JsonProcessingException exception)
        {
            throw new RuntimeException(exception);
        }
        if (GlobalTracer.get().activeSpan() != null)
        {
            GlobalTracer.get().activeSpan().setTag("error", text);
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
    }
}
