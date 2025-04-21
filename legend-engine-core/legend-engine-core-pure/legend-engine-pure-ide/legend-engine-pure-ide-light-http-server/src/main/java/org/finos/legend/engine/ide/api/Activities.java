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

package org.finos.legend.engine.ide.api;

import io.swagger.annotations.Api;
import org.finos.legend.engine.ide.session.PureSessionManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.PrintWriter;

@Api(tags = "Activities")
@Path("/")
public class Activities
{
    private final PureSessionManager sessionManager;

    public Activities(PureSessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    @GET
    @Path("conceptsActivity")
    public Response conceptsActivity()
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            outputStream.write(("{\"initializing\":" + !this.sessionManager.getSession().getPureRuntime().isInitializedNoLock() + ",\"text\":\"" + this.sessionManager.getSession().message.getMessage() + "\"}").getBytes());
            outputStream.close();
        }).build();
    }

    @GET
    @Path("executionActivity")
    public Response executionActivity(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            boolean isExecuting = false;
            boolean isInitializing = true;
            if (sessionManager.getSession() != null)
            {
                isInitializing = sessionManager.getSession().getPureRuntime().isInitializing();
                isExecuting = sessionManager.getSession().getCurrentExecutionCount() != 0;
            }
            outputStream.write(("{\"executing\":" + (isExecuting || isInitializing) + ",\"text\":\"" + sessionManager.getSession().message.getMessage() + "\"}").getBytes());
            outputStream.close();
        }).build();
    }

    @GET
    @Path("initializationActivity")
    public Response initializationActivity()
    {
        return Response.ok((StreamingOutput) outStream ->
        {
            JSONObject json = new JSONObject();
            json.put("initializing", false);
            json.put("text", this.sessionManager.getSession().message.getMessage());
            json.put("archiveLocked", false);
            try (PrintWriter writer = new PrintWriter(outStream))
            {
                JSONValue.writeJSONString(json, writer);
            }
            outStream.close();
        }).build();
    }
}
