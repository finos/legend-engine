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

package org.finos.legend.engine.plan.execution.api.result;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

public class ResultManager
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final JsonStringEncoder jsonStringEncoder = JsonStringEncoder.getInstance();

    public static Response manageResult(Subject subject, Result result, LoggingEventType loggingEventType)
    {
        return manageResult(subject, result, SerializationFormat.defaultFormat, loggingEventType);
    }

    public static Response manageResult(Subject subject, Result result, SerializationFormat format, LoggingEventType loggingEventType)
    {
        if (result instanceof ErrorResult)
        {
            String message = ((ErrorResult) result).getMessage();
            LOGGER.info(new LogInfo(subject, loggingEventType, message).toString());
            return Response.status(500).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(20, "{\"message\":\"" + new String(jsonStringEncoder.quoteAsString(message)) + "\"}")).build();
        }
        else if (result instanceof StreamingResult)
        {
            return Response.ok((StreamingOutput) outputStream -> ((StreamingResult) result).stream(outputStream, format)).build();
        }
        else if (result instanceof ConstantResult)
        {
            return ManageConstantResult.manageResult(subject, ((ConstantResult) result).getValue());
        }
        else
        {
            throw new RuntimeException("Unknown Error " + result.getClass().getName());
        }
    }

    public static class ErrorMessage
    {
        public int code;
        public String message;

        public ErrorMessage()
        {
        }

        public ErrorMessage(int code, String message)
        {
            this.code = code;
            this.message = message;
        }
    }
}
