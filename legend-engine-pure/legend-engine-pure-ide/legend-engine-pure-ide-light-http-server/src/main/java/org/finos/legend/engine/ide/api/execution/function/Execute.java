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

package org.finos.legend.engine.ide.api.execution.function;

import io.swagger.annotations.Api;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.ide.api.execution.function.manager.ContentType;
import org.finos.legend.engine.ide.api.execution.function.manager.ExecutionManager;
import org.finos.legend.engine.ide.api.execution.function.manager.ExecutionRequest;
import org.finos.legend.engine.ide.api.execution.function.manager.HttpServletResponseWriter;
import org.finos.legend.engine.ide.helpers.JSONResponseTools;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Map;

@Api(tags = "Execute")
@Path("/")
public class Execute
{
    private PureSession pureSession;

    public Execute(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("execute")
    public Response execute(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            try
            {
                FunctionExecution functionExecution = this.pureSession.getFunctionExecution();
                if (null == functionExecution || !functionExecution.isFullyInitializedForExecution())
                {
                    throw new PureExecutionException("System not initialized. Make sure that your pure code has compiled successfully in the IDE.");
                }
                ExecutionManager executionManager = new ExecutionManager(functionExecution);

                StringBuffer urlBuffer = request.getRequestURL();
                String queryString = request.getQueryString();
                if (null != queryString)
                {
                    urlBuffer.append('?');
                    urlBuffer.append(queryString);
                }

                Map<String, String[]> requestParams = request.getParameterMap();
                boolean isJsonInput = JSONResponseTools.JSON_CONTENT_TYPE.equals(request.getContentType()) && (MapIterate.isEmpty(requestParams) || 1 == requestParams.size());

                if (isJsonInput)
                {
                    executionManager.execute(new ExecutionRequest(requestParams), new HttpServletResponseWriter(response), ContentType.json);
                }
                else
                {
                    executionManager.execute(new ExecutionRequest(requestParams), new HttpServletResponseWriter(response), ContentType.text);
                }
            }
            catch (IllegalArgumentException e)
            {
                JSONResponseTools.sendJSONErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e, false);
            }
            catch (RuntimeException | Error e)
            {
                if (e instanceof Error)
                {
                    throw (Error) e;
                }
                JSONResponseTools.sendJSONErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e, false);
            }
        }).build();
    }
}
