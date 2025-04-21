// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.ide.api.debug;

import io.swagger.annotations.Api;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.ide.session.PureSessionManager;
import org.finos.legend.engine.pure.ide.interpreted.debug.DebugState;
import org.finos.legend.engine.pure.ide.interpreted.debug.FunctionExecutionInterpretedWithDebugSupport;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@Api(tags = "Debug")
@Path("/")
public class Debugging
{
    private final PureSessionManager sessionManager;

    public Debugging(PureSessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    @POST
    @Path("debugging")
    public Response debugging(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception
    {
        FunctionExecution functionExecution = sessionManager.getSession().getFunctionExecution();
        if (functionExecution instanceof FunctionExecutionInterpretedWithDebugSupport)
        {
            FunctionExecutionInterpretedWithDebugSupport debugSupport = (FunctionExecutionInterpretedWithDebugSupport) functionExecution;
            DebugState debugState = debugSupport.getDebugState();
            if (debugState == null)
            {
                return Response.ok(getText("Not on debug state!")).build();
            }

            JSONObject mainObject = (JSONObject) new JSONParser().parse(new InputStreamReader(request.getInputStream()));
            JSONObject extraParams = (JSONObject) mainObject.get("extraParams");
            List<String> args = (List<String>) extraParams.get("args");

            if (args.isEmpty())
            {
                args.add("summary");
            }

            switch (args.get(0))
            {
                case "summary":
                    return debugSummary(debugState);
                case "abort":
                    return debugAbort(debugState);
                default: // no command is shortcut for evaluation
                    return debugEvaluate(debugState, String.join(" ", args));
            }
        }
        else
        {
            return Response.status(Response.Status.BAD_REQUEST).entity("Environment does not support debug!").build();
        }
    }

    private Response debugAbort(DebugState debugState)
    {
        debugState.abort();
        return Response.ok(getText("aborting execution...")).build();
    }

    private Response debugSummary(DebugState debugState)
    {
        return Response.ok(getText(debugState.getSummary())).build();
    }

    private Response debugEvaluate(DebugState debugState, String command)
    {
        try
        {
            return Response.ok(getText(debugState.evaluate(command))).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(getText(e.getMessage())).build();
        }
    }

    private static Map<String, String> getText(String value)
    {
        return Maps.fixedSize.of("text", value);
    }
}
