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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class ResultManager
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final JsonStringEncoder jsonStringEncoder = JsonStringEncoder.getInstance();

    public static Response manageResult(MutableList<CommonProfile> pm, Result result, LoggingEventType loggingEventType)
    {
        return manageResult(pm, result, SerializationFormat.defaultFormat, loggingEventType);
    }

    public static Response manageResult(MutableList<CommonProfile> pm, Result result, SerializationFormat format, LoggingEventType loggingEventType)
    {
        return manageResultWithCustomErrorCodeImpl(pm, result, false, format, loggingEventType);
    }

    public static Response manageResultWithCustomErrorCode(MutableList<CommonProfile> pm, Result result, SerializationFormat format, LoggingEventType loggingEventType)
    {
        return manageResultWithCustomErrorCodeImpl(pm, result, true, format, loggingEventType);
    }

    private static Response manageResultWithCustomErrorCodeImpl(MutableList<CommonProfile> pm, Result result, boolean propagateErrorCode, SerializationFormat format, LoggingEventType loggingEventType)
    {
        if (result instanceof ErrorResult)
        {
            ErrorResult errorResult = (ErrorResult) result;
            String message = errorResult.getMessage();
            LOGGER.info(new LogInfo(pm, loggingEventType, message).toString());
            // Not sure about the history behind the 20 error code. We are keep it as is for backwards compatibility
            int errorMessageCode = !propagateErrorCode ? 20 : errorResult.getCode();
            return Response.status(500).type(MediaType.APPLICATION_JSON_TYPE).entity(new ErrorMessage(errorMessageCode, "{\"message\":\"" + new String(jsonStringEncoder.quoteAsString(message)) + "\"}")).build();
        }
        else if (result instanceof StreamingResult)
        {
            return Response.ok(new StreamingResultHandler((StreamingResult) result, format)).build();
        }
        else if (result instanceof ConstantResult)
        {
            return ManageConstantResult.manageResult(pm, ((ConstantResult) result).getValue());
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

    private static class StreamingResultHandler implements StreamingOutput
    {
        private final StreamingResult result;
        private final SerializationFormat format;

        public StreamingResultHandler(StreamingResult result, SerializationFormat format)
        {
            this.result = result;
            this.format = format;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException
        {
            try
            {
                this.result.stream(output, this.format);
            }
            catch (Exception e)
            {
                this.printErrorOnStream(output, e);
                throw e;
            }
        }

        private void printErrorOnStream(OutputStream output, Exception e)
        {
            try
            {
                PrintStream printStream = new PrintStream(output, true);
                printStream.println();
                printStream.println();
                printStream.println("*************************************ERROR*************************************");
                printStream.println('*');
                e.printStackTrace(printStream);
                printStream.println('*');
                printStream.println("*******************************************************************************");
            }
            catch (Exception onPrint)
            {
                e.addSuppressed(onPrint);
            }
        }
    }
}
