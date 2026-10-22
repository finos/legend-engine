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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.finos.legend.engine.ide.session.PureSession;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.PrintWriter;

@Tag(name = "Activities")
@Path("/")
public class Activities
{
    private PureSession pureSession;

    public Activities(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("conceptsActivity")
    public Response conceptsActivity()
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            outputStream.write(("{\"initializing\":" + !this.pureSession.getPureRuntime().isInitializedNoLock() + ",\"text\":\"" + this.pureSession.message.getMessage() + "\"}").getBytes());
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
            if (pureSession != null)
            {
                isInitializing = pureSession.getPureRuntime().isInitializing();
                isExecuting = pureSession.getCurrentExecutionCount() != 0;
            }
            outputStream.write(("{\"executing\":" + (isExecuting || isInitializing) + ",\"text\":\"" + pureSession.message.getMessage() + "\"}").getBytes());
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
            json.put("text", this.pureSession.message.getMessage());
            json.put("archiveLocked", false);
            try (PrintWriter writer = new PrintWriter(outStream))
            {
                JSONValue.writeJSONString(json, writer);
            }
            outStream.close();
        }).build();
    }
}
